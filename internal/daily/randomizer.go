package daily

import (
	"crypto/sha256"
	"encoding/binary"
	"math"
	mrand "math/rand"
	"time"
)

type Item struct {
	ID          int64
	Topics      []string
	Importance  *int
	SolveCount  int
	ReviseCount int
	LastSolved  time.Time // zero when never solved
	LastAttempt time.Time // zero when never practiced
}

type Options struct {
	Size         int
	LookbackDays int
	Day          time.Time // midnight in the application timezone
}

// Day truncates t to midnight in loc.
func Day(t time.Time, loc *time.Location) time.Time {
	y, m, d := t.In(loc).Date()
	return time.Date(y, m, d, 0, 0, 0, 0, loc)
}

// Generate returns an ordered, de-duplicated daily set.
//
// The set is fully deterministic for a given date: it is derived from a
// SHA-256 seeded PRNG, so it survives reloads and redeploys. Questions solved
// within LookbackDays are excluded, weights favour important, stale and
// under-practised problems, and selection spreads across distinct topics.
func Generate(items []Item, opts Options) []int64 {
	if len(items) == 0 {
		return nil
	}
	size := opts.Size
	if size <= 0 {
		size = 5
	}
	lookback := opts.LookbackDays
	if lookback <= 0 {
		lookback = 7
	}

	rng := mrand.New(mrand.NewSource(seedFor(opts.Day)))

	pool := eligible(items, opts.Day, lookback)
	for _, relaxed := range []int{lookback, 1, 0} {
		pool = eligible(items, opts.Day, relaxed)
		if len(pool) >= size {
			break
		}
	}
	if size > len(pool) {
		size = len(pool)
	}

	loads := topicLoads(items)
	weights := make([]float64, len(items))
	for i, it := range items {
		weights[i] = weight(it, opts.Day, loads)
	}

	picked := make([]int64, 0, size)
	used := map[string]bool{}
	remaining := make([]int, len(pool))
	copy(remaining, pool)

	for len(picked) < size && len(remaining) > 0 {
		candidates := make([]int, 0, len(remaining))
		for _, idx := range remaining {
			topic := primaryTopic(items[idx])
			if topic == "" || !used[topic] {
				candidates = append(candidates, idx)
			}
		}
		if len(candidates) == 0 {
			candidates = remaining
		}

		chosen := roulette(rng, candidates, weights)
		item := items[chosen]
		picked = append(picked, item.ID)
		if topic := primaryTopic(item); topic != "" {
			used[topic] = true
		}
		remaining = removeInt(remaining, chosen)
	}

	return picked
}

func seedFor(day time.Time) int64 {
	sum := sha256.Sum256([]byte(day.Format("2006-01-02")))
	return int64(binary.BigEndian.Uint64(sum[:8]))
}

func eligible(items []Item, day time.Time, lookback int) []int {
	out := make([]int, 0, len(items))
	for i, it := range items {
		if it.LastSolved.IsZero() {
			out = append(out, i)
			continue
		}
		days := int(day.Sub(it.LastSolved).Hours() / 24)
		if days >= lookback {
			out = append(out, i)
		}
	}
	return out
}

func weight(it Item, day time.Time, loads map[string]int) float64 {
	w := 1.0
	if it.Importance != nil {
		w *= 1 + float64(*it.Importance)/5
	}

	stale := 60.0
	if !it.LastAttempt.IsZero() {
		days := int(day.Sub(it.LastAttempt).Hours() / 24)
		if days < 0 {
			days = 0
		}
		if days > 60 {
			days = 60
		}
		stale = float64(days)
	}
	w *= 1 + stale/14
	w *= 1 / (1 + float64(it.SolveCount))
	w *= topicBoost(it.Topics, loads)

	if math.IsNaN(w) || math.IsInf(w, 0) || w <= 0 {
		return 1e-9
	}
	return w
}

func topicLoads(items []Item) map[string]int {
	loads := map[string]int{}
	for _, it := range items {
		for _, t := range it.Topics {
			loads[t] += it.SolveCount + it.ReviseCount
		}
	}
	return loads
}

func topicBoost(topics []string, loads map[string]int) float64 {
	if len(topics) == 0 {
		return 1
	}
	max := 0
	for _, load := range loads {
		if load > max {
			max = load
		}
	}
	if max == 0 {
		return 1
	}
	var sum float64
	for _, t := range topics {
		sum += 1 + float64(max-loads[t])/float64(max+1)
	}
	return sum / float64(len(topics))
}

func primaryTopic(it Item) string {
	if len(it.Topics) == 0 {
		return ""
	}
	return it.Topics[0]
}

func roulette(rng *mrand.Rand, candidates []int, weights []float64) int {
	var total float64
	for _, idx := range candidates {
		total += weights[idx]
	}
	if total <= 0 {
		return candidates[rng.Intn(len(candidates))]
	}
	target := rng.Float64() * total
	var cumulative float64
	for _, idx := range candidates {
		cumulative += weights[idx]
		if target <= cumulative {
			return idx
		}
	}
	return candidates[len(candidates)-1]
}

func removeInt(values []int, target int) []int {
	out := values[:0]
	for _, v := range values {
		if v != target {
			out = append(out, v)
		}
	}
	return out
}
