// Pre-paint theme resolution. Loaded non-deferred from <head> so the correct
// palette is applied before first paint, without an inline script (CSP).
(function () {
  var KEY = "dsa-theme";
  var MODES = ["system", "light", "dark"];

  function readMode() {
    var value = null;
    try {
      value = window.localStorage.getItem(KEY);
    } catch (e) {
      /* storage unavailable */
    }
    return MODES.indexOf(value) >= 0 ? value : "system";
  }

  function resolve(mode) {
    if (mode !== "system") return mode;
    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function apply(mode) {
    var root = document.documentElement;
    root.setAttribute("data-theme-mode", mode);
    root.setAttribute("data-theme", resolve(mode));
  }

  apply(readMode());

  var mq = window.matchMedia("(prefers-color-scheme: dark)");
  var onChange = function () {
    if (readMode() === "system") apply("system");
  };
  if (mq.addEventListener) mq.addEventListener("change", onChange);
  else if (mq.addListener) mq.addListener(onChange);

  window.DSATheme = {
    mode: readMode,
    resolved: function () {
      return resolve(readMode());
    },
    set: function (mode) {
      if (MODES.indexOf(mode) < 0) mode = "system";
      try {
        window.localStorage.setItem(KEY, mode);
      } catch (e) {
        /* storage unavailable */
      }
      apply(mode);
    },
    cycle: function () {
      var next = MODES[(MODES.indexOf(readMode()) + 1) % MODES.length];
      window.DSATheme.set(next);
      return next;
    },
  };
})();
