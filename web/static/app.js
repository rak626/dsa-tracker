(() => {
  "use strict";

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  /* ------------------------------------------------------------- theme */

  function syncThemeUI() {
    const mode = window.DSATheme ? window.DSATheme.mode() : "system";
    $$("[data-theme-icon]").forEach((el) => {
      el.classList.toggle("hidden", el.getAttribute("data-theme-icon") !== mode);
    });
    $$("[data-theme-choice]").forEach((el) => {
      el.classList.toggle("seg-on", el.getAttribute("data-theme-choice") === mode);
    });
    const toggle = $("[data-theme-toggle]");
    if (toggle) {
      const label = { system: "system", light: "light", dark: "dark" }[mode];
      toggle.title = "Theme: " + label + " — press t";
      toggle.setAttribute("aria-label", "Colour theme: " + label + ". Press t to cycle.");
    }
  }

  function cycleTheme() {
    if (!window.DSATheme) return;
    window.DSATheme.cycle();
    syncThemeUI();
  }

  const themeToggle = $("[data-theme-toggle]");
  if (themeToggle) themeToggle.addEventListener("click", cycleTheme);

  $$("[data-theme-choice]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (!window.DSATheme) return;
      window.DSATheme.set(btn.getAttribute("data-theme-choice"));
      syncThemeUI();
    });
  });

  syncThemeUI();

  /* ------------------------------------------------------------- toast */

  function showToast(kind, message) {
    const host = $("#toasts");
    if (!host || !message) return;

    const isError = kind === "error";
    const toast = document.createElement("div");
    toast.className = "toast pointer-events-auto " + (isError ? "toast-err" : "toast-ok");
    toast.setAttribute("role", isError ? "alert" : "status");

    const icon = document.createElement("span");
    icon.className = "toast-icon";
    icon.textContent = isError ? "!" : "✓";

    const body = document.createElement("div");
    body.className = "min-w-0 flex-1 pt-0.5 text-sm";
    body.textContent = message;

    const close = document.createElement("button");
    close.className = "btn btn-icon shrink-0";
    close.type = "button";
    close.setAttribute("aria-label", "Dismiss");
    close.innerHTML =
      '<svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>';

    toast.append(icon, body, close);
    host.appendChild(toast);

    let timer = null;
    const dismiss = () => {
      if (timer) clearTimeout(timer);
      toast.classList.add("toast-out");
      toast.addEventListener("animationend", () => toast.remove(), { once: true });
      setTimeout(() => toast.remove(), 500);
    };
    close.addEventListener("click", dismiss);
    timer = setTimeout(dismiss, 4200);
  }

  // Convert server flash banners (?notice= / ?error=) into toasts, then clean
  // the URL so a refresh does not repeat them.
  const flashes = $$("[data-flash]");
  if (flashes.length) {
    flashes.forEach((el) => {
      showToast(el.getAttribute("data-flash"), el.textContent.trim());
      el.remove();
    });
    const url = new URL(window.location.href);
    if (url.searchParams.has("notice") || url.searchParams.has("error")) {
      url.searchParams.delete("notice");
      url.searchParams.delete("error");
      const search = url.searchParams.toString();
      window.history.replaceState(
        {},
        "",
        url.pathname + (search ? "?" + search : "") + url.hash
      );
    }
  }

  /* --------------------------------------------------------- shortcuts */

  const modal = $("#shortcuts");
  const openShortcuts = () => modal && modal.classList.remove("hidden");
  const closeShortcuts = () => modal && modal.classList.add("hidden");

  const openTrigger = $("[data-open-shortcuts]");
  if (openTrigger) openTrigger.addEventListener("click", openShortcuts);
  const closeTrigger = $("[data-close-shortcuts]");
  if (closeTrigger) closeTrigger.addEventListener("click", closeShortcuts);
  if (modal) {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeShortcuts();
    });
  }

  function isTyping(el) {
    if (!el) return false;
    const tag = el.tagName;
    return (
      tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || el.isContentEditable
    );
  }

  function currentActionForm(action) {
    return $$('form[action="/practice"]').find(
      (form) =>
        form.querySelector('input[name="action"]') &&
        form.querySelector('input[name="action"]').value === action
    );
  }

  function focusSearch() {
    const input = $("#q-search");
    if (input) {
      input.focus();
      input.select();
      return;
    }
    if (!location.pathname.startsWith("/questions")) location.href = "/questions";
  }

  function openExternal(selector) {
    const link = $(selector);
    if (link) window.open(link.href, "_blank", "noopener");
  }

  document.addEventListener("keydown", (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;

    if (isTyping(document.activeElement)) {
      if (e.key === "Escape") document.activeElement.blur();
      return;
    }

    if (e.key === "Escape") {
      closeShortcuts();
      return;
    }
    if (e.key === "?" || (e.key === "/" && e.shiftKey)) {
      e.preventDefault();
      if (modal && !modal.classList.contains("hidden")) closeShortcuts();
      else openShortcuts();
      return;
    }
    if (e.key === "/") {
      e.preventDefault();
      focusSearch();
      return;
    }
    if (e.key === "t") {
      cycleTheme();
      return;
    }

    const action = { "1": "solve", "2": "revise", "3": "skip" }[e.key];
    if (action) {
      const form = currentActionForm(action);
      if (form) {
        e.preventDefault();
        const button = form.querySelector("button[type=submit]");
        if (button) button.disabled = true;
        form.submit();
      }
      return;
    }
    if (e.key === "o") openExternal("[data-open-link]");
    if (e.key === "w") openExternal("[data-video-link]");
  });

  /* ------------------------------------------------------------ actions */

  // Copy link buttons on the daily card.
  $$("[data-copy]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const value = btn.getAttribute("data-copy");
      if (!value) return;
      try {
        await navigator.clipboard.writeText(value);
        const original = btn.innerHTML;
        btn.textContent = "Copied";
        setTimeout(() => (btn.innerHTML = original), 1400);
        showToast("notice", "Link copied to clipboard");
      } catch (_) {
        /* clipboard unavailable */
      }
    });
  });

  // Quick size presets on the settings page.
  const sizeInput = $('input[name="daily_size"]');
  const markSizePreset = () => {
    if (!sizeInput) return;
    $$("[data-set-size]").forEach((btn) => {
      btn.classList.toggle("seg-on", btn.getAttribute("data-set-size") === sizeInput.value);
    });
  };
  $$("[data-set-size]").forEach((btn) => {
    btn.addEventListener("click", () => {
      if (!sizeInput) return;
      sizeInput.value = btn.getAttribute("data-set-size");
      markSizePreset();
    });
  });
  if (sizeInput) {
    sizeInput.addEventListener("input", markSizePreset);
    markSizePreset();
  }

  // Show / hide password on the login screen.
  const pwToggle = $("[data-toggle-password]");
  if (pwToggle) {
    pwToggle.addEventListener("click", () => {
      const input = $("#password");
      if (!input) return;
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      pwToggle.setAttribute("aria-label", show ? "Hide password" : "Show password");
      const on = $('[data-eye="on"]', pwToggle);
      const off = $('[data-eye="off"]', pwToggle);
      if (on && off) {
        on.classList.toggle("hidden", show);
        off.classList.toggle("hidden", !show);
      }
      input.focus();
    });
  }

  // Client-side search on the question list.
  const search = $("#q-search");
  if (search) {
    const rows = $$("#q-list > li");
    const empty = $("#q-empty");
    const count = $("#q-count");
    const apply = () => {
      const needle = search.value.trim().toLowerCase();
      let visible = 0;
      rows.forEach((row) => {
        const haystack = row.getAttribute("data-search") || "";
        const show = needle === "" || haystack.includes(needle);
        row.classList.toggle("hidden", !show);
        if (show) visible++;
      });
      if (empty) empty.classList.toggle("hidden", visible > 0);
      if (count) count.textContent = needle === "" ? String(rows.length) : String(visible);
    };
    search.addEventListener("input", apply);
    apply();
  }
})();
