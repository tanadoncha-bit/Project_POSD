document.addEventListener("DOMContentLoaded", () => {
    const modal =
        document.getElementById("equipment-detail-modal");

    const closeButton =
        document.getElementById("equipment-modal-close");

    const cancelButton =
        document.getElementById("equipment-modal-cancel");

    const modalName =
        document.getElementById("equipment-modal-name");

    const modalCategory =
        document.getElementById("equipment-modal-category");

    const modalStatus =
        document.getElementById("equipment-modal-status");

    const modalAvailability =
        document.getElementById("equipment-modal-availability");

    const borrowButton =
        document.getElementById("equipment-borrow-button");

    const equipmentCards = document.querySelectorAll(
    ".catalog-grid .equipment-card, .equipment-preview .equipment-card");

    if (!modal || !closeButton) {
        return;
    }

    function openEquipmentModal(card) {
        const name =
            card.querySelector(".equipment-card-body h2, .equipment-card-body h3")
                ?.textContent.trim() || "Equipment";

        const category =
            card.querySelector(".equipment-card-body p")
                ?.textContent.trim() || "IT Equipment";

        const statusElement =
            card.querySelector(".availability-badge");

        const status =
            statusElement?.textContent.trim() || "Available";

        modalName.textContent = name;
        modalCategory.textContent = category;
        
        const isAvailable =
            status.toLowerCase() === "available";

    modalStatus.textContent = isAvailable
        ? "Slot Online"
        : "Slot Offline";

    modalStatus.className = isAvailable
        ? "equipment-locker-status"
        : "equipment-locker-status equipment-locker-offline";
        modalAvailability.textContent = isAvailable
            ? "Ready to borrow"
            : "Not available for borrowing";

        borrowButton.disabled = !isAvailable;

        borrowButton.textContent = isAvailable
            ? "Borrow Equipment"
            : "Currently Unavailable";

        modal.hidden = false;
        document.body.classList.add("modal-open");

        closeButton.focus();
    }

    function closeEquipmentModal() {
        modal.hidden = true;
        document.body.classList.remove("modal-open");
    }

    equipmentCards.forEach((card) => {
        card.addEventListener("click", (event) => {
            event.preventDefault();
            openEquipmentModal(card);
        });
    });

    closeButton.addEventListener(
        "click",
        closeEquipmentModal
    );

    if (cancelButton) {
        cancelButton.addEventListener(
            "click",
            closeEquipmentModal
        );
    }

    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeEquipmentModal();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) {
            closeEquipmentModal();
        }
    });
});