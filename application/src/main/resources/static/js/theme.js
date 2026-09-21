(() => {
    const storageKey = "person-app-theme";
    const savedTheme = window.localStorage.getItem(storageKey);
    const initialTheme = savedTheme === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = initialTheme;

    const updateButtons = () => {
        const isDark = document.documentElement.dataset.theme === "dark";
        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const icon = isDark ? "☀" : "☾";
            const iconElement = button.querySelector("[data-theme-icon]");
            if (iconElement) {
                iconElement.textContent = icon;
            } else {
                button.textContent = icon;
            }
            const labelElement = button.querySelector("[data-theme-label]");
            if (labelElement) {
                labelElement.textContent = isDark ? "Claro" : "Escuro";
            }
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
