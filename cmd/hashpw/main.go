package main

import (
	"fmt"
	"os"
	"strings"
	"syscall"

	"golang.org/x/term"

	"github.com/rakesh/dsa-tracker/internal/auth"
)

// hashpw prints an Argon2id hash for use as AUTH_PASSWORD_HASH.
//
// Usage: go run ./cmd/hashpw [password]   (use "-" to read it hidden from stdin)
func main() {
	password := ""
	if len(os.Args) > 1 {
		password = os.Args[1]
	}
	if password == "-" || password == "" {
		fmt.Fprint(os.Stderr, "Password: ")
		raw, err := term.ReadPassword(int(syscall.Stdin))
		if err != nil {
			fmt.Fprintln(os.Stderr, "error:", err)
			os.Exit(1)
		}
		fmt.Fprintln(os.Stderr)
		password = string(raw)
	}

	if err := auth.ValidatePassword(password); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}

	hash, err := auth.HashPassword(password)
	if err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
	fmt.Println(strings.TrimSpace(hash))
}
