// Command seed imports the DSA question bank from the Excel workbook.
//
// Usage: go run ./cmd/seed [-file docs/DSA Pactice List.xlsx]
package main

import (
	"context"
	"flag"
	"fmt"
	"math"
	"net/url"
	"os"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/xuri/excelize/v2"

	"github.com/rakesh/dsa-tracker/internal/config"
	"github.com/rakesh/dsa-tracker/internal/store"
)

type record struct {
	excelID    string
	title      string
	platform   string
	link       string
	topics     []string
	subtopic   string
	importance *int
	videoURL   string
	videoTitle string
}

func main() {
	file := flag.String("file", "docs/DSA Pactice List.xlsx", "path to the workbook")
	flag.Parse()

	if err := run(*file); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func run(path string) error {
	cfg, err := config.LoadDatabase()
	if err != nil {
		return err
	}

	ctx := context.Background()
	st, err := store.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		return err
	}
	defer st.Close()
	if err := st.Migrate(ctx); err != nil {
		return err
	}

	records, stats, err := parseWorkbook(path)
	if err != nil {
		return err
	}

	if err := upsert(ctx, st, records); err != nil {
		return err
	}

	stale, err := prune(ctx, st, records)
	if err != nil {
		return err
	}

	fmt.Printf("topics:            %d\n", stats.topics)
	fmt.Printf("rows read:         %d\n", stats.read)
	fmt.Printf("empty rows:        %d\n", stats.empty)
	fmt.Printf("questions seeded:  %d\n", len(records))
	fmt.Printf("merged duplicates: %d\n", stats.merged)
	fmt.Printf("missing link:      %d\n", stats.noLink)
	fmt.Printf("stale removed:     %d\n", stale)
	return nil
}

type parseStats struct {
	topics int
	read   int
	empty  int
	merged int
	noLink int
}

func parseWorkbook(path string) ([]record, parseStats, error) {
	var stats parseStats

	f, err := excelize.OpenFile(path)
	if err != nil {
		return nil, stats, fmt.Errorf("open workbook: %w", err)
	}
	defer f.Close()

	order := map[string]int{} // dedupe key -> index
	out := []record{}

	for _, sheet := range f.GetSheetList() {
		rows, err := f.GetRows(sheet)
		if err != nil {
			return nil, stats, fmt.Errorf("read sheet %s: %w", sheet, err)
		}

		topic := topicName(sheet)
		headerSeen := false
		sheetHasData := false

		for i, cols := range rows {
			rowNum := i + 1 // rows in this workbook are contiguous from 1

			if !headerSeen {
				if cellAt(cols, 0) == "Id" {
					headerSeen = true
				}
				continue
			}

			excelID := cellAt(cols, 0)
			rawTitle := cellAt(cols, 1)
			if excelID == "" && rawTitle == "" {
				continue // padding row
			}
			stats.read++
			if excelID == "" || rawTitle == "" {
				stats.empty++
				continue
			}
			sheetHasData = true

			title, titlePlatform := cleanTitle(rawTitle)
			link := hyperLink(f, sheet, 2, rowNum)
			videoURL := hyperLink(f, sheet, 6, rowNum)
			if link == "" {
				stats.noLink++
			}

			rec := record{
				excelID:    excelID,
				title:      title,
				platform:   platform(titlePlatform, link),
				link:       link,
				topics:     []string{topic},
				subtopic:   cellAt(cols, 2),
				importance: parseImportance(cellAt(cols, 3)),
				videoURL:   videoURL,
				videoTitle: cellAt(cols, 5),
			}

			key := dedupeKey(rec)
			if idx, seen := order[key]; seen {
				stats.merged++
				if !contains(out[idx].topics, topic) {
					out[idx].topics = append(out[idx].topics, topic)
				}
				continue
			}
			order[key] = len(out)
			out = append(out, rec)
		}
		if sheetHasData {
			stats.topics++
		}
	}

	for i := range out {
		if out[i].platform == "" {
			out[i].platform = platform("", out[i].link)
		}
	}
	return out, stats, nil
}

func upsert(ctx context.Context, st *store.Store, records []record) error {
	const query = `
		INSERT INTO questions
			(excel_id, title, platform, url, topics, subtopic, importance, video_url, video_title)
		VALUES ($1, $2, $3, NULLIF($4, ''), $5, NULLIF($6, ''), $7, NULLIF($8, ''), NULLIF($9, ''))
		ON CONFLICT (excel_id) DO UPDATE SET
			title       = EXCLUDED.title,
			platform    = EXCLUDED.platform,
			url         = EXCLUDED.url,
			topics      = EXCLUDED.topics,
			subtopic    = EXCLUDED.subtopic,
			importance  = EXCLUDED.importance,
			video_url   = EXCLUDED.video_url,
			video_title = EXCLUDED.video_title`

	ids := map[string]bool{}
	batch := &pgx.Batch{}
	for i, rec := range records {
		id := rec.excelID
		for n := 2; ids[id]; n++ {
			id = fmt.Sprintf("%s#%d", rec.excelID, n)
		}
		ids[id] = true
		records[i].excelID = id
		batch.Queue(query, id, rec.title, rec.platform, rec.link, rec.topics,
			rec.subtopic, rec.importance, rec.videoURL, rec.videoTitle)
	}

	results := st.Pool.SendBatch(ctx, batch)
	defer results.Close()
	for range records {
		if _, err := results.Exec(); err != nil {
			return fmt.Errorf("upsert question: %w", err)
		}
	}
	return nil
}

func prune(ctx context.Context, st *store.Store, records []record) (int, error) {
	keep := make([]string, 0, len(records))
	for _, rec := range records {
		keep = append(keep, rec.excelID)
	}
	tag, err := st.Pool.Exec(ctx,
		`DELETE FROM questions WHERE NOT (excel_id = ANY($1))`, keep)
	if err != nil {
		return 0, err
	}
	return int(tag.RowsAffected()), nil
}

// ---------------------------------------------------------------- helpers

func cellAt(cols []string, idx int) string {
	if idx >= len(cols) {
		return ""
	}
	return strings.TrimSpace(cols[idx])
}

func hyperLink(f *excelize.File, sheet string, col, row int) string {
	ref, err := excelize.CoordinatesToCellName(col, row)
	if err != nil {
		return ""
	}
	ok, link, err := f.GetCellHyperLink(sheet, ref)
	if err != nil || !ok {
		return ""
	}
	return strings.TrimSpace(link)
}

var titleSuffixes = []string{
	" | Practice | GeeksforGeeks",
	" - LeetCode",
	" | LeetCode",
	" - GeeksforGeeks",
	" - Naukri Code 360",
	" | Naukri Code 360",
}

// cleanTitle strips distribution suffixes and returns any platform hint they
// contained.
func cleanTitle(raw string) (string, string) {
	title := strings.TrimSpace(raw)
	hint := ""
	for {
		trimmed := false
		for _, suffix := range titleSuffixes {
			if strings.HasSuffix(title, suffix) {
				if hint == "" {
					hint = suffix
				}
				title = strings.TrimSpace(strings.TrimSuffix(title, suffix))
				trimmed = true
				break
			}
		}
		if !trimmed {
			break
		}
	}
	return title, hint
}

func platform(hint, link string) string {
	lower := strings.ToLower(link)
	switch {
	case strings.Contains(lower, "leetcode.com"):
		return "LeetCode"
	case strings.Contains(lower, "geeksforgeeks.org"):
		return "GFG"
	case strings.Contains(lower, "youtube.com"), strings.Contains(lower, "youtu.be"):
		return "YouTube"
	case strings.Contains(lower, "naukri.com"):
		return "Naukri Code 360"
	}

	h := strings.ToLower(hint)
	switch {
	case strings.Contains(h, "leetcode"):
		return "LeetCode"
	case strings.Contains(h, "geeksforgeeks"):
		return "GFG"
	case strings.Contains(h, "naukri"):
		return "Naukri Code 360"
	}
	return ""
}

func parseImportance(raw string) *int {
	if raw == "" {
		return nil
	}
	var value float64
	if _, err := fmt.Sscanf(raw, "%f", &value); err != nil {
		return nil
	}
	if value < 0 || value > 5 {
		return nil
	}
	n := int(math.Round(value))
	return &n
}

func dedupeKey(rec record) string {
	if rec.link != "" {
		return "url:" + normalizeURL(rec.link)
	}
	return "title:" + strings.ToLower(rec.title) + "|" + rec.subtopic
}

func normalizeURL(raw string) string {
	u, err := url.Parse(strings.TrimSpace(raw))
	if err != nil {
		return strings.ToLower(strings.TrimRight(raw, "/"))
	}
	u.Fragment = ""
	u.RawQuery = ""
	path := strings.TrimRight(u.Path, "/")
	path = strings.TrimSuffix(path, "/description")
	u.Path = path
	return strings.ToLower(u.String())
}

func topicName(sheet string) string {
	if name, ok := topicNames[sheet]; ok {
		return name
	}
	return sheet
}

var topicNames = map[string]string{
	"SlidingWindowTwoPointer": "Sliding Window",
	"PrefixSum":               "Prefix Sum",
	"StackQueue":              "Stack & Queue",
	"BT":                      "Binary Tree",
	"BST":                     "Binary Search Tree",
	"DP":                      "Dynamic Programming",
	"Greedy":                  "Greedy",
	"Graph":                   "Graph",
	"Binary Search":           "Binary Search",
	"LinkedList":              "Linked List",
	"Recursion":               "Recursion",
	"Trie":                    "Trie",
	"Heap":                    "Heap",
	"Arrays":                  "Arrays",
}

func contains(values []string, target string) bool {
	for _, v := range values {
		if v == target {
			return true
		}
	}
	return false
}
