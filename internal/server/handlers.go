package server

import (
	"encoding/json"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/rakesh/dsa-tracker/internal/auth"
	"github.com/rakesh/dsa-tracker/internal/daily"
	"github.com/rakesh/dsa-tracker/internal/store"
)

func (s *Server) healthz(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	_, _ = w.Write([]byte("ok"))
}

// ---------------------------------------------------------------- login

func (s *Server) loginPage(w http.ResponseWriter, r *http.Request) {
	if cookie, err := r.Cookie(sessionCookie); err == nil && cookie.Value != "" {
		if sess, err := s.Store.GetSession(r.Context(), auth.HashToken(cookie.Value)); err == nil && sess != nil {
			http.Redirect(w, r, "/", http.StatusSeeOther)
			return
		}
	}
	s.render(w, "login", loginView{viewData: s.base(r, "Sign in", "login"),
		Next: safeNext(r.URL.Query().Get("next"))})
}

func (s *Server) loginSubmit(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		http.Error(w, "bad request", http.StatusBadRequest)
		return
	}

	ip := clientIP(r)
	if !s.Limiter.Allow(ip) {
		http.Error(w, "too many attempts, try again later", http.StatusTooManyRequests)
		return
	}

	next := safeNext(r.FormValue("next"))
	if !auth.VerifyPassword(r.FormValue("password"), s.Cfg.PasswordHash) {
		s.log.Warn("failed login attempt", "ip", ip)
		view := loginView{viewData: s.base(r, "Sign in", "login"), Next: next}
		view.Error = "Incorrect password."
		w.WriteHeader(http.StatusUnauthorized)
		s.render(w, "login", view)
		return
	}

	s.Limiter.Reset(ip)

	token := auth.NewToken(32)
	csrf := []byte(auth.NewToken(24))
	if err := s.Store.CreateSession(r.Context(), auth.HashToken(token), csrf,
		time.Now().Add(s.Cfg.SessionTTL)); err != nil {
		s.log.Error("create session", "error", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	_ = s.Store.PurgeExpiredSessions(r.Context())

	setSessionCookie(w, r, token, s.Cfg.SessionTTL)
	http.Redirect(w, r, next, http.StatusSeeOther)
}

func (s *Server) logout(w http.ResponseWriter, r *http.Request) {
	if sess, ok := sessionFrom(r.Context()); ok {
		_ = s.Store.DeleteSession(r.Context(), auth.HashToken(sess.token))
	}
	clearSessionCookie(w, r)
	http.Redirect(w, r, "/login", http.StatusSeeOther)
}

// ---------------------------------------------------------------- today

// todaySet loads today's deterministic set, creating it when missing.
func (s *Server) todaySet(r *http.Request) (*store.DailySet, []questionRow, error) {
	ctx := r.Context()
	day := s.today()
	size := s.dailySize(r)

	questions, err := s.Store.AllQuestions(ctx)
	if err != nil {
		return nil, nil, err
	}
	byID := make(map[int64]store.Question, len(questions))
	for _, q := range questions {
		byID[q.ID] = q
	}

	progress, err := s.Store.ProgressMap(ctx)
	if err != nil {
		return nil, nil, err
	}

	set, err := s.Store.GetDailySet(ctx, day)
	if err != nil {
		return nil, nil, err
	}

	existing := set != nil
	if set != nil && set.Size != size {
		done, err := s.Store.PracticedOn(ctx, day, set.QuestionIDs)
		if err != nil {
			return nil, nil, err
		}
		// Only re-roll an untouched set when the size changes; a set that
		// already has progress stays fixed until tomorrow.
		if len(done) == 0 {
			set = nil
		}
	}

	if set == nil {
		items := buildItems(questions, progress)
		ids := daily.Generate(items, daily.Options{
			Size:         size,
			LookbackDays: s.Cfg.LookbackDays,
			Day:          day,
		})
		if existing {
			err = s.Store.ReplaceDailySet(ctx, day, size, ids)
		} else {
			err = s.Store.SaveDailySet(ctx, day, size, ids)
		}
		if err != nil {
			return nil, nil, err
		}
		set = &store.DailySet{Day: day, Size: size, QuestionIDs: ids}
	}

	practiced, err := s.Store.PracticedOn(ctx, day, set.QuestionIDs)
	if err != nil {
		return nil, nil, err
	}

	rows := make([]questionRow, 0, len(set.QuestionIDs))
	for _, id := range set.QuestionIDs {
		q, ok := byID[id]
		if !ok {
			continue
		}
		rows = append(rows, questionRow{
			Question:    q,
			Progress:    progress[id],
			TodayAction: practiced[id],
		})
	}
	return set, rows, nil
}

func buildItems(questions []store.Question, progress map[int64]store.Progress) []daily.Item {
	items := make([]daily.Item, 0, len(questions))
	for _, q := range questions {
		p := progress[q.ID]
		items = append(items, daily.Item{
			ID:          q.ID,
			Topics:      q.Topics,
			Importance:  q.Importance,
			SolveCount:  p.SolveCount,
			ReviseCount: p.ReviseCount,
			LastSolved:  p.LastSolvedOn,
			LastAttempt: p.LastPracticedOn,
		})
	}
	return items
}

func (s *Server) todayPage(w http.ResponseWriter, r *http.Request) {
	set, rows, err := s.todaySet(r)
	if err != nil {
		s.log.Error("load today", "error", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	view := todayView{
		viewData: s.base(r, "Today", "today"),
		Day:      set.Day.In(s.Cfg.Location).Format("Monday, 02 January 2006"),
		Total:    len(rows),
	}

	for i, row := range rows {
		item := todayItem{Question: row.Question, Action: row.TodayAction, Position: i + 1}
		item.Done = item.Action != ""
		if item.Done {
			view.Done++
		} else if view.Current == nil {
			view.Current = &item
		}
		view.Items = append(view.Items, item)
	}
	view.Size = view.Total

	if view.Stats, err = s.Store.Stats(r.Context()); err != nil {
		s.log.Error("load stats", "error", err)
	}

	s.render(w, "today", view)
}

// ---------------------------------------------------------------- questions

func (s *Server) questionsPage(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	questions, err := s.Store.AllQuestions(ctx)
	if err != nil {
		s.log.Error("load questions", "error", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}
	progress, err := s.Store.ProgressMap(ctx)
	if err != nil {
		s.log.Error("load progress", "error", err)
		http.Error(w, "internal error", http.StatusInternalServerError)
		return
	}

	topic := strings.TrimSpace(r.URL.Query().Get("topic"))
	seen := map[string]bool{}
	rows := make([]questionRow, 0, len(questions))
	for _, q := range questions {
		for _, t := range q.Topics {
			seen[t] = true
		}
		if topic != "" && !contains(q.Topics, topic) {
			continue
		}
		rows = append(rows, questionRow{Question: q, Progress: progress[q.ID]})
	}

	todayIDs := map[int64]string{}
	if set, err := s.Store.GetDailySet(ctx, s.today()); err == nil && set != nil {
		if practiced, err := s.Store.PracticedOn(ctx, s.today(), set.QuestionIDs); err == nil {
			todayIDs = practiced
		}
	}
	for i := range rows {
		rows[i].TodayAction = todayIDs[rows[i].Question.ID]
	}

	view := questionsView{
		viewData:    s.base(r, "Questions", "questions"),
		Topics:      sortedKeys(seen),
		ActiveTopic: topic,
		Rows:        rows,
	}
	for i := range rows {
		if rows[i].TodayAction != "" {
			view.DoneToday++
		}
	}
	if view.Stats, err = s.Store.Stats(ctx); err != nil {
		s.log.Error("load stats", "error", err)
	}

	s.render(w, "questions", view)
}

// ---------------------------------------------------------------- settings

func (s *Server) settingsPage(w http.ResponseWriter, r *http.Request) {
	view := settingsView{
		viewData:     s.base(r, "Settings", "settings"),
		DailySize:    s.dailySize(r),
		LookbackDays: s.Cfg.LookbackDays,
		TimeZone:     s.Cfg.Location.String(),
	}
	if n, err := s.Store.Stats(r.Context()); err == nil {
		view.QuestionCount = n.QuestionCount
	}
	s.render(w, "settings", view)
}

func (s *Server) settingsSave(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		redirectTo(w, r, "/settings", "error", "Invalid form")
		return
	}
	size := atoiOr(r.FormValue("daily_size"), 0)
	if size < 1 || size > 50 {
		redirectTo(w, r, "/settings", "error", "Daily set size must be between 1 and 50")
		return
	}
	if err := s.Store.SetSetting(r.Context(), "daily_size", strconv.Itoa(size)); err != nil {
		s.log.Error("save settings", "error", err)
		redirectTo(w, r, "/settings", "error", "Could not save settings")
		return
	}
	redirectTo(w, r, "/settings", "notice", "Saved")
}

// ---------------------------------------------------------------- practice

var allowedActions = map[string]bool{"solve": true, "revise": true, "skip": true}

func (s *Server) practiceSubmit(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		redirectTo(w, r, "/", "error", "Invalid form")
		return
	}

	next := safeNext(r.FormValue("next"))
	id, _ := strconv.ParseInt(r.FormValue("question_id"), 10, 64)
	action := r.FormValue("action")

	if id <= 0 || !allowedActions[action] {
		redirectTo(w, r, next, "error", "Could not record that")
		return
	}
	if _, err := s.Store.QuestionByID(r.Context(), id); err != nil {
		redirectTo(w, r, next, "error", "Unknown question")
		return
	}
	if err := s.Store.Record(r.Context(), id, action, s.today()); err != nil {
		s.log.Error("record practice", "error", err)
		redirectTo(w, r, next, "error", "Could not record that")
		return
	}

	label := map[string]string{
		"solve":  "Marked as solved",
		"revise": "Marked for revision",
		"skip":   "Skipped for now",
	}[action]
	redirectTo(w, r, next, "notice", label)
}

// ---------------------------------------------------------------- json api

type apiQuestion struct {
	ID          int64    `json:"id"`
	Title       string   `json:"title"`
	Platform    string   `json:"platform,omitempty"`
	URL         string   `json:"url,omitempty"`
	Topics      []string `json:"topics,omitempty"`
	Subtopic    string   `json:"subtopic,omitempty"`
	Importance  *int     `json:"importance,omitempty"`
	VideoURL    string   `json:"video_url,omitempty"`
	VideoTitle  string   `json:"video_title,omitempty"`
	Action      string   `json:"action,omitempty"`
	SolveCount  int      `json:"solve_count"`
	ReviseCount int      `json:"revise_count"`
}

func (s *Server) apiToday(w http.ResponseWriter, r *http.Request) {
	set, rows, err := s.todaySet(r)
	if err != nil {
		s.log.Error("api today", "error", err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "internal error"})
		return
	}

	out := struct {
		Date      string        `json:"date"`
		Size      int           `json:"size"`
		Done      int           `json:"done"`
		CSRF      string        `json:"csrf_token"`
		Questions []apiQuestion `json:"questions"`
	}{
		Date: set.Day.In(s.Cfg.Location).Format("2006-01-02"),
		Size: len(rows),
		CSRF: string(sessionCSRF(r)),
	}

	for _, row := range rows {
		if row.TodayAction != "" {
			out.Done++
		}
		out.Questions = append(out.Questions, apiQuestion{
			ID: row.Question.ID, Title: row.Question.Title, Platform: row.Question.Platform,
			URL: row.Question.URL, Topics: row.Question.Topics, Subtopic: row.Question.Subtopic,
			Importance: row.Question.Importance, VideoURL: row.Question.VideoURL,
			VideoTitle: row.Question.VideoTitle, Action: row.TodayAction,
			SolveCount: row.Progress.SolveCount, ReviseCount: row.Progress.ReviseCount,
		})
	}
	writeJSON(w, http.StatusOK, out)
}

func (s *Server) apiPractice(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(io.LimitReader(r.Body, 1<<20))
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "bad request"})
		return
	}
	var payload struct {
		QuestionID int64  `json:"question_id"`
		Action     string `json:"action"`
	}
	if err := json.Unmarshal(body, &payload); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid json"})
		return
	}
	if payload.QuestionID <= 0 || !allowedActions[payload.Action] {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid payload"})
		return
	}
	if _, err := s.Store.QuestionByID(r.Context(), payload.QuestionID); err != nil {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "question not found"})
		return
	}
	if err := s.Store.Record(r.Context(), payload.QuestionID, payload.Action, s.today()); err != nil {
		s.log.Error("api record", "error", err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "could not record"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"ok": true, "action": payload.Action})
}

// ---------------------------------------------------------------- helpers

func sessionCSRF(r *http.Request) []byte {
	if sess, ok := sessionFrom(r.Context()); ok {
		return sess.csrf
	}
	return nil
}

func safeNext(next string) string {
	if isSafeNext(next) {
		return next
	}
	return "/"
}

// redirectTo appends a flash message as a query parameter.
func redirectTo(w http.ResponseWriter, r *http.Request, target, key, message string) {
	sep := "?"
	if strings.Contains(target, "?") {
		sep = "&"
	}
	http.Redirect(w, r, target+sep+key+"="+url.QueryEscape(message), http.StatusSeeOther)
}

func contains(values []string, target string) bool {
	for _, v := range values {
		if v == target {
			return true
		}
	}
	return false
}

func sortedKeys(m map[string]bool) []string {
	out := make([]string, 0, len(m))
	for k := range m {
		out = append(out, k)
	}
	for i := 1; i < len(out); i++ {
		for j := i; j > 0 && out[j] < out[j-1]; j-- {
			out[j], out[j-1] = out[j-1], out[j]
		}
	}
	return out
}
