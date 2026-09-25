document.addEventListener("DOMContentLoaded", () => {
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
                "RETURNED"
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


    initializeFilterFromUrl();
});