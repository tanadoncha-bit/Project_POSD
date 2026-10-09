/* Shared equipment details and a session-persistent, account-scoped borrowing list. */
(() => {
    const authenticated = () => document.body.dataset.authenticated === "true";
    const pendingKey = "leadit.borrowList.pendingAdd";
    // Guests may inspect equipment, but have no borrowing list.
    try { sessionStorage.removeItem("leadit.borrowList.guest"); } catch { }
    const key = "leadit.borrowList." + (document.body.dataset.username || "guest");
    let items = [];
    try { const saved = JSON.parse(sessionStorage.getItem(key) || "[]"); if (authenticated() && Array.isArray(saved)) items = saved.filter(x => Number.isSafeInteger(x.id) && x.id > 0 && typeof x.name === "string"); } catch { }
    function node(tag, text, className) { const el = document.createElement(tag); if (text) el.textContent = text; if (className) el.className = className; return el; }
    const launcher = node("button", "", "borrow-list-launcher"); launcher.type = "button";
    const launcherIcon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    launcherIcon.setAttribute("viewBox", "0 0 24 24");
    launcherIcon.setAttribute("aria-hidden", "true");
    launcherIcon.innerHTML = '<path d="M6 7h12l1 13H5L6 7Z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>';
    const launcherCount = node("span", "", "borrow-list-count");
    launcher.append(launcherIcon, launcherCount);
    function placeLauncher() {
        const actions = document.querySelector(".site-header .header-actions");
        if (actions) { actions.prepend(launcher); launcher.classList.add("borrow-list-header"); return true; }
        return false;
    }
    document.body.append(launcher);
    if (!placeLauncher()) {
        const headerObserver = new MutationObserver(() => { if (placeLauncher()) headerObserver.disconnect(); });
        headerObserver.observe(document.body, { childList: true, subtree: true });
    }
    function save() { try { sessionStorage.setItem(key, JSON.stringify(items)); } catch { } update(); }
    function update() { launcherCount.textContent = String(items.length); launcher.setAttribute("aria-label", `Borrowing list (${items.length})`); launcher.title = `Borrowing list (${items.length})`; }
    async function validateSavedItems() {
        if (!authenticated() || !items.length) return;
        const checks = await Promise.all(items.map(async item => {
            try {
                const response = await fetch(`/api/v1/equipment/${encodeURIComponent(item.id)}`);
                if (response.status === 404) return item.id;
                if (!response.ok) return null; // Preserve selections during network/server failures.
                const equipment = await response.json();
                return equipment.status === 'AVAILABLE' && !equipment.reserved ? null : item.id;
            } catch { return null; }
        }));
        const invalid = new Set(checks.filter(id => id !== null));
        if (invalid.size) { items = items.filter(item => !invalid.has(item.id)); save(); }
    }
    validateSavedItems();
    function makeDialog(className, title) {
        const dialog = node("dialog", "", className);
        const heading = node("h2", title); heading.id = className + "-title";
        dialog.setAttribute("aria-labelledby", heading.id);
        const header = node("header"); const close = node("button", "Close"); close.type = "button"; close.onclick = () => dialog.close();
        if (className === "borrow-item-dialog") {
            close.textContent = "\u00d7";
            close.setAttribute("aria-label", "Close equipment details");
        }
        header.append(heading, close); dialog.append(header);
        let pressedOutside = false;
        function outside(event) {
            const rect = dialog.getBoundingClientRect();
            return event.target === dialog && (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom);
        }
        dialog.addEventListener("pointerdown", event => { pressedOutside = outside(event); });
        dialog.addEventListener("click", event => { if (pressedOutside && outside(event)) dialog.close(); pressedOutside = false; });
        const previous = document.activeElement;
        dialog.addEventListener("close", () => {
            const notifications = dialog.querySelector(".toast-region"); if (notifications) document.body.append(notifications);
            dialog.remove();
            if (!document.querySelector('dialog[open], .search-overlay:not([hidden]), .modal-backdrop:not([hidden])')) document.body.classList.remove("modal-open");
            if (previous?.isConnected) previous.focus({ preventScroll: true });
        }, { once: true });
        document.body.append(dialog); dialog.showModal(); document.body.classList.add("modal-open"); return dialog;
    }
    async function getEquipment(id) {
        const response = await fetch(`/api/v1/equipment/${encodeURIComponent(id)}`);
        if (!response.ok) throw new Error("Unable to load equipment. Please try again.");
        return response.json();
    }
    function add(equipment) {
        if (!authenticated() || (equipment.status !== "AVAILABLE" || equipment.reserved)) return;
        if (!items.some(x => x.id === equipment.id)) items.push({ id: equipment.id, name: equipment.name, assetCode: equipment.assetCode });
        save();
    }
    async function details(id) {
        if (document.querySelector(".borrow-item-dialog[open]")) return;
        const dialog = makeDialog("borrow-item-dialog", "Equipment details");
        const body = node("div", "", "borrow-list-body");
        body.setAttribute("aria-busy", "true");
        const loading = node("div", "", "borrow-detail-overview detail-loading");
        loading.setAttribute("aria-hidden", "true");
        const preview = node("div", "", "borrow-detail-media detail-skeleton");
        const lines = node("div", "", "detail-loading-lines");
        for (const style of ["short", "title", "badge", "line", "line", "line", "line"]) {
            lines.append(node("span", "", `detail-skeleton detail-skeleton-${style}`));
        }
        loading.append(preview, lines);
        const announcement = node("span", "Loading equipment details", "detail-loading-announcement");
        announcement.setAttribute("role", "status");
        body.append(loading, announcement);
        const actions = node("footer", "", "borrow-detail-actions");
        const pendingAction = node("button", "Loading details...", "button button-primary");
        pendingAction.disabled = true;
        actions.append(pendingAction);
        dialog.append(body, actions);
        try {
            const [equipment, categories] = await Promise.all([
                getEquipment(id),
                fetch("/api/v1/categories").then(response => response.ok ? response.json() : []).catch(() => [])
            ]);
            if (!dialog.isConnected) return;
            const category = categories.find(x => x.id === equipment.categoryId)?.name || "IT Equipment";
            const media = node("div", "", "borrow-detail-media");
            if (equipment.imageUrl && (equipment.imageUrl.startsWith("https://") || equipment.imageUrl.startsWith("/images/"))) {
                const image = node("img"); image.src = equipment.imageUrl; image.alt = equipment.name;
                image.onerror = () => media.replaceChildren(node("span", "No image available")); media.append(image);
            } else media.append(node("span", "No image available"));
            const specs = node("section", "", "borrow-detail-specifications");
            const specRows = node("div");
            window.renderEquipmentSpecs(specRows, equipment.specifications);
            specs.append(node("h4", "Specifications"), specRows);
            const identity = node("div", "", "borrow-detail-identity");
            const badge = node("span", equipment.reserved ? "Reserved" : { AVAILABLE: "Available", IN_USE: "In use", MAINTENANCE: "Maintenance", DISPOSED: "Disposed" }[equipment.status] || "Unknown", "borrow-detail-status");
            badge.dataset.status = equipment.status;
            badge.dataset.available = String(equipment.status === "AVAILABLE" && !equipment.reserved);
            identity.append(node("p", category, "borrow-detail-category"), node("h3", equipment.name), badge);
            const overview = node("div", "", "borrow-detail-overview"); overview.append(media, identity);
            identity.append(specs);
            body.replaceChildren(overview);
            const action = node("button", items.some(x => x.id === equipment.id) ? "View borrowing list" : "Add to borrowing list", "button button-primary");
            action.disabled = (equipment.status !== "AVAILABLE" || equipment.reserved);
            if (action.disabled) action.textContent = "Currently unavailable";
            action.onclick = () => {
                if (!authenticated()) {
                    try {
                        sessionStorage.setItem(pendingKey, JSON.stringify({
                            id: equipment.id,
                            path: location.pathname + location.search, expires: Date.now() + 5 * 60 * 1000
                        }));
                    } catch { }
                    dialog.close();
                    setModalOpen("login-modal", true);
                    return;
                }
                const alreadyAdded = items.some(item => item.id === equipment.id);
                if (alreadyAdded) { dialog.close(); openList(); return; }
                add(equipment);
                dialog.close();
                toast.success("Added to your borrowing list. You can keep browsing.");
            };
            actions.replaceChildren(action);
        } catch (error) {
            if (!dialog.isConnected) return;
            const message = node("p", error.message, "detail-load-error");
            message.setAttribute("role", "alert");
            body.replaceChildren(message);
            const close = node("button", "Close", "button button-primary");
            close.onclick = () => dialog.close();
            actions.replaceChildren(close);
        } finally {
            body.setAttribute("aria-busy", "false");
        }
    }
    function openList() {
        if (!authenticated()) { setModalOpen("login-modal", true); return; }
        if (document.querySelector(".borrow-list-drawer[open]")) return;
        const dialog = makeDialog("borrow-list-drawer", "Borrowing list");
        const body = node("div", "", "borrow-list-body");
        const footer = node("footer"); const checkout = node("button", "Continue to borrow", "button button-primary");
        footer.append(node("p", "One return date applies to every item in this request."), checkout); dialog.append(body, footer);
        function render() {
            body.replaceChildren(); checkout.disabled = !items.length;
            body.classList.toggle("borrow-list-empty", !items.length); footer.hidden = !items.length;
            if (!items.length) {
                body.append(node("p", "Your list is empty. Choose equipment to add."));
                const browse = node("a", "Browse equipment", "button button-dark"); browse.href = "/equipment"; body.append(browse);
            }
            items.forEach(item => { const row = node("div", "", "borrow-list-row"); const remove = node("button", "Remove"); remove.setAttribute("aria-label", `Remove ${item.name}`); remove.onclick = () => { items = items.filter(x => x.id !== item.id); save(); render(); }; const identity = node("div"); identity.append(node("span", item.name)); if (item.assetCode) identity.append(node("small", item.assetCode)); row.append(identity, remove); body.append(row); });
        }
        checkout.onclick = async () => {
            if (!isAuthenticated()) { dialog.close(); setModalOpen("login-modal", true); return; }
            checkout.disabled = true;
            try {
                const equipment = await Promise.all(items.map(item => getEquipment(item.id)));
                if (!dialog.isConnected) return;
                const unavailable = equipment.filter(x => (x.status !== "AVAILABLE" || x.reserved));
                if (unavailable.length) throw new Error("Remove unavailable items: " + unavailable.map(x => x.name).join(", "));
                const ids = items.map(x => x.id); dialog.close(); window.openBorrowModal(ids);
            } catch (error) { toast.error(error.message); }
            finally { checkout.disabled = !items.length; }
        };
        render();
    }
    window.borrowList = { showDetails: details, removeMany(ids) { const selected = new Set(ids.map(String)); items = items.filter(x => !selected.has(String(x.id))); save(); }, open: openList };
    launcher.onclick = () => { document.dispatchEvent(new CustomEvent("header-menu-open", { detail: "borrow-list" })); openList(); }; update();
    async function resumePendingAdd() {
        if (!authenticated()) return;
        let pending;
        try { pending = JSON.parse(sessionStorage.getItem(pendingKey) || "null"); } catch { return; }
        if (!pending) return;
        if (!Number.isSafeInteger(pending.id) || pending.id <= 0 || !(pending.expires > Date.now())) {
            sessionStorage.removeItem(pendingKey); return;
        }
        const destination = new URL(pending.path || "/equipment", location.origin);
        const allowed = destination.origin === location.origin &&
            (/^\/equipment(?:\/\d+)?$/.test(destination.pathname) || ["/", "/Dashboard", "/my-requests"].includes(destination.pathname));
        if (allowed && destination.pathname + destination.search !== location.pathname + location.search) {
            location.replace(destination.href); return;
        }
        sessionStorage.removeItem(pendingKey);
        try {
            const equipment = await getEquipment(pending.id);
            if ((equipment.status !== "AVAILABLE" || equipment.reserved)) {
                toast.error("This equipment is no longer available."); return;
            }
            add(equipment);
            toast.success("Added to your borrowing list. You can keep browsing.");
        } catch (error) { toast.error(error.message); }
    }
    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", resumePendingAdd, { once: true });
    else resumePendingAdd();
    // Capture before older catalog handlers so every entry point has the same flow.
    document.addEventListener("click", event => {
        const target = event.target.closest(".search-equipment-card, [data-equipment-card], .equipment-detail-borrow, #open-borrow-modal-btn");
        if (!target) return;
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); event.stopImmediatePropagation();
        if (target.id === "open-borrow-modal-btn") { openList(); return; }
        const url = target.href ? new URL(target.href) : null;
        const id = target.dataset.equipmentId || url?.searchParams.get("equipmentId") || url?.pathname.match(/\/equipment\/(\d+)$/)?.[1];
        if (id) details(id);
    }, true);
})();
