const toggleButton =
    document.getElementById("sidebar-toggle");

const sidebar =
    document.getElementById("main-sidebar");

const backdrop =
    document.getElementById("sidebar-backdrop");

function setSidebarOpen(isOpen) {
    document.body.classList.toggle(
        "sidebar-open",
        isOpen
    );

    toggleButton.setAttribute(
        "aria-expanded",
        String(isOpen)
    );

    toggleButton.setAttribute(
        "aria-label",
        isOpen ? "ปิดเมนู" : "เปิดเมนู"
    );

    sidebar.inert = !isOpen;
}

if (toggleButton && sidebar && backdrop) {
    setSidebarOpen(false);

    toggleButton.addEventListener("click", () => {
        const isOpen =
            document.body.classList.contains(
                "sidebar-open"
            );

        setSidebarOpen(!isOpen);
    });

    backdrop.addEventListener("click", () => {
        setSidebarOpen(false);
        toggleButton.focus();
    });

    document.addEventListener("keydown", (event) => {
        const isOpen =
            document.body.classList.contains(
                "sidebar-open"
            );

        if (event.key === "Escape" && isOpen) {
            setSidebarOpen(false);
            toggleButton.focus();
        }
    });
}

const equipmentSearch =
    document.getElementById("equipment-search");

const equipmentStatus =
    document.getElementById("equipment-status");

const equipmentCount =
    document.getElementById("equipment-count");

const equipmentFilterEmpty =
    document.getElementById("filter-empty-row");

const equipmentRows = Array.from(
    document.querySelectorAll(".equipment-row")
).filter((row) => {
    return row.dataset.name ||
           row.dataset.code ||
           row.dataset.status;
});

function filterEquipment() {
    if (!equipmentSearch || !equipmentStatus) {
        return;
    }

    const keyword =
        equipmentSearch.value.trim().toLowerCase();

    const selectedStatus =
        equipmentStatus.value;

    let visibleCount = 0;

    equipmentRows.forEach((row) => {
        const name =
            (row.dataset.name || "").toLowerCase();

        const code =
            (row.dataset.code || "").toLowerCase();

        const status =
            row.dataset.status || "";

        const matchesKeyword =
            name.includes(keyword) ||
            code.includes(keyword);

        const matchesStatus =
            selectedStatus === "" ||
            status === selectedStatus;

        const shouldShow =
            matchesKeyword && matchesStatus;

        row.style.display =
            shouldShow ? "table-row" : "none";

        if (shouldShow) {
            visibleCount++;
        }
    });

    if (equipmentCount && equipmentRows.length > 0) {
        equipmentCount.textContent = visibleCount;
    }

    if (equipmentFilterEmpty &&
        equipmentRows.length > 0) {

        equipmentFilterEmpty.style.display =
            visibleCount === 0
                ? "table-row"
                : "none";
    }
}

if (equipmentSearch && equipmentStatus) {
    equipmentSearch.addEventListener(
        "input",
        filterEquipment
    );

    equipmentStatus.addEventListener(
        "change",
        filterEquipment
    );
}

const borrowingSearch =
    document.getElementById("borrowing-search");

const borrowingStatus =
    document.getElementById("borrowing-status");

const borrowingCount =
    document.getElementById("borrowing-count");

const borrowingFilterEmpty =
    document.getElementById(
        "borrowing-filter-empty"
    );

const borrowingCards = Array.from(
    document.querySelectorAll(".borrowing-card")
).filter((card) => {
    return card.dataset.requestId ||
           card.dataset.status;
});

function getBorrowingSearchText(card) {
    const equipmentItems = Array.from(
        card.querySelectorAll(
            "[data-equipment-name]"
        )
    );

    const equipmentText = equipmentItems
        .map((item) => {
            const name =
                item.dataset.equipmentName || "";

            const code =
                item.dataset.equipmentCode || "";

            return `${name} ${code}`;
        })
        .join(" ");

    return [
        card.dataset.requestId || "",
        equipmentText
    ]
        .join(" ")
        .toLowerCase();
}

function filterBorrowingHistory() {
    if (!borrowingSearch || !borrowingStatus) {
        return;
    }

    const keyword =
        borrowingSearch.value.trim().toLowerCase();

    const selectedStatus =
        borrowingStatus.value;

    let visibleCount = 0;

    borrowingCards.forEach((card) => {
        const searchText =
            getBorrowingSearchText(card);

        const status =
            card.dataset.status || "";

        const matchesKeyword =
            searchText.includes(keyword);

        const matchesStatus =
            selectedStatus === "" ||
            status === selectedStatus;

        const shouldShow =
            matchesKeyword && matchesStatus;

        card.style.display =
            shouldShow ? "flex" : "none";

        if (shouldShow) {
            visibleCount++;
        }
    });

    if (borrowingCount &&
        borrowingCards.length > 0) {

        borrowingCount.textContent =
            visibleCount;
    }

    if (borrowingFilterEmpty &&
        borrowingCards.length > 0) {

        borrowingFilterEmpty.style.display =
            visibleCount === 0
                ? "block"
                : "none";
    }
}

if (borrowingSearch && borrowingStatus) {
    borrowingSearch.addEventListener(
        "input",
        filterBorrowingHistory
    );

    borrowingStatus.addEventListener(
        "change",
        filterBorrowingHistory
    );
}