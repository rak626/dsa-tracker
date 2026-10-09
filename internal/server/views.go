package server

import (
	"net/http"
	"strconv"
	"time"

	"github.com/rakesh/dsa-tracker/internal/stats"
	"github.com/rakesh/dsa-tracker/internal/store"
)

// viewData is embedded by every page view.
type viewData struct {
	Title  string
	CSRF   string
	Notice string
	Error  string
	Nav    string
}

type loginView struct {
	viewData
	Next string
}

type todayItem struct {
	Question store.Question
	Action   string
	Position int
	Done     bool
}

type todayView struct {
	viewData
	Day     string
	Size    int
	Total   int
	Done    int
	Current *todayItem
	Items   []todayItem
	Stats   store.Stats
}

type questionRow struct {
	Question    store.Question
	Progress    store.Progress
	TodayAction string
}

type questionsView struct {
	viewData
	Topics      []string
	ActiveTopic string
	Rows        []questionRow
	Stats       store.Stats
	DoneToday   int
}

type settingsView struct {
	viewData
	DailySize     int
	LookbackDays  int
	TimeZone      string
	QuestionCount int
}

type statsView struct {
	viewData
	MonthLabel    string
	PrevMonth     string
	NextMonth     string
	CanGoNext     bool
	CurrentStreak int
	LongestStreak int
	ActiveDays    int
	TotalEntries  int
	MonthActive   int
	MonthEntries  int
	MonthSolves   int
	BestDay       string
	Weeks         [][]stats.Cell
	Weekdays      []string
	Today         time.Time
	HasActivity   bool
}

// base builds the common view data for a request.
func (s *Server) base(r *http.Request, title, nav string) viewData {
	return viewData{
		Title:  title,
		Nav:    nav,
		CSRF:   string(sessionCSRF(r)),
		Notice: r.URL.Query().Get("notice"),
		Error:  r.URL.Query().Get("error"),
	}
}

func (s *Server) render(w http.ResponseWriter, page string, data any) {
	t, ok := s.views[page]
	if !ok {
		http.Error(w, "template not found", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	if err := t.ExecuteTemplate(w, page, data); err != nil {
		s.log.Error("render failed", "page", page, "error", err)
	}
}

func (s *Server) today() time.Time {
	now := time.Now()
	return time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, s.Cfg.Location)
}

func (s *Server) dailySize(r *http.Request) int {
	value, found, err := s.Store.GetSetting(r.Context(), "daily_size")
	if err == nil && found {
		if n, convErr := strconv.Atoi(value); convErr == nil && n > 0 && n <= 50 {
			return n
		}
	}
	return s.Cfg.DailySize
}
