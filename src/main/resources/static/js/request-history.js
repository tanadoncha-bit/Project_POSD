document.addEventListener("DOMContentLoaded", () => {
    const main = document.querySelector("main[data-server-paged=true]");
    if (main) {
        const cache = new Map();
        const ttl = 30000;
        let pending;
        let version = 0;
        const key = url => {
            const normalized = new URL(url);
            if (normalized.searchParams.get("status") === "ALL") normalized.searchParams.delete("status");
            if (normalized.searchParams.get("page") === "0") normalized.searchParams.delete("page");
            normalized.searchParams.sort();
            return normalized.pathname + normalized.search;
        };
        const remember = (url, html) => {
            cache.delete(key(url));
            cache.set(key(url), {html, expires: Date.now() + ttl});
            if (cache.size > 10) cache.delete(cache.keys().next().value);
        };
        remember(new URL(location.href), main.innerHTML);
        async function navigate(url, push = true) {
            const id = ++version;
            pending?.abort();
            pending = new AbortController();
            main.setAttribute("aria-busy", "true");
            try {
                let entry = cache.get(key(url));
                if (!entry || entry.expires <= Date.now()) {
                    const response = await fetch(url, {signal: pending.signal, cache: "no-store"});
                    if (id !== version) return;
                    if (response.redirected) { location.assign(response.url); return; }
                    if (!response.ok) throw new Error("Unable to load requests. Please try again.");
                    const doc = new DOMParser().parseFromString(await response.text(), "text/html");
                    const content = doc.querySelector("main[data-server-paged=true]");
                    if (!content || doc.body.dataset.authenticated !== "true") {
                        cache.clear(); location.assign(url); return;
                    }
                    if (id !== version) return;
                    remember(url, content.innerHTML);
                    entry = cache.get(key(url));
                }
                if (id !== version) return;
                main.innerHTML = entry.html;
                document.dispatchEvent(new Event("requests:updated"));
                if (push) history.pushState(null, "", url);
                const selected = main.querySelector('[data-request-filter].active');
                selected?.focus({preventScroll: true});
            } catch (error) {
                if (error.name !== "AbortError" && id === version) {
                    if (!push) { location.reload(); return; }
                    window.toast?.error(error.message);
                }
            } finally {
                if (id === version) main.removeAttribute("aria-busy");
            }
        }
        main.addEventListener("click", event => {
            const filter = event.target.closest("[data-request-filter]");
            const link = event.target.closest(".management-pagination a");
            if (!filter && !link) return;
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
            event.preventDefault();
            const url = new URL(link ? link.href : location.href);
            if (filter) { url.searchParams.set("status", filter.dataset.requestFilter); url.searchParams.delete("page"); }
            navigate(url);
        });
        window.addEventListener("popstate", () => navigate(new URL(location.href), false));
        window.addEventListener("focus", () => cache.clear());
        window.addEventListener("pageshow", event => { if (event.persisted) { cache.clear(); navigate(new URL(location.href), false); } });
        document.addEventListener("click", event => { if (event.target.closest("[data-cancel-request]")) cache.clear(); });
        return;
    }

    const filterButtons =
        Array.from(
            document.querySelectorAll(
                "[data-request-filter]"
            )
        );

    const requestCards =
        Array.from(
            document.querySelectorAll(
                "[data-request-card]"
            )
        );

    const filterEmptyState =
        document.getElementById(
            "request-filter-empty"
        );

    const serverEmptyState =
        document.getElementById(
            "request-server-empty"
        );

    const serverPaged = document.querySelector("[data-server-paged=true]") !== null;
    let currentFilter = "ALL";


    function normalizeStatus(status) {
        const normalizedStatus =
            String(status || "")
                .trim()
                .toUpperCase()
                .replaceAll(" ", "_");

        if (
            normalizedStatus === "APPROVED" ||
            normalizedStatus === "BORROWED" ||
            normalizedStatus === "OVERDUE" ||
            normalizedStatus === "IN_USE"
        ) {
            return "ACTIVE";
        }

        return normalizedStatus;
    }


    function getSearchKeyword() {
        const searchParameters =
            new URLSearchParams(
                window.location.search
            );

        return (
            searchParameters
                .get("keyword")
                ?.trim()
                .toLowerCase() || ""
        );
    }


    function isPreviewAuthenticated() {
        return (
            document.body.dataset.authenticated ===
            "true"
        );
    }


    function isStaticPreview() {
        return (
            window.location.port === "5500" ||
            window.location.port === "5501" ||
            window.location.pathname.includes(
                "/src/main/resources/templates/"
            )
        );
    }


    function cardMatchesFilter(card) {
        if (currentFilter === "ALL") {
            return true;
        }

        const cardStatus =
            normalizeStatus(
                card.dataset.requestStatus
            );

        return cardStatus === currentFilter;
    }


    function cardMatchesSearch(card) {
        if (serverPaged) return true;
        const keyword =
            getSearchKeyword();

        if (!keyword) {
            return true;
        }

        const searchableText =
            [
                card.textContent,
                card.dataset.requestStatus
            ]
                .filter(Boolean)
                .join(" ")
                .replace(/\s+/g, " ")
                .trim()
                .toLowerCase();

        return searchableText.includes(keyword);
    }


    function shouldShowFilterEmpty(
        visibleCardCount
    ) {
        if (visibleCardCount > 0) {
            return false;
        }

        if (requestCards.length === 0) {
            return false;
        }

        if (
            isStaticPreview() &&
            !isPreviewAuthenticated()
        ) {
            return false;
        }

        return true;
    }


    function updateFilterButtons() {
        filterButtons.forEach((button) => {
            const buttonFilter =
                button.dataset.requestFilter;

            const selected =
                buttonFilter === currentFilter;

            button.classList.toggle(
                "active",
                selected
            );

            button.setAttribute(
                "aria-pressed",
                String(selected)
            );
        });
    }


    function applyRequestFilter() {
        let visibleCardCount = 0;

        requestCards.forEach((card) => {
            const visible =
                cardMatchesFilter(card) &&
                cardMatchesSearch(card);

            card.hidden = !visible;

            if (visible) {
                visibleCardCount += 1;
            }
        });

        if (filterEmptyState) {
            filterEmptyState.hidden =
                !shouldShowFilterEmpty(
                    visibleCardCount
                );
        }

        /*
         * ถ้ามีการ์ดอยู่ แต่ Filter ไม่ตรง
         * จะซ่อน Server Empty State เพื่อไม่ให้
         * Empty State สองอันแสดงพร้อมกัน
         */
        if (
            serverEmptyState &&
            requestCards.length > 0
        ) {
            serverEmptyState.hidden = true;
        }

        updateFilterButtons();
    }


    function selectFilter(filter) {
        const normalizedFilter =
            normalizeStatus(filter);

        const allowedFilters =
            [
                "ALL",
                "ACTIVE",
                "PENDING",
                "RETURNED",
                "CANCELLED"
            ];

        currentFilter =
            allowedFilters.includes(
                normalizedFilter
            )
                ? normalizedFilter
                : "ALL";

        applyRequestFilter();
    }


    function initializeFilterFromUrl() {
        const searchParameters =
            new URLSearchParams(
                window.location.search
            );

        const status =
            searchParameters.get("status");

        if (status) {
            selectFilter(status);
            return;
        }

        selectFilter("ALL");
    }


    filterButtons.forEach((button) => {
        button.addEventListener(
            "click",
            () => {
                if (serverPaged) { const url=new URL(location.href); url.searchParams.set('status',button.dataset.requestFilter); url.searchParams.delete('page'); location.assign(url); return; }
                selectFilter(
                    button.dataset.requestFilter
                );
            }
        );
    });


    /*
     * site-preview.js จะส่ง Event นี้หลังจาก
     * โหลด Header, Footer และตรวจสอบ Login แล้ว
     */
    document.addEventListener(
        "leadit:site-ready",
        () => {
            applyRequestFilter();
        }
    );


    /*
     * ตรวจจับตอน Preview Login หรือ Logout
     * เพราะ site-preview.js จะเปลี่ยน
     * data-authenticated บน body
     */
    const authenticationObserver =
        new MutationObserver((mutations) => {
            const authenticationChanged =
                mutations.some((mutation) => {
                    return (
                        mutation.type ===
                            "attributes" &&
                        mutation.attributeName ===
                            "data-authenticated"
                    );
                });

            if (authenticationChanged) {
                applyRequestFilter();
            }
        });


    authenticationObserver.observe(
        document.body,
        {
            attributes: true,
            attributeFilter: [
                "data-authenticated"
            ]
        }
    );


    filterButtons.forEach(button => {
        const filter = button.dataset.requestFilter;
        const count = serverPaged ? Number(button.dataset.total || 0) : requestCards.filter(card => filter === "ALL" || normalizeStatus(card.dataset.requestStatus) === filter).length;
        const badge = document.createElement("span"); badge.className = "request-filter-count"; badge.textContent = count;
        button.append(badge);
    });
    initializeFilterFromUrl();
});
document.addEventListener("click", async event => {
    const button = event.target.closest("[data-cancel-request]");
    if (!button) return;
    button.disabled = true;
    try {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        const response = await fetch(`/api/v1/borrow-requests/${button.dataset.cancelRequest}/cancel`, {method:"PATCH", headers:{[header]:token}});
        if (!response.ok) throw new Error("Unable to cancel this request. Refresh and retry.");
        window.location.reload();
    } catch (error) { toast.error(error.message); button.disabled=false; }
});
