document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("equipment-detail-modal");
    const closeButton = document.getElementById("equipment-modal-close");
    const cancelButton = document.getElementById("equipment-modal-cancel");
    const borrowButton = document.getElementById("equipment-borrow-button");
    const borrowLabel = document.getElementById("equipment-borrow-label");

    const modalName = document.getElementById("equipment-modal-name");
    const modalCategory = document.getElementById("equipment-modal-category");
    const modalImage = document.getElementById("equipment-modal-image");
    const modalImagePlaceholder = document.getElementById("equipment-modal-image-placeholder");

    const modalCode = document.getElementById("equipment-modal-code");
    const modalProcessor = document.getElementById("equipment-modal-processor");
    const modalMemory = document.getElementById("equipment-modal-memory");
    const modalStorage = document.getElementById("equipment-modal-storage");
    const modalScreen = document.getElementById("equipment-modal-screen");
    const modalStatusText = document.getElementById("equipment-modal-status-text");
    const modalLocker = document.getElementById("equipment-modal-locker");
    const modalStatusBadge = document.getElementById("equipment-modal-status");

    const equipmentCards = document.querySelectorAll("[data-equipment-card]");

    let selectedEquipmentId = "";
    let lastFocusedElement = null;

    if (!modal || !closeButton) {
        return;
    }

    function normalizeStatus(status) {
        return String(status || "").trim().toUpperCase().replaceAll(" ", "_");
    }

    function isAvailableStatus(status) {
        return normalizeStatus(status) === "AVAILABLE";
    }

    function getCardImage(card) {
        const image = card.querySelector(".equipment-card-image img");
        if (!image || image.hidden) return "";
        return image.currentSrc || image.getAttribute("src") || "";
    }

    function setModalImage(card, equipmentName) {
        if (!modalImage || !modalImagePlaceholder) return;
        const imageSource = getCardImage(card);

        if (imageSource) {
            modalImage.src = imageSource;
            modalImage.alt = equipmentName;
            modalImage.hidden = false;
            modalImagePlaceholder.hidden = true;
            return;
        }

        modalImage.removeAttribute("src");
        modalImage.alt = "";
        modalImage.hidden = true;
        modalImagePlaceholder.hidden = false;
    }

    function setModalStatus(status) {
        const available = isAvailableStatus(status);

        if (modalStatusText) modalStatusText.textContent = available ? "Available" : "In Used";

        if (modalStatusBadge) {
            modalStatusBadge.textContent = available ? "Available" : "In Used";
            modalStatusBadge.classList.toggle("equipment-locker-offline", !available);
        }

        if (borrowButton) {
            borrowButton.disabled = !available;
            borrowButton.classList.toggle("button-disabled", !available);
            borrowButton.setAttribute("aria-disabled", String(!available));
        }

        if (borrowLabel) {
            borrowLabel.textContent = available ? "Borrow" : "Unavailable";
        }
    }

    function openEquipmentModal(card) {
        selectedEquipmentId = card.dataset.equipmentId || "";

        const equipmentName = card.dataset.name || card.querySelector(".equipment-card-body h2")?.textContent.trim() || "Equipment";
        const category = card.dataset.category || card.querySelector(".equipment-card-body p")?.textContent.trim() || "IT Equipment";
        const status = card.dataset.status || card.querySelector(".availability-badge")?.textContent.trim() || "AVAILABLE";

        if (modalName) modalName.textContent = equipmentName;
        if (modalCategory) modalCategory.textContent = category;
        if (modalCode) modalCode.textContent = card.dataset.code || "-";
        if (modalProcessor) modalProcessor.textContent = card.dataset.processor || "-";
        if (modalMemory) modalMemory.textContent = card.dataset.memory || "-";
        if (modalStorage) modalStorage.textContent = card.dataset.storage || "-";
        if (modalScreen) modalScreen.textContent = card.dataset.screen || "-";
        if (modalLocker) modalLocker.textContent = card.dataset.locker || "Locker information pending";

        setModalImage(card, equipmentName);
        setModalStatus(status);

        lastFocusedElement = document.activeElement;
        modal.hidden = false;
        document.body.classList.add("modal-open");
        closeButton.focus();
    }

    function closeEquipmentModal(options = {}) {
        const { restoreFocus = true, keepBodyLocked = false } = options;
        modal.hidden = true;

        if (!keepBodyLocked) {
            document.body.classList.remove("modal-open");
        }

        if (restoreFocus && lastFocusedElement instanceof HTMLElement) {
            lastFocusedElement.focus();
        }
    }

    function getLoginModal() {
        return document.getElementById("login-modal");
    }

    function loginModalIsOpen() {
        const loginModal = getLoginModal();
        return Boolean(loginModal && !loginModal.hidden);
    }

    function openBorrowForm(event) {
        if (!borrowButton || borrowButton.disabled) {
            event.preventDefault();
            return;
        }

        window.setTimeout(() => {
            if (loginModalIsOpen()) {
                closeEquipmentModal({ restoreFocus: false, keepBodyLocked: true });
                document.body.classList.add("modal-open");
                getLoginModal()?.querySelector("input, button")?.focus();
                return;
            }

            if (event.defaultPrevented) return;

            // ปิด Detail Modal แล้วเปิด Borrow Form Popup บนหน้าเดิม
            closeEquipmentModal({ restoreFocus: false, keepBodyLocked: true });

            if (typeof window.openBorrowModal === "function") {
                window.openBorrowModal(selectedEquipmentId);
            } else {
                const borrowFormModal = document.getElementById("borrow-form-modal");
                if (borrowFormModal) {
                    const checkboxes = borrowFormModal.querySelectorAll('input[name="equipmentIds"]');
                    checkboxes.forEach(cb => {
                        cb.checked = (cb.value === String(selectedEquipmentId));
                    });
                    borrowFormModal.removeAttribute("hidden");
                }
            }
        }, 0);
    }

    function applyKeywordFilter() {
        const parameters = new URLSearchParams(window.location.search);
        const keyword = parameters.get("keyword")?.trim().toLowerCase();

        if (!keyword) return;

        equipmentCards.forEach((card) => {
            const searchableText = [
                card.dataset.name,
                card.dataset.category,
                card.dataset.code
            ].filter(Boolean).join(" ").toLowerCase();

            card.hidden = !searchableText.includes(keyword);
        });
    }

    equipmentCards.forEach((card) => {
        card.addEventListener("click", () => {
            openEquipmentModal(card);
        });

        card.addEventListener("keydown", (event) => {
            if (event.key !== "Enter" && event.key !== " ") return;
            event.preventDefault();
            openEquipmentModal(card);
        });
    });

    closeButton.addEventListener("click", () => closeEquipmentModal());
    cancelButton?.addEventListener("click", () => closeEquipmentModal());
    borrowButton?.addEventListener("click", openBorrowForm);

    modal.addEventListener("click", (event) => {
        if (event.target === modal) closeEquipmentModal();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) {
            closeEquipmentModal();
        }
    });

    applyKeywordFilter();

    function openRequestedEquipment() {
        const parameters = new URLSearchParams(window.location.search);
        const requestedEquipmentId = parameters.get("equipmentId");

        if (!requestedEquipmentId) return;

        const requestedCard = Array.from(equipmentCards).find((card) => {
            return card.dataset.equipmentId === requestedEquipmentId;
        });

        if (requestedCard) {
            openEquipmentModal(requestedCard);
        }
    }

    openRequestedEquipment();
});