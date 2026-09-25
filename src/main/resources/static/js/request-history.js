const requestCards = Array.from(
    document.querySelectorAll(".request-card")
);

const requestTabs = Array.from(
    document.querySelectorAll(".request-tab")
);

const requestFilterEmpty =
    document.getElementById("request-filter-empty");

const requestSearchSummary =
    document.getElementById("request-search-summary");

const clearRequestFilterButton =
    document.getElementById("clear-request-filter");

let selectedRequestStatus = "ALL";
let requestKeyword = "";


function normalizeRequestText(value) {
    return String(value || "")
        .trim()
        .toLowerCase();
}


function statusMatchesFilter(status, filter) {
    if (filter === "ALL") {
        return true;
    }

    if (filter === "ACTIVE") {
        return [
            "APPROVED",
            "BORROWED",
            "OVERDUE"
        ].includes(status);
    }

    return status === filter;
}


function filterRequests() {
    if (requestCards.length === 0) {
        return;
    }

    const normalizedKeyword =
        normalizeRequestText(requestKeyword);

    let visibleCount = 0;

    requestCards.forEach((card) => {
        const status =
            String(card.dataset.requestStatus || "")
                .toUpperCase();

        const searchableText =
            normalizeRequestText(card.textContent);

        const matchesStatus =
            statusMatchesFilter(
                status,
                selectedRequestStatus
            );

        const matchesKeyword =
            normalizedKeyword === "" ||
            searchableText.includes(normalizedKeyword);

        const shouldShow =
            matchesStatus && matchesKeyword;

        card.hidden = !shouldShow;

        if (shouldShow) {
            visibleCount++;
        }
    });

    if (requestFilterEmpty) {
        requestFilterEmpty.hidden = visibleCount !== 0;
    }

    if (requestSearchSummary) {
        if (normalizedKeyword === "") {
            requestSearchSummary.hidden = true;
            requestSearchSummary.textContent = "";
        } else {
            requestSearchSummary.hidden = false;
            requestSearchSummary.textContent =
                `Search results for "${requestKeyword}" — ${visibleCount} request(s)`;
        }
    }
}


requestTabs.forEach((tab) => {
    tab.addEventListener("click", () => {
        requestTabs.forEach((currentTab) => {
            currentTab.classList.remove("active");
        });

        tab.classList.add("active");

        selectedRequestStatus =
            tab.dataset.requestFilter || "ALL";

        filterRequests();
    });
});


document
    .querySelectorAll(".request-details-toggle")
    .forEach((button) => {
        button.addEventListener("click", () => {
            const requestCard =
                button.closest(".request-card");

            const details =
                requestCard?.querySelector(
                    ".request-card-details"
                );

            if (!details) {
                return;
            }

            const isOpen = !details.hidden;

            details.hidden = isOpen;

            button.setAttribute(
                "aria-expanded",
                String(!isOpen)
            );

            button.textContent =
                isOpen
                    ? "View Details"
                    : "Hide Details";
        });
    });


clearRequestFilterButton?.addEventListener(
    "click",
    () => {
        selectedRequestStatus = "ALL";
        requestKeyword = "";

        requestTabs.forEach((tab) => {
            tab.classList.toggle(
                "active",
                tab.dataset.requestFilter === "ALL"
            );
        });

        const currentUrl =
            new URL(window.location.href);

        currentUrl.searchParams.delete("keyword");

        window.history.replaceState(
            {},
            "",
            currentUrl.pathname
        );

        if (searchInput) {
            searchInput.value = "";
        }

        setSearchOpen(false);
        filterRequests();
    }
);


if (requestCards.length > 0) {
    const searchParameters =
        new URLSearchParams(window.location.search);

    requestKeyword =
        searchParameters.get("keyword") || "";

    if (requestKeyword && searchInput) {
        searchInput.value = requestKeyword;
        setSearchOpen(true);
    }

    filterRequests();
}