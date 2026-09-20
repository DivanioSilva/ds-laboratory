document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-live-search]");
    const input = form?.querySelector("input[name='firstName']");
    const results = document.querySelector("#people-results");

    if (!form || !input || !results) {
        return;
    }

    let debounceTimer;
    let activeRequest;

    const search = async () => {
        activeRequest?.abort();
        activeRequest = new AbortController();

        const term = input.value.trim();
        const url = new URL(form.action, window.location.origin);
        if (term) {
            url.searchParams.set("firstName", term);
        }

        results.setAttribute("aria-busy", "true");
        form.classList.add("is-searching");

        try {
            const response = await fetch(url, {
                headers: { "X-Requested-With": "XMLHttpRequest" },
                signal: activeRequest.signal
            });

            if (!response.ok) {
                throw new Error(`Search failed with status ${response.status}`);
            }

            const html = await response.text();
            const documentResult = new DOMParser().parseFromString(html, "text/html");
            const updatedResults = documentResult.querySelector("#people-results");

            if (updatedResults) {
                results.innerHTML = updatedResults.innerHTML;
                window.history.replaceState({}, "", `${url.pathname}${url.search}`);
            }
        } catch (error) {
            if (error.name !== "AbortError") {
                form.submit();
            }
        } finally {
            results.setAttribute("aria-busy", "false");
            form.classList.remove("is-searching");
        }
    };

    input.addEventListener("input", () => {
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(search, 300);
    });
});
