(() => {
    let loading = false;
    window.openBorrowModal = async (equipmentId) => {
        if (!isAuthenticated()) { setModalOpen("login-modal", true); return; }
        if (loading || document.querySelector(".borrow-dialog[open]")) return;
        loading = true;
        const trigger = document.activeElement;
        trigger?.setAttribute("aria-busy", "true");
        try {
            const url = new URL("/borrow", window.location.origin);
            url.searchParams.set("modal", "true");
            if (equipmentId && !Array.isArray(equipmentId)) url.searchParams.set("equipmentId", equipmentId);
            const response = await fetch(url, { credentials: "same-origin" });
            if (!response.ok) throw new Error("Unable to load available equipment. Please try again.");
            const page = new DOMParser().parseFromString(await response.text(), "text/html");
            const form = page.getElementById("borrow-request-form");
            if (!form) throw new Error("Your session may have expired. Please sign in again.");
            if (Array.isArray(equipmentId)) {
                const selected = new Set(equipmentId.map(String));
                const inputs = [...form.querySelectorAll('input[name="equipmentIds"]')];
                if ([...selected].some(id => !inputs.some(input => input.value === id))) {
                    throw new Error("Some selected equipment is no longer available. Review your borrowing list.");
                }
                inputs.forEach(input => { input.checked = selected.has(input.value); input.closest("label").hidden = !input.checked; });
                form.dataset.borrowList = "true";
            }
            const dialog = document.createElement("dialog");
            dialog.className = "borrow-dialog";
            form.querySelector("h1").id = "borrow-dialog-title";
            dialog.setAttribute("aria-labelledby", "borrow-dialog-title");
            dialog.append(document.importNode(form, true));
            document.body.append(dialog);
            dialog.addEventListener("close", () => {
                const notifications = dialog.querySelector(".toast-region");
                if (notifications) document.body.append(notifications);
                dialog.remove();
                if (!document.querySelector('dialog[open], .equipment-detail-backdrop:not([hidden]), .modal-backdrop:not([hidden])')) {
                    document.body.classList.remove("modal-open");
                }
                if (trigger?.isConnected) trigger.focus({ preventScroll: true });
            }, { once: true });
            dialog.addEventListener("click", event => {
                if (event.target !== dialog) return;
                const rect = dialog.getBoundingClientRect();
                if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();
            });
            document.dispatchEvent(new Event("borrow-form-loaded"));
            dialog.showModal();
            document.body.classList.add("modal-open");
        } catch (error) { toast.error(error.message); }
        finally { loading = false; trigger?.removeAttribute("aria-busy"); }
    };
    document.querySelector(".equipment-detail-borrow")?.addEventListener("click", event => {
        event.preventDefault();
        const link = event.currentTarget;
        if (link.getAttribute("aria-disabled") === "true") return;
        window.openBorrowModal(new URL(link.href).searchParams.get("equipmentId"));
    });
    document.getElementById("open-borrow-modal-btn")?.addEventListener("click", () => window.openBorrowModal());
})();
