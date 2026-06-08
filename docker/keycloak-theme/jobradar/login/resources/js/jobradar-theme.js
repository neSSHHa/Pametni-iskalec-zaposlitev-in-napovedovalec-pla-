(function () {
  const storageKey = "smartjobs-theme";
  const homeStorageKey = "smartjobs-auth-home";
  const root = document.documentElement;
  const icon = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v2"/><path d="M12 19v2"/><path d="m5.64 5.64 1.42 1.42"/><path d="m16.94 16.94 1.42 1.42"/><path d="M3 12h2"/><path d="M19 12h2"/><path d="m5.64 18.36 1.42-1.42"/><path d="m16.94 7.06 1.42-1.42"/><circle cx="12" cy="12" r="4"/></svg>';

  function readTheme() {
    const queryTheme = new URLSearchParams(window.location.search).get("theme");
    if (queryTheme === "light" || queryTheme === "dark") return queryTheme;
    const stored = localStorage.getItem(storageKey);
    if (stored === "light" || stored === "dark") return stored;
    return window.matchMedia?.("(prefers-color-scheme: light)")?.matches ? "light" : "dark";
  }

  function applyTheme(theme) {
    root.classList.toggle("jr-auth-light", theme === "light");
    root.classList.toggle("jr-auth-dark", theme !== "light");
    root.style.colorScheme = theme;
    localStorage.setItem(storageKey, theme);
  }

  function createToggle() {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "jr-auth-theme-toggle";
    button.setAttribute("aria-label", "Toggle light or dark mode");
    button.innerHTML = icon;

    const updateLabel = () => {
      const light = root.classList.contains("jr-auth-light");
      button.title = light ? "Switch to dark mode" : "Switch to light mode";
      button.setAttribute("aria-label", button.title);
    };

    button.addEventListener("click", () => {
      applyTheme(root.classList.contains("jr-auth-light") ? "dark" : "light");
      updateLabel();
    });

    document.body.appendChild(button);
    updateLabel();
  }

  function getHomeHref() {
    const redirectUri = new URLSearchParams(window.location.search).get("redirect_uri") || readRedirectFromReferrer();
    const storedHome = sessionStorage.getItem(homeStorageKey) || localStorage.getItem(homeStorageKey);

    if (redirectUri) {
      try {
        const homeHref = new URL(redirectUri).origin + "/";
        sessionStorage.setItem(homeStorageKey, homeHref);
        localStorage.setItem(homeStorageKey, homeHref);
        return homeHref;
      } catch (_error) {
        // Keep going to stored/fallback home below.
      }
    }

    return storedHome || "http://localhost:5173/";
  }

  function readRedirectFromReferrer() {
    if (!document.referrer) return null;

    try {
      return new URL(document.referrer).searchParams.get("redirect_uri");
    } catch (_error) {
      return null;
    }
  }

  function createHomeLink() {
    const link = document.createElement("a");
    link.className = "jr-auth-home-link";
    link.href = getHomeHref();
    link.innerHTML = '<span aria-hidden="true">&larr;</span><span>Back to home</span>';
    document.body.appendChild(link);
  }

  applyTheme(readTheme());

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      createHomeLink();
      createToggle();
    });
  } else {
    createHomeLink();
    createToggle();
  }
})();
