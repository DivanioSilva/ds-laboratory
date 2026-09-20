(() => {
    const storageKey = "person-app-theme";
    const savedTheme = window.localStorage.getItem(storageKey);
    const initialTheme = savedTheme === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = initialTheme;

    const updateButtons = () => {
        const isDark = document.documentElement.dataset.theme === "dark";
        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            button.textContent = isDark ? "☀" : "☾";
            button.setAttribute("aria-pressed", String(isDark));
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        updateButtons();
        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            button.addEventListener("click", () => {
                const nextTheme = document.documentElement.dataset.theme === "dark"
                    ? "light"
                    : "dark";
                document.documentElement.dataset.theme = nextTheme;
                window.localStorage.setItem(storageKey, nextTheme);
                updateButtons();
            });
        });
    });
})();
