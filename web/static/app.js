(() => {
  "use strict";

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  // Copy link buttons on the daily card.
  $$("[data-copy]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const value = btn.getAttribute("data-copy");
      if (!value) return;
      try {
        await navigator.clipboard.writeText(value);
        const original = btn.textContent;
        btn.textContent = "Copied";
        setTimeout(() => (btn.textContent = original), 1400);
      } catch (_) {
        /* clipboard unavailable */
      }
    });
  });

  // Quick size presets on the settings page.
  $$("[data-set-size]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = $('input[name="daily_size"]');
      if (input) input.value = btn.getAttribute("data-set-size");
    });
  });

  // Client-side search on the question list.
  const search = $("#q-search");
  if (search) {
    const rows = $$("#q-list > li");
    const empty = $("#q-empty");
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
    };
    search.addEventListener("input", apply);
    apply();
  }
})();
