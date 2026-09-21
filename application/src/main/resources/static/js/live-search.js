document.addEventListener("DOMContentLoaded", () => {
    let debounceTimer;
    let activeRequest;

    const search = async (form) => {
        const results = document.querySelector("#people-results");
        if (!results) return;
        activeRequest?.abort();
        activeRequest = new AbortController();
        const url = new URL(form.action, window.location.origin);
        new FormData(form).forEach((value, key) => { if (value) url.searchParams.set(key, value); });
        results.setAttribute("aria-busy", "true");
        try {
            const response = await fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" }, signal: activeRequest.signal });
            if (!response.ok) throw new Error("Search failed");
            const html = await response.text();
            const updatedResults = new DOMParser().parseFromString(html, "text/html").querySelector("#people-results");
            if (updatedResults) {
                results.replaceWith(updatedResults);
                window.history.replaceState({}, "", `${url.pathname}${url.search}`);
            }
        } catch (error) {
            if (error.name !== "AbortError") form.submit();
        } finally {
            results.setAttribute("aria-busy", "false");
        }
    };

    document.addEventListener("input", (event) => {
        const input = event.target.closest("[data-live-search] input[name='query']");
        if (!input) return;
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(() => search(input.closest("[data-live-search]")), 300);
    });

    document.addEventListener("change", (event) => {
        if (!event.target.matches("[data-page-size]")) return;
        const url = new URL(window.location.href);
        url.searchParams.set("size", event.target.value);
        url.searchParams.set("page", "1");
        window.location.assign(url);
    });

    document.addEventListener("submit", async (event) => {
        const importForm = event.target.closest("[data-csv-import]");
        if (!importForm) return;
        event.preventDefault();
        const feedback = importForm.querySelector("[data-import-feedback]") || document.querySelector("[data-import-feedback]");
        const submitButton = importForm.querySelector("button[type='submit']");
        submitButton.disabled = true;
        try {
            const response = await fetch("/api/persons/import", { method: "POST", body: new FormData(importForm) });
            if (!response.ok) throw new Error("Import failed");
            const result = await response.json();
            feedback.textContent = `${importForm.dataset.success}: ${result.written} gravadas, ${result.ignored} ignoradas.`;
            window.setTimeout(() => window.location.reload(), 900);
        } catch (error) {
            feedback.textContent = importForm.dataset.error;
        } finally {
            submitButton.disabled = false;
        }
    });
});
