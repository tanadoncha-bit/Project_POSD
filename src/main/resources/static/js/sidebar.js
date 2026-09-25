const menuButton = document.querySelector(".menu-toggle");
const navigation = document.getElementById("main-navigation");

const loginModal = document.getElementById("login-modal");
const registerModal = document.getElementById("register-modal");

const protectedActions = document.querySelectorAll(".requires-login");

const profileDropdown = document.querySelector(".profile-dropdown");
const profileTrigger = profileDropdown?.querySelector(".profile-trigger");
const profileMenu = profileDropdown?.querySelector(".profile-menu");

let lastFocusedElement = null;


/* ================================================================
   MOBILE MENU
   ================================================================ */

function setMenuOpen(isOpen) {
    document.body.classList.toggle("menu-open", isOpen);

    if (!menuButton) {
        return;
    }

    menuButton.setAttribute("aria-expanded", String(isOpen));
    menuButton.setAttribute(
        "aria-label",
        isOpen ? "ปิดเมนู" : "เปิดเมนู"
    );
}

menuButton?.addEventListener("click", () => {
    const isOpen = document.body.classList.contains("menu-open");

    setMenuOpen(!isOpen);
    setSearchOpen(false);
    setProfileMenuOpen(false);
});

navigation?.addEventListener("click", (event) => {
    if (event.target.closest("a")) {
        setMenuOpen(false);
    }
});

window.addEventListener("resize", () => {
    if (window.innerWidth > 820) {
        setMenuOpen(false);
    }
});


/* ================================================================
   SEARCH OVERLAY SLIDE ANIMATION 
   ================================================================ */

function setSearchOpen(isOpen) {
    // 1. ค้นหา Overlay โดยรองรับทั้งการใช้ ID และ Class
    const searchOverlay = document.getElementById("search-overlay") || document.querySelector(".search-overlay");
    const searchInput = document.getElementById("global-search-input") || document.querySelector(".search-overlay-form input");

    if (!searchOverlay) {
        console.error("❌ ไม่พบแท็ก Search Overlay! ตรวจสอบว่าใน HTML มี <div class='search-overlay'> หรือไม่");
        return;
    }

    if (isOpen) {
        // เอา hidden ออกเพื่อให้มีสถานะอยู่ในหน้าเว็บ
        searchOverlay.removeAttribute("hidden");
        searchOverlay.style.display = "block";

        // ⭐️ ทริคสำคัญ: บังคับให้เบราว์เซอร์อัปเดตสถานะ (Reflow) ก่อนที่จะใส่ Transition ไม่งั้นมันจะไม่เล่น Animation
        void searchOverlay.offsetWidth;

        // สั่งสไลด์ลงมา
        searchOverlay.classList.add("is-open");
        document.body.classList.add("modal-open");

        // ปิดเมนูอื่นๆ
        if (typeof setMenuOpen === "function") setMenuOpen(false);
        if (typeof setProfileMenuOpen === "function") setProfileMenuOpen(false);

        // โฟกัสช่องพิมพ์
        setTimeout(() => {
            if (searchInput) searchInput.focus();
        }, 300);

    } else {
        // ดึงขึ้น
        searchOverlay.classList.remove("is-open");
        document.body.classList.remove("modal-open");

        // รอ Animation เสร็จค่อยซ่อน
        setTimeout(() => {
            if (!searchOverlay.classList.contains("is-open")) {
                searchOverlay.setAttribute("hidden", "true");
                searchOverlay.style.display = "none";
            }
        }, 350);
    }
}

// ผูก Event Listener (เพิ่มคลาสปุ่มต่างๆ ให้ครอบคลุมมากขึ้น)
document.addEventListener("click", (event) => {
    // เช็คว่าปุ่มที่กด เป็นปุ่มเปิดค้นหาหรือไม่
    const openSearchBtn = event.target.closest("[data-open-search], .search-toggle, .header-search .icon-button");
    const closeSearchBtn = event.target.closest("[data-close-search], .search-overlay-close");

    if (openSearchBtn) {
        event.preventDefault();
        event.stopPropagation();
        setSearchOpen(true);
    }

    if (closeSearchBtn) {
        event.preventDefault();
        event.stopPropagation();
        setSearchOpen(false);
    }
});


/* ================================================================
   PROFILE MENU
   ================================================================ */

function setProfileMenuOpen(isOpen) {
    if (!profileTrigger || !profileMenu) {
        return;
    }

    profileMenu.hidden = !isOpen;
    profileTrigger.setAttribute("aria-expanded", String(isOpen));

    if (isOpen) {
        setSearchOpen(false);
        setMenuOpen(false);
    }
}

profileTrigger?.addEventListener("click", (event) => {
    event.stopPropagation();

    const isOpen = profileTrigger.getAttribute("aria-expanded") === "true";

    setProfileMenuOpen(!isOpen);
});

profileMenu?.addEventListener("click", (event) => {
    event.stopPropagation();
});


/* ================================================================
   AUTH MODALS
   ================================================================ */

function setModalOpen(modal, isOpen) {
    if (!modal) {
        return;
    }

    modal.hidden = !isOpen;

    const hasOpenModal =
        (loginModal && !loginModal.hidden) ||
        (registerModal && !registerModal.hidden);

    document.body.classList.toggle("modal-open", Boolean(hasOpenModal));

    if (isOpen) {
        const closeButton = modal.querySelector(".modal-close");

        window.setTimeout(() => {
            closeButton?.focus();
        }, 50);
    }
}

function openLoginModal(trigger = null) {
    lastFocusedElement = trigger;

    setSearchOpen(false);
    setMenuOpen(false);
    setProfileMenuOpen(false);
    setModalOpen(registerModal, false);
    setModalOpen(loginModal, true);
}

function closeLoginModal() {
    setModalOpen(loginModal, false);
    lastFocusedElement?.focus();
}

function openRegisterModal() {
    setModalOpen(loginModal, false);
    setModalOpen(registerModal, true);
}

function closeRegisterModal() {
    setModalOpen(registerModal, false);
    lastFocusedElement?.focus();
}

protectedActions.forEach((action) => {
    action.addEventListener("click", (event) => {
        event.preventDefault();
        openLoginModal(action);
    });
});

loginModal
    ?.querySelector(".modal-close")
    ?.addEventListener("click", closeLoginModal);

registerModal
    ?.querySelector(".modal-close")
    ?.addEventListener("click", closeRegisterModal);

loginModal?.addEventListener("click", (event) => {
    if (event.target === loginModal) {
        closeLoginModal();
    }
});

registerModal?.addEventListener("click", (event) => {
    if (event.target === registerModal) {
        closeRegisterModal();
    }
});

document.querySelectorAll(".open-register").forEach((button) => {
    button.addEventListener("click", openRegisterModal);
});

document.querySelectorAll(".open-login").forEach((button) => {
    button.addEventListener("click", () => {
        setModalOpen(registerModal, false);
        setModalOpen(loginModal, true);
    });
});


/* ================================================================
   CLOSE WHEN CLICKING OUTSIDE
   ================================================================ */

document.addEventListener("click", (event) => {
    if (
        profileMenu &&
        !profileMenu.hidden &&
        !profileDropdown.contains(event.target)
    ) {
        setProfileMenuOpen(false);
    }
});


/* ================================================================
   ESCAPE KEY
   ================================================================ */

document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
        return;
    }

    if (registerModal && !registerModal.hidden) {
        closeRegisterModal();
        return;
    }

    if (loginModal && !loginModal.hidden) {
        closeLoginModal();
        return;
    }

    const searchOverlay = document.getElementById("search-overlay");
    if (searchOverlay && searchOverlay.classList.contains("is-open")) {
        setSearchOpen(false);
        return;
    }

    if (profileMenu && !profileMenu.hidden) {
        setProfileMenuOpen(false);
        profileTrigger?.focus();
        return;
    }

    setMenuOpen(false);
});


/* ================================================================
   OPEN LOGIN MODAL AFTER LOGIN ERROR
   ================================================================ */

const hasLoginError = document.body.dataset.loginError === "true";

if (hasLoginError) {
    openLoginModal();
}

/* ================================================================
   EQUIPMENT CATALOG SEARCH
   ================================================================ */

const catalogCards = Array.from(
    document.querySelectorAll(".catalog-equipment-card")
);

const catalogSearchSummary =
    document.getElementById("catalog-search-summary");

const catalogFilterEmpty =
    document.getElementById("catalog-filter-empty");

const clearCatalogSearchButton =
    document.getElementById("clear-catalog-search");

function normalizeSearchText(value) {
    return String(value || "")
        .trim()
        .toLowerCase();
}

function filterEquipmentCatalog(keyword) {
    if (catalogCards.length === 0) {
        return;
    }

    const normalizedKeyword = normalizeSearchText(keyword);
    let visibleCount = 0;

    catalogCards.forEach((card) => {
        const equipmentName =
            normalizeSearchText(card.dataset.name);

        const equipmentStatus =
            normalizeSearchText(card.dataset.status);

        const matchesSearch =
            normalizedKeyword === "" ||
            equipmentName.includes(normalizedKeyword) ||
            equipmentStatus.includes(normalizedKeyword);

        card.hidden = !matchesSearch;

        if (matchesSearch) {
            visibleCount++;
        }
    });

    if (catalogSearchSummary) {
        if (normalizedKeyword === "") {
            catalogSearchSummary.hidden = true;
            catalogSearchSummary.textContent = "";
        } else {
            catalogSearchSummary.hidden = false;
            catalogSearchSummary.textContent =
                `Search results for "${keyword}" — ${visibleCount} item(s)`;
        }
    }

    if (catalogFilterEmpty) {
        catalogFilterEmpty.hidden =
            visibleCount !== 0 || normalizedKeyword === "";
    }
}

function clearCatalogSearch() {
    const currentUrl = new URL(window.location.href);

    currentUrl.searchParams.delete("keyword");

    window.history.replaceState(
        {},
        "",
        currentUrl.pathname
    );

    const searchInput = document.getElementById("global-search-input");
    if (searchInput) {
        searchInput.value = "";
    }

    filterEquipmentCatalog("");
    setSearchOpen(false);
}

if (catalogCards.length > 0) {
    const searchParameters =
        new URLSearchParams(window.location.search);

    const keyword =
        searchParameters.get("keyword") || "";

    const searchInput = document.getElementById("global-search-input");
    if (keyword && searchInput) {
        searchInput.value = keyword;
        setSearchOpen(true);
    }

    filterEquipmentCatalog(keyword);
}

clearCatalogSearchButton?.addEventListener(
    "click",
    clearCatalogSearch
);