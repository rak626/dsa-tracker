package server

import (
	"context"
	"encoding/json"
	"fmt"
	"html/template"
	"io/fs"
	"log/slog"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/rakesh/dsa-tracker/internal/auth"
	"github.com/rakesh/dsa-tracker/internal/config"
	"github.com/rakesh/dsa-tracker/internal/store"
	"github.com/rakesh/dsa-tracker/web"
)

const sessionCookie = "sid"

type ctxKey int

const sessionKey ctxKey = iota

type sessionData struct {
	token string
	csrf  []byte
}

type Server struct {
	Cfg     *config.Config
	Store   *store.Store
	Limiter *auth.RateLimiter
	log     *slog.Logger
	views   map[string]*template.Template
	static  http.Handler
}

func New(cfg *config.Config, st *store.Store, logger *slog.Logger) (*Server, error) {
	views, err := parseViews()
	if err != nil {
		return nil, err
	}

	staticFS, err := fs.Sub(web.FS, "static")
	if err != nil {
		return nil, err
	}

	return &Server{
		Cfg:     cfg,
		Store:   st,
		Limiter: auth.NewRateLimiter(5, 15*time.Minute),
		log:     logger,
		views:   views,
		static:  http.StripPrefix("/static/", http.FileServerFS(staticFS)),
	}, nil
}

var viewPages = []string{"login", "today", "questions", "settings"}

func parseViews() (map[string]*template.Template, error) {
	funcs := template.FuncMap{
		"join":  strings.Join,
		"lower": strings.ToLower,
		"add":   func(a, b int) int { return a + b },
		"sub":   func(a, b int) int { return a - b },
		"pct": func(done, total int) int {
			if total == 0 {
				return 0
			}
			return done * 100 / total
		},
		"MakeSeq": func(from, to int) []int {
			out := make([]int, 0, to-from+1)
			for i := from; i <= to; i++ {
				out = append(out, i)
			}
			return out
		},
		// ringOffset returns the SVG stroke offset for a 26px radius circle.
		"ringOffset": func(done, total int) string {
			const circumference = 163.36
			if total <= 0 || done <= 0 {
				return strconv.FormatFloat(circumference, 'f', 2, 64)
			}
			if done > total {
				done = total
			}
			offset := circumference * (1 - float64(done)/float64(total))
			return strconv.FormatFloat(offset, 'f', 2, 64)
		},
	}

	out := make(map[string]*template.Template, len(viewPages))
	for _, page := range viewPages {
		t, err := template.New(page).Funcs(funcs).ParseFS(web.FS,
			"templates/base.html", "templates/"+page+".html")
		if err != nil {
			return nil, fmt.Errorf("parse template %s: %w", page, err)
		}
		out[page] = t
	}
	return out, nil
}

func (s *Server) Handler() http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("GET /healthz", s.healthz)
	mux.Handle("GET /static/", s.staticHandler())
	mux.HandleFunc("GET /login", s.loginPage)
	mux.HandleFunc("POST /login", s.loginSubmit)

	mux.HandleFunc("GET /{$}", s.requireAuth(s.todayPage))
	mux.HandleFunc("GET /questions", s.requireAuth(s.questionsPage))
	mux.HandleFunc("GET /settings", s.requireAuth(s.settingsPage))
	mux.HandleFunc("POST /settings", s.requireAuth(s.requireCSRF(s.settingsSave)))
	mux.HandleFunc("POST /practice", s.requireAuth(s.requireCSRF(s.practiceSubmit)))
	mux.HandleFunc("POST /logout", s.requireAuth(s.requireCSRF(s.logout)))

	mux.HandleFunc("GET /api/today", s.requireAuth(s.apiToday))
	mux.HandleFunc("POST /api/practice", s.requireAuth(s.requireCSRF(s.apiPractice)))

	return s.securityHeaders(mux)
}

func (s *Server) staticHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "public, max-age=3600")
		s.static.ServeHTTP(w, r)
	})
}

// securityHeaders locks down the response for a publicly reachable app.
func (s *Server) securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Content-Security-Policy",
			"default-src 'self'; "+
				"img-src 'self' data:; "+
				"style-src 'self' 'unsafe-inline'; "+
				"script-src 'self'; "+
				"font-src 'self'; "+
				"connect-src 'self'; "+
				"form-action 'self'; "+
				"base-uri 'none'; "+
				"frame-ancestors 'none'; "+
				"object-src 'none'")
		h.Set("X-Frame-Options", "DENY")
		h.Set("X-Content-Type-Options", "nosniff")
		h.Set("Referrer-Policy", "same-origin")
		h.Set("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
		next.ServeHTTP(w, r)
	})
}

func (s *Server) requireAuth(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		cookie, err := r.Cookie(sessionCookie)
		if err != nil || cookie.Value == "" {
			s.denyUnauthenticated(w, r)
			return
		}

		sess, err := s.Store.GetSession(r.Context(), auth.HashToken(cookie.Value))
		if err != nil {
			s.log.Error("session lookup failed", "error", err)
			http.Error(w, "internal error", http.StatusInternalServerError)
			return
		}
		if sess == nil {
			clearSessionCookie(w, r)
			s.denyUnauthenticated(w, r)
			return
		}

		ctx := context.WithValue(r.Context(), sessionKey, sessionData{
			token: cookie.Value,
			csrf:  sess.CSRF,
		})
		next(w, r.WithContext(ctx))
	}
}

func (s *Server) requireCSRF(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		sess, ok := sessionFrom(r.Context())
		if !ok {
			s.denyUnauthenticated(w, r)
			return
		}
		provided := r.Header.Get("X-CSRF-Token")
		if provided == "" {
			provided = r.FormValue("_csrf")
		}
		if !auth.EqualSecret([]byte(provided), sess.csrf) {
			if strings.HasPrefix(r.URL.Path, "/api") {
				writeJSON(w, http.StatusForbidden, map[string]string{"error": "invalid csrf token"})
				return
			}
			http.Error(w, "invalid form token, please retry", http.StatusForbidden)
			return
		}
		next(w, r)
	}
}

func (s *Server) denyUnauthenticated(w http.ResponseWriter, r *http.Request) {
	if strings.HasPrefix(r.URL.Path, "/api") {
		writeJSON(w, http.StatusUnauthorized, map[string]string{"error": "authentication required"})
		return
	}
	target := "/login"
	if next := r.URL.RequestURI(); next != "/" && isSafeNext(next) {
		target += "?next=" + next
	}
	http.Redirect(w, r, target, http.StatusSeeOther)
}

func sessionFrom(ctx context.Context) (sessionData, bool) {
	sess, ok := ctx.Value(sessionKey).(sessionData)
	return sess, ok
}

func isSafeNext(next string) bool {
	return strings.HasPrefix(next, "/") && !strings.HasPrefix(next, "//") &&
		!strings.Contains(next, "\n") && !strings.Contains(next, "\r")
}

func writeJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(payload)
}

func clientIP(r *http.Request) string {
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		if i := strings.IndexByte(xff, ','); i > 0 {
			return strings.TrimSpace(xff[:i])
		}
		return strings.TrimSpace(xff)
	}
	if host, _, err := net.SplitHostPort(r.RemoteAddr); err == nil {
		return host
	}
	return r.RemoteAddr
}

func isSecure(r *http.Request) bool {
	return r.TLS != nil || strings.EqualFold(r.Header.Get("X-Forwarded-Proto"), "https")
}

func setSessionCookie(w http.ResponseWriter, r *http.Request, token string, ttl time.Duration) {
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookie,
		Value:    token,
		Path:     "/",
		MaxAge:   int(ttl.Seconds()),
		HttpOnly: true,
		Secure:   isSecure(r),
		SameSite: http.SameSiteStrictMode,
	})
}

func clearSessionCookie(w http.ResponseWriter, r *http.Request) {
	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookie,
		Value:    "",
		Path:     "/",
		MaxAge:   -1,
		HttpOnly: true,
		Secure:   isSecure(r),
		SameSite: http.SameSiteStrictMode,
	})
}

func atoiOr(s string, fallback int) int {
	if n, err := strconv.Atoi(s); err == nil {
		return n
	}
	return fallback
}
