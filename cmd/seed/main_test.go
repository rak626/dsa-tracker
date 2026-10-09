package main

import "testing"

func TestCleanTitle(t *testing.T) {
	cases := []struct {
		in     string
		want   string
		platOK bool
	}{
		{"Climbing Stairs - LeetCode", "Climbing Stairs", true},
		{"BFS of graph | Practice | GeeksforGeeks", "BFS of graph", true},
		{"Disjoint Set Data Structure", "Disjoint Set Data Structure", false},
		{"Implement Trie ll - Naukri Code 360", "Implement Trie ll", true},
		{"  Trailing space  ", "Trailing space", false},
	}
	for _, c := range cases {
		got, hint := cleanTitle(c.in)
		if got != c.want {
			t.Errorf("cleanTitle(%q) = %q, want %q", c.in, got, c.want)
		}
		if (hint != "") != c.platOK {
			t.Errorf("cleanTitle(%q) hint = %q, platform hint expected: %v", c.in, hint, c.platOK)
		}
	}
}

func TestPlatform(t *testing.T) {
	if got := platform("", "https://leetcode.com/problems/two-sum/"); got != "LeetCode" {
		t.Errorf("url detection failed: %q", got)
	}
	if got := platform("", "https://www.geeksforgeeks.org/problems/x/y"); got != "GFG" {
		t.Errorf("gfg detection failed: %q", got)
	}
	if got := platform(" - LeetCode", ""); got != "LeetCode" {
		t.Errorf("title hint failed: %q", got)
	}
	if got := platform("", ""); got != "" {
		t.Errorf("expected empty platform, got %q", got)
	}
}

func TestParseImportance(t *testing.T) {
	if v := parseImportance(""); v != nil {
		t.Errorf("empty importance should be nil, got %v", *v)
	}
	if v := parseImportance("garbage"); v != nil {
		t.Errorf("garbage importance should be nil, got %v", *v)
	}
	if v := parseImportance("5.0"); v == nil || *v != 5 {
		t.Errorf("expected 5, got %v", v)
	}
	if v := parseImportance("4.6"); v == nil || *v != 5 {
		t.Errorf("expected rounding to 5, got %v", v)
	}
	if v := parseImportance("9"); v != nil {
		t.Errorf("out of range importance should be nil, got %v", *v)
	}
}

func TestNormalizeURL(t *testing.T) {
	want := "https://leetcode.com/problems/two-sum"
	got := normalizeURL("https://leetcode.com/problems/two-sum/description/?utm=x#frag")
	if got != want {
		t.Errorf("normalizeURL = %q, want %q", got, want)
	}
}

func TestDedupeKeyPrefersURL(t *testing.T) {
	a := record{title: "Same", link: "https://leetcode.com/problems/x/"}
	b := record{title: "Same", link: "https://leetcode.com/problems/x/description"}
	if dedupeKey(a) != dedupeKey(b) {
		t.Errorf("equivalent URLs produced different keys: %q vs %q", dedupeKey(a), dedupeKey(b))
	}
	c := record{title: "Same", subtopic: "A"}
	d := record{title: "Same", subtopic: "B"}
	if dedupeKey(c) == dedupeKey(d) {
		t.Error("different subtopics without URL should not collide")
	}
}
