document.addEventListener("DOMContentLoaded", () => {
    const page = document.body;
    const profileToggle = document.querySelector("[data-admin-profile-toggle]");
    const profileMenu = document.getElementById("admin-profile-menu");
    function closeProfileMenu(restoreFocus = false) {
        profileMenu.hidden = true; profileToggle.setAttribute("aria-expanded", "false");
        if (restoreFocus) profileToggle.focus();
    }
    profileToggle.addEventListener("click", () => {
        profileMenu.hidden = !profileMenu.hidden;
        profileToggle.setAttribute("aria-expanded", String(!profileMenu.hidden));
    });
    document.addEventListener("click", event => { if (!event.target.closest(".admin-profile-dropdown")) closeProfileMenu(); });
    document.addEventListener("keydown", event => { if (event.key === "Escape" && !profileMenu.hidden) { closeProfileMenu(true); } });
    document.querySelector(".admin-profile-dropdown").addEventListener("focusout", event => {
        if (!event.currentTarget.contains(event.relatedTarget)) closeProfileMenu();
    });
    const inventory = document.getElementById("admin-inventory-grid");
    const requestList = document.getElementById("admin-request-list");
    const search = document.querySelector("[data-admin-inventory-search]");
    const statusFilter = document.querySelector("[data-admin-status-filter]");
    let selectedEquipmentId = null;
    let selectedRequestId = null;
    let equipments = new Map();
    let requests = new Map();
    let categories = [];
    let assetPage = 0, assetTotal = 0, assetLoadVersion = 0;
    let requestPage = 0;
    let requestLoadVersion = 0;
    const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
    const money = value => Number(value || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    function showError(message) { toast.error(message); }
    const requestJson = window.workflowApi;
    const { renderInspections } = window.createAdminReturns({ getRequests: () => requests, getSelectedRequestId: () => selectedRequestId, money, escapeHtml, requestImage, requestJson, success, loadData, showError });
    function statusClass(status) {
        if (status === "AVAILABLE") return "available";
        if (["IN_USE", "BORROWED", "APPROVED"].includes(status)) return "in-use";
        if (status === "DISPOSED") return "disposed";
        if (status === "PENDING") return "pending";
        if (status === "RETURNED") return "returned";
        if (status === "OVERDUE") return "overdue";
        return "maintenance";
    }
    document.addEventListener("error", event => {
        if (event.target instanceof HTMLImageElement && event.target.closest(".management-shelves, .request-management-image, .inventory-tile, .management-inventory-table, .management-queue-card, .admin-detail-image")) {
            const placeholder = document.createElement("span"); placeholder.className = "management-no-image"; placeholder.textContent = "No image";
            event.target.replaceWith(placeholder);
        }
    }, true);
    function equipmentImage(id) {
        const url = equipments.get(String(id))?.imageUrl;
        return url && (url.startsWith("https://") || url.startsWith("/images/")) ? escapeHtml(url) : "";
    }
    function imageMarkup(id) {
        const url = equipmentImage(id);
        return url ? `<img src="${url}" alt="" loading="lazy">` : '<span class="management-no-image">No image</span>';
    }
    function borrowerAvatar(user) {
        const initial = escapeHtml((user.username || 'U').slice(0, 1).toUpperCase());
        const image = user.userId == null ? '' : `<img src="/admin/borrower-avatar/${encodeURIComponent(user.userId)}" alt="" loading="lazy" decoding="async" data-borrower-avatar>`;
        return `<span class="request-borrower-initial" aria-hidden="true"><span>${initial}</span>${image}</span>`;
    }
    document.addEventListener('error', event => {
        if (event.target instanceof HTMLImageElement && event.target.hasAttribute('data-borrower-avatar')) event.target.remove();
    }, true);
    function requestImage(item) {
        const url = item?.imageUrl;
        return url && (url.startsWith("https://") || url.startsWith("/images/"))
            ? `<img src="${escapeHtml(url)}" alt="" loading="lazy">`
            : '<span class="management-no-image">No image</span>';
    }
    function shelfDevice(item, category) {
        const kind = /laptop|notebook/i.test(category) ? "laptop" : /display|monitor/i.test(category) ? "monitor" : "accessory";
        const states = {
            AVAILABLE: { className: "ready", label: "Available", icon: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>' },
            IN_USE: { className: "busy", label: "In use", icon: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>' },
            MAINTENANCE: { className: "maintenance", label: "Maintenance", icon: '<path d="m14 6 4 4M4 20l7-7a6 6 0 0 0 8-8l-4 4-3-3 4-4a6 6 0 0 0-8 8l-7 7z"/>' },
            DISPOSED: { className: "disposed", label: "Disposed", icon: '<circle cx="12" cy="12" r="9"/><path d="M8 12h8"/>' }
        };
        const state = states[item.status] || { className: "disposed", label: item.status, icon: '<circle cx="12" cy="12" r="9"/><path d="M8 12h8"/>' };
        return `<span class="shelf-device ${kind} ${state.className}" aria-hidden="true"><span class="shelf-screen"><span class="shelf-symbol"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${state.icon}</svg></span><span>${escapeHtml(state.label)}</span></span><span class="shelf-base"></span></span>`;
    }
    function renderShelves(items) {
        const groups = categories.map(category => ({ ...category, items: items.filter(item => item.categoryId === category.id) }));
        const other = items.filter(item => !categories.some(category => category.id === item.categoryId));
        if (other.length) groups.push({ name: "Other equipment", items: other });
        document.getElementById("management-shelves").innerHTML = groups.map((group, index) => `<section class="management-shelf"><header><h2>${escapeHtml(group.name)}</h2><span>${group.items.length} items</span></header><div class="management-shelf-items" tabindex="0" aria-label="${escapeHtml(group.name)} equipment">${group.items.map(item => `<button type="button" class="management-shelf-item" data-shelf-equipment="${item.id}">${shelfDevice(item, group.name)}<strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.storageSlot ? "Slot: " + item.storageSlot : item.assetCode)}</small><span class="admin-status ${statusClass(item.status)}">${escapeHtml(item.status.replaceAll("_", " "))}</span></button>`).join("") || '<p class="management-shelf-empty">No equipment in this category.</p>'}</div><div class="management-shelf-board" aria-hidden="true">Tier ${index + 1} Shelf</div></section>`).join("") || '<p>No equipment categories yet.</p>';
    }
    let modalTrigger = null;
    function closeModals() {
        document.querySelectorAll(".admin-modal-backdrop").forEach(modal => { modal.hidden = true; });
        page.classList.remove("modal-open");
        if (modalTrigger?.isConnected) modalTrigger.focus({ preventScroll: true });
    }
    function openModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        const openingTrigger = document.activeElement;
        closeModals();
        if (!openingTrigger?.closest(".admin-modal-backdrop")) modalTrigger = openingTrigger;
        if (id === 'add-equipment-modal') {
            const form = document.getElementById('admin-add-equipment-form');
            form.reset(); delete form.dataset.savedEquipmentId;
            form.querySelector('.equipment-upload-preview').hidden = true;
        }
        modal.hidden = false;
        document.dispatchEvent(new Event("picker:sync"));
        page.classList.add("modal-open");
        modal.querySelector("input:not(.picker-native), select:not(.picker-native), textarea, button")?.focus();
    }
    function success(message) { closeModals(); toast.success(message); }
    let userLoadVersion = 0;
    let userLoadController;
    let currentUsersUrl = "/admin/users";
    async function loadUsers(url = "/admin/users") {
        const container = document.querySelector("[data-user-management-content]");
        if (!container) return;
        const version = ++userLoadVersion;
        userLoadController?.abort();
        userLoadController = new AbortController();
        container.setAttribute("aria-busy", "true");
        const results = container.querySelector('[data-user-results]');
        results?.setAttribute('aria-busy', 'true');
        try {
            const response = await fetch(url, { signal: userLoadController.signal });
            if (!response.ok) throw new Error("Unable to load users. Please try again.");
            const documentResult = new DOMParser().parseFromString(await response.text(), "text/html");
            const content = documentResult.querySelector("[data-user-management-content] .user-management");
            if (!content) throw new Error("Your session or permissions have changed. Please reload the page.");
            if (version !== userLoadVersion) return;
            if (results) {
                results.replaceChildren(...content.querySelector('[data-user-results]').childNodes);
                container.querySelector('.user-total-count').textContent = content.querySelector('.user-total-count').textContent;
            } else container.replaceChildren(content);
            currentUsersUrl = url;
            const historyPanel = container.querySelector('.user-history-panel');
            const updatedHistory = content.querySelector('.user-history-panel');
            if (historyPanel && updatedHistory && historyPanel !== updatedHistory) {
                updatedHistory.open = historyPanel.open;
                historyPanel.replaceWith(updatedHistory);
            }
            document.dispatchEvent(new Event('picker:sync'));
        } catch (error) {
            if (error.name === 'AbortError' || version !== userLoadVersion) return;
            if (!results) container.innerHTML = '<div class="equipment-load-failed"><p>Unable to load users.</p><button type="button" data-retry-users>Try again</button></div>';
            toast.error(error.message);
        } finally {
            if (version === userLoadVersion) {
                container.removeAttribute("aria-busy");
                results?.removeAttribute('aria-busy');
            }
        }
    }
    document.addEventListener("click", event => {
        const pagination = event.target.closest('[data-user-management-content] nav a');
        if (pagination) { event.preventDefault(); loadUsers(pagination.href); }
        if (event.target.closest("[data-retry-users]")) loadUsers();
    });
    document.addEventListener("submit", event => {
        if (!event.target.matches("[data-user-search]")) return;
        event.preventDefault();
        loadUsers("/admin/users?" + new URLSearchParams(new FormData(event.target)));
    });
    document.addEventListener('submit', async event => {
        const form = event.target;
        if (!form.matches('.user-role-form')) return;
        event.preventDefault();
        const button = form.querySelector('[type="submit"]');
        if (button.disabled) return;
        const label = button.textContent;
        const body = new URLSearchParams(new FormData(form));
        button.disabled = true;
        button.textContent = 'Saving...';
        form.setAttribute('aria-busy', 'true');
        try {
            const headers = {};
            const token = document.querySelector('meta[name="_csrf"]')?.content;
            const header = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (token && header) headers[header] = token;
            const response = await fetch(form.action, { method: 'POST', body, headers });
            if (!response.ok) throw new Error('Unable to save role. Please try again.');
            const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
            const error = doc.querySelector('[data-user-management-content] [data-toast="error"]');
            if (error) throw new Error(error.textContent.trim());
            if (!doc.querySelector('[data-user-management-content] .user-management')) {
                // Changing your own role can remove access to user management.
                location.assign(response.url);
                return;
            }
            toast.success(doc.querySelector('[data-user-management-content] [data-toast="success"]')?.textContent.trim() || 'Role updated.');
            await loadUsers(currentUsersUrl);
        } catch (error) {
            toast.error(error.message);
        } finally {
            button.disabled = false;
            button.textContent = label;
            form.removeAttribute('aria-busy');
        }
    });
    function showView(name) {
        const view = ["dashboard", "inventory", "requests", ...(document.querySelector("[data-admin-panel=users]") ? ["users"] : [])].includes(name) ? name : "dashboard";
        if (view === "users" && !document.querySelector("[data-user-management-content] .user-management")) loadUsers();
        page.dataset.adminView = view;
        page.classList.remove("admin-menu-open");
        document.querySelectorAll("[data-admin-panel]").forEach(panel => { panel.hidden = panel.dataset.adminPanel !== view; panel.classList.toggle("active", !panel.hidden); });
        document.querySelectorAll("[data-admin-nav]").forEach(link => link.classList.toggle("active", link.dataset.adminNav === view));
        window.history.replaceState(null, "", `#${view}`);
    }
    function filterInventory() {
        document.getElementById("admin-inventory-empty").hidden = equipments.size > 0;
        document.getElementById("inventory-result-count").textContent = `${equipments.size} of ${assetTotal} matching equipment`;
    }
    async function loadAssets() {
        const version = ++assetLoadVersion;
        const params = new URLSearchParams({ size: '12', page: assetPage, sort: 'name,asc', keyword: search.value.trim() });
        if (statusFilter.value !== 'ALL') params.set('status', statusFilter.value);
        const [result, summary] = await Promise.all([requestJson('/api/v1/equipment?' + params), requestJson('/api/v1/equipment/summary')]);
        if (version !== assetLoadVersion) return;
        assetTotal = result.totalElements;
        renderEquipment(result.content);
        document.querySelector('[data-count-available]').textContent = summary.available;
        document.querySelector('[data-count-in-use]').textContent = summary.inUse;
        document.querySelectorAll('.equipment-pagination').forEach(nav => nav.hidden = result.totalPages <= 1);
        document.querySelectorAll('[data-asset-prev]').forEach(b => b.disabled = result.first);
        document.querySelectorAll('[data-asset-next]').forEach(b => b.disabled = result.last);
        document.querySelectorAll('[data-asset-page]').forEach(b => b.textContent = `Equipment page ${result.number + 1} of ${Math.max(1, result.totalPages)}`);
    }
    function filterRequests() {
        const status = document.querySelector("[data-admin-request-filter].active")?.dataset.adminRequestFilter || "ALL";
        let visible = 0;
        requestList.querySelectorAll(".admin-request-card").forEach(card => {
            card.hidden = status !== "ALL" && card.dataset.requestStatus !== status;
            if (!card.hidden) visible++;
        });
        document.getElementById("admin-request-empty").hidden = visible > 0;
    }
    function renderEquipment(items) {
        equipments = new Map(items.map(item => [String(item.id), item]));
        inventory.replaceChildren();
        items.forEach(item => {
            const card = document.createElement("article"); card.className = "admin-equipment-card inventory-tile";
            Object.assign(card.dataset, { id: item.id, name: item.name, code: item.assetCode, status: item.status });
            const category = categories.find(category => category.id === item.categoryId)?.name || "Other";
            const statusLabel = { AVAILABLE: "Available", IN_USE: "In use", MAINTENANCE: "Maintenance", DISPOSED: "Disposed" }[item.status] || item.status;
            card.innerHTML = `<button class="inventory-tile-image" type="button" data-open-equipment-admin aria-label="View ${escapeHtml(item.name)}">${imageMarkup(item.id)}</button><span class="inventory-tile-status ${statusClass(item.status)}">${escapeHtml(statusLabel)}</span><div class="inventory-tile-content"><p>${escapeHtml(category)}</p><h2>${escapeHtml(item.name)}</h2><footer><span class="inventory-asset-location"><span>${escapeHtml(item.storageSlot ? "Slot: " + item.storageSlot : "Slot not assigned")}</span><small>${escapeHtml(item.assetCode)}</small></span><button type="button" data-open-equipment-admin aria-label="Details for ${escapeHtml(item.name)}">View details</button></footer></div>`;
            inventory.append(card);
        });
        renderShelves(items);
        document.querySelector("[data-count-available]").textContent = items.filter(item => item.status === "AVAILABLE").length;
        document.querySelector("[data-count-in-use]").textContent = items.filter(item => item.status === "IN_USE").length;
        filterInventory();
    }
    function renderRequests(items, summary) {
        requests = new Map(items.map(item => [String(item.id), item]));
        requestList.replaceChildren();
        items.forEach(item => {
            const card = document.createElement("article"); card.className = "admin-request-card";
            Object.assign(card.dataset, { requestId: item.id, requestStatus: item.status });
            let action = item.status === "RETURNED" ? `<button type="button" data-return-receipt="${item.id}">View charges</button>` : "";
            if (item.status === "PENDING") action = '<button type="button" data-workflow="reject">Reject</button><button type="button" data-admin-approve><img src="/images/request-confirm.svg" alt="">Approve</button>';

            if (["BORROWED", "OVERDUE"].includes(item.status)) action = '<button type="button" data-open-admin-modal="return-modal"><img src="/images/request-return.svg" alt="">Return</button>';
            if (["PENDING", "APPROVED", "BORROWED", "OVERDUE"].includes(item.status) && item.username === page.dataset.username)
                action = '<span class="request-awaiting-review">Another staff member must handle this request.</span>';
            if (item.status === "RETURNED" && item.username !== page.dataset.username) action += '<button type="button" data-workflow="settlement" hidden>Record payment</button>';
            const label = { PENDING: 'Pending', APPROVED: 'Approved', BORROWED: 'In use', OVERDUE: 'Overdue', RETURNED: 'Returned', CANCELLED: 'Cancelled' }[item.status] || item.status;
            const note = item.rejectionReason ? "Rejected: " + item.rejectionReason : item.note?.trim();
            card.innerHTML = `<header><div class="request-borrower">${borrowerAvatar(item)}<div><h2>${escapeHtml(item.username)}</h2><p>Request #${item.id} &middot; Borrowed: ${escapeHtml(item.borrowDate)} &middot; Due: ${escapeHtml(item.dueDate)}</p></div></div><span class="admin-status ${statusClass(item.status)}">${escapeHtml(label)}</span></header><div class="request-management-equipment">${item.items.map(asset => `<div class="request-management-item"><div class="request-management-image">${requestImage(asset)}</div><div><h3>${escapeHtml(asset.equipmentName)}</h3><small>Quantity: ${asset.quantity}${asset.returnedOn ? " ? Returned " + escapeHtml(asset.returnedOn) : ""}</small>${asset.assetCode ? `<p class="request-asset-code">${escapeHtml(asset.assetCode)}</p>` : ''}</div></div>`).join('')}</div>${note ? `<div class="request-management-note"><strong>Note:</strong> ${escapeHtml(note)}</div>` : ''}${action ? `<footer>${item.status === 'RETURNED' ? '<span class="request-return-complete"><img src="/images/request-complete.svg" alt="">Return completed</span>' : ''}${action}</footer>` : ''}`;
            requestList.append(card);
            const paymentButton = card.querySelector('[data-workflow="settlement"]');
            if (paymentButton) {
                requestJson(`/api/v1/borrow-requests/${item.id}/settlement`).then(info => {
                    if (card.isConnected) paymentButton.hidden = !info.finalized || info.paid || Number(info.total) <= 0;
                }).catch(() => { /* Keep the action hidden until the amount is verified. */ });
            }
        });
        document.querySelector("[data-count-pending]").textContent = summary.pending;
        document.querySelector("[data-count-overdue]").textContent = summary.overdue;
        const work = summary.queue;
        const personIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M5 21v-3a7 7 0 0 1 14 0v3"/></svg>';
        const calendarIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><rect x="4" y="5" width="16" height="16" rx="2"/><path d="M8 3v4m8-4v4M4 11h16"/></svg>';
        document.getElementById("admin-recent-requests").innerHTML = work.slice(0, 6).map(item => {
            const first = item.items[0];
            const equipment = equipments.get(String(first?.equipmentId));
            const category = first?.categoryName || "Equipment";
            const extra = item.items.length > 1 ? ` +${item.items.length - 1} more` : "";

            const label = { PENDING: "Pending", APPROVED: "Approved", OVERDUE: "Overdue" }[item.status];
            return `<article class="management-queue-card"><header class="management-queue-heading"><div class="management-queue-image">${requestImage(first)}</div><div class="management-queue-title"><h3>${escapeHtml(first?.equipmentName || "Request #" + item.id)}</h3><p>${escapeHtml(category)}${escapeHtml(extra)}</p></div><span class="management-queue-status ${statusClass(item.status)}">${label}</span></header><dl><div><dt>${personIcon}Borrower:</dt><dd>${escapeHtml(item.username)}</dd></div><div><dt>${calendarIcon}Due Date:</dt><dd>${escapeHtml(item.dueDate)}</dd></div></dl><button type="button" data-queue-request="${item.id}" aria-label="More detail for request ${item.id}">More Detail</button></article>`;
        }).join("") || '<p class="admin-empty-inline">All caught up. No requests need attention.</p>';
        filterRequests();
    }
    function updateRequestPaging(result) {
        document.querySelector(".management-pagination").hidden = false;
        document.querySelector('[data-request-prev]').disabled = result.first;
        document.querySelector('[data-request-next]').disabled = result.last;
        document.querySelector('[data-request-page]').textContent = `Page ${result.number + 1} of ${Math.max(1, result.totalPages)}`;
    }
    async function loadRequestPage() {
        const version = ++requestLoadVersion;
        const status = document.querySelector('[data-admin-request-filter].active')?.dataset.adminRequestFilter || 'ALL';
        const panel = document.querySelector('[data-admin-panel="requests"]');
        const pagingButtons = Array.from(document.querySelectorAll('[data-request-prev], [data-request-next]'));
        const pagingState = pagingButtons.map(button => button.disabled);
        let loaded = false;
        const empty = document.getElementById('admin-request-empty');
        panel.setAttribute('aria-busy', 'true');
        requestList.setAttribute('aria-busy', 'true');
        empty.textContent = 'No requests match.';
        document.querySelectorAll('[data-request-prev], [data-request-next]').forEach(button => button.disabled = true);
        try {
            const result = await requestJson(`/api/v1/borrow-requests/management?page=${requestPage}&status=${status}`);
            if (version !== requestLoadVersion) return;
            renderRequests(result.page.content, result);
            updateRequestPaging(result.page);
            loaded = true;
        } catch (error) {
            if (version !== requestLoadVersion) return;
            showError(error.message);
        } finally {
            if (version === requestLoadVersion) {
                panel.removeAttribute('aria-busy');
                requestList.setAttribute('aria-busy', 'false');
                if (!loaded) pagingButtons.forEach((button, index) => button.disabled = pagingState[index]);
            }
        }
    }
    document.querySelector('[data-request-prev]').addEventListener('click', () => { requestPage = Math.max(0, requestPage - 1); loadRequestPage(); });
    document.querySelector('[data-request-next]').addEventListener('click', () => { requestPage++; loadRequestPage(); });
    async function loadData() {
        if (!categories.length) {
            categories = await requestJson('/api/v1/categories');
            for (const id of ['admin-category', 'admin-edit-category']) document.getElementById(id).replaceChildren(new Option('Select a category', ''), ...categories.map(item => new Option(item.name, item.id)));
        }
        await Promise.all([loadAssets(), loadRequestPage()]);
        document.getElementById("admin-load-error").hidden = true;
    }
    document.querySelectorAll('[data-asset-prev],[data-asset-next]').forEach(button => button.addEventListener('click', () => { assetPage = Math.max(0, assetPage + (button.hasAttribute('data-asset-next') ? 1 : -1)); loadAssets().catch(e => showError(e.message)); }));
    document.addEventListener("click", async event => {
        const target = event.target;
        const operation = target.closest('[data-workflow]');
        if (operation) { try { if (await window.workflowAction(operation.dataset.workflow, operation.closest('[data-request-id]')?.dataset.requestId || selectedEquipmentId)) { success('Saved.'); await loadData(); } } catch (e) { showError(e.message); } return; }
        const nav = target.closest("[data-admin-nav]");
        if (nav) { event.preventDefault(); showView(nav.dataset.adminNav); return; }
        if (target.closest("[data-admin-menu]")) { page.classList.toggle("admin-menu-open"); return; }
        if (target.closest("[data-close-admin-modal]")) { closeModals(); return; }
        const shelf = target.closest("[data-shelf-equipment]");
        if (shelf) { inventory.querySelector(`[data-id="${shelf.dataset.shelfEquipment}"] [data-open-equipment-admin]`)?.click(); return; }
        const queued = target.closest("[data-queue-request]");
        if (queued) {
            showView("requests");
            document.querySelectorAll('[data-admin-request-filter]').forEach(button => button.classList.toggle('active', button.dataset.adminRequestFilter === 'ALL'));
            requestPage = 0;
            await loadRequestPage();
            if (!requests.has(queued.dataset.queueRequest)) {
                try {
                    const loan = await requestJson(`/api/v1/borrow-requests/${queued.dataset.queueRequest}`);
                    const summary = await requestJson('/api/v1/borrow-requests/management?page=0&status=ALL');
                    renderRequests([loan], summary);
                    document.querySelector(".management-pagination").hidden = true;
                } catch (error) { showError(error.message); return; }
            }
            const card = requestList.querySelector(`[data-request-id="${queued.dataset.queueRequest}"]`);
            card?.scrollIntoView({ block: "center", behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth" });
            if (card) { card.tabIndex = -1; card.focus({ preventScroll: true }); }
            return;
        }
        const details = target.closest("[data-open-equipment-admin]");
        if (details) {
            selectedEquipmentId = details.closest(".admin-equipment-card").dataset.id;
            const item = equipments.get(selectedEquipmentId);
            const deleteButton = document.querySelector("[data-admin-delete]");
            if (deleteButton) deleteButton.hidden = true;
            const inspectedId = selectedEquipmentId;
            if (deleteButton) requestJson(`/api/v1/equipment/${inspectedId}/deletion`).then(result => {
                if (selectedEquipmentId === inspectedId) deleteButton.hidden = !result.allowed;
            }).catch(() => { deleteButton.hidden = true; });
            document.getElementById("admin-equipment-title").textContent = item.name;
            document.getElementById("admin-equipment-code").textContent = item.assetCode;
            document.getElementById("admin-equipment-image").innerHTML = imageMarkup(item.id);
            document.getElementById("admin-equipment-category").textContent = categories.find(category => category.id === item.categoryId)?.name || "Other";
            window.renderEquipmentSpecs(document.getElementById("admin-equipment-specs"), item.specifications);
            const detailStatus = document.getElementById("admin-equipment-status");
            detailStatus.className = `admin-status ${statusClass(item.status)}`;
            document.querySelector('[data-workflow="repair"]').hidden = item.status !== "MAINTENANCE";
            detailStatus.textContent = { AVAILABLE: "Available", IN_USE: "In use", MAINTENANCE: "Maintenance", DISPOSED: "Disposed" }[item.status] || item.status;
            document.getElementById("admin-borrower-strip").hidden = item.status !== "IN_USE";
            const form = document.getElementById("admin-edit-equipment-form");
            form.elements.categoryId.value = item.categoryId;
            form.elements.storageSlot.value = item.storageSlot || "";
            document.getElementById("admin-equipment-slot").textContent = item.storageSlot || "Not assigned";
            form.elements.image.value = "";
            const preview = form.querySelector(".equipment-upload-preview"); preview.hidden = !item.imageUrl; if (item.imageUrl) preview.src = item.imageUrl;
            form.elements.imageUrl.value = item.imageUrl || ""; form.elements.specifications.value = item.specifications || "";
            form.elements.name.value = item.name; form.elements.status.value = item.status; form.elements.purchasePrice.value = item.purchasePrice ?? "";
            document.getElementById("admin-equipment-price").textContent = `${item.purchasePrice == null ? "Not set" : money(item.purchasePrice) + " THB"}`;
            openModal("equipment-admin-modal"); return;
        }
        const opener = target.closest("[data-open-admin-modal]");
        if (opener) {
            if (opener.dataset.openAdminModal === "return-modal") {
                selectedRequestId = opener.closest(".admin-request-card").dataset.requestId;
                const loan = requests.get(selectedRequestId);
                document.getElementById("return-request-summary").innerHTML = `${borrowerAvatar(loan)}<div><strong>${escapeHtml(loan.username)}</strong><p>Borrowed: ${escapeHtml(loan.borrowDate)} | Due: ${escapeHtml(loan.dueDate)} &middot; Request #${loan.id}</p></div>`;
                renderInspections(loan); document.getElementById("return-remark").value = "";
            }
            openModal(opener.dataset.openAdminModal); return;
        }
        const action = target.closest("[data-admin-approve], [data-admin-delete]");
        if (!action) return;
        action.disabled = true;
        try {
            let message = "Updated successfully.";
            if (action.hasAttribute("data-admin-delete")) {
                if (!confirm("Delete this equipment? Equipment with loan history cannot be deleted.")) return;
                await requestJson(`/api/v1/equipment/${selectedEquipmentId}`, { method: "DELETE" });
            } else {
                const id = action.closest(".admin-request-card").dataset.requestId;
                const operation = "approve";
                await requestJson(`/api/v1/borrow-requests/${id}/${operation}`, { method: "PATCH" });
                message = "Request approved.";
            }
            await loadData(); success(message);
        } catch (error) { showError(error.message); } finally { action.disabled = false; }
    });
    document.addEventListener("keydown", event => {
        const modal = document.querySelector('.admin-modal-backdrop:not([hidden])');
        if (!modal || event.defaultPrevented || document.querySelector('.picker-panel:popover-open')) return;
        if (event.key === 'Escape') { event.preventDefault(); closeModals(); }
        if (event.key === 'Tab') {
            const fields = [...modal.querySelectorAll('button, input:not(.picker-native), select:not(.picker-native), textarea, a[href]')].filter(el => !el.disabled && el.getClientRects().length);
            const first = fields[0], last = fields.at(-1);
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
        }
    });
    let searchTimer; search?.addEventListener("input", () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { assetPage = 0; loadAssets().catch(e => showError(e.message)); }, 250); }); statusFilter?.addEventListener("change", () => { assetPage = 0; loadAssets().catch(e => showError(e.message)); });
    document.querySelectorAll("[data-admin-request-filter]").forEach(button => button.addEventListener("click", () => {
        document.querySelectorAll("[data-admin-request-filter]").forEach(item => item.classList.toggle("active", item === button)); requestPage = 0; loadRequestPage();
    }));
    document.querySelectorAll('.equipment-upload input[type=file]').forEach(input => {
        let previewUrl;
        const preview = input.closest('label').querySelector('img');
        input.addEventListener('change', () => {
            if (previewUrl) URL.revokeObjectURL(previewUrl);
            const file = input.files[0];
            if (file && (file.size > 2 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(file.type))) {
                input.value = ''; preview.hidden = true; toast.error('Choose a JPG or PNG up to 2 MB.'); return;
            }
            preview.hidden = !file;
            if (file) { previewUrl = URL.createObjectURL(file); preview.src = previewUrl; }
        });
    });
    for (const mode of ["add", "edit"]) document.getElementById(`admin-${mode}-equipment-form`).addEventListener("submit", async event => {
        event.preventDefault(); const form = event.currentTarget; const data = new FormData(form); const button = form.querySelector('[type="submit"]'); button.disabled = true;
        const payload = mode === "add" ? { name: data.get("name"), assetCode: data.get("assetCode"), categoryId: Number(data.get("categoryId")), status: "AVAILABLE" } : { name: data.get("name"), categoryId: Number(data.get("categoryId")), status: data.get("status") };
        // Uploaded photos belong to the image endpoint, not the equipment JSON payload.
        payload.specifications = data.get("specifications").trim();
        payload.storageSlot = data.get("storageSlot").trim();
        payload.purchasePrice = data.get("purchasePrice");
        try {
            const savedId = mode === 'edit' ? selectedEquipmentId : form.dataset.savedEquipmentId;
            const saved = await requestJson(`/api/v1/equipment${savedId ? '/' + savedId : ''}`, { method: savedId ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            if (mode === 'add') form.dataset.savedEquipmentId = saved.id;
            const file = data.get('image');
            if (file?.size) {
                const body = new FormData(); body.append('image', file);
                const header = document.querySelector('meta[name="_csrf_header"]').content;
                const token = document.querySelector('meta[name="_csrf"]').content;
                const response = await fetch(`/api/v1/equipment/${saved.id}/image`, { method: 'POST', body, headers: { [header]: token } });
                if (!response.ok) {
                    const error = await response.json().catch(() => ({}));
                    throw new Error(`Equipment saved, but photo upload failed. ${error.message || 'Please try again.'}`);
                }
                const result = await response.json(); form.elements.imageUrl.value = result.imageUrl;
            }
            delete form.dataset.savedEquipmentId;
            form.elements.image.value = ''; form.querySelector('.equipment-upload-preview').hidden = true;
            if (mode === 'add') form.reset(); await loadData(); success('Equipment saved.');
        }
        catch (error) { showError(error.message); } finally { button.disabled = false; }
    });
    document.querySelector("[data-admin-logout]")?.addEventListener("click", () => {
        const form = document.createElement("form"); form.method = "POST"; form.action = "/logout";
        const token = document.createElement("input"); token.type = "hidden"; token.name = "_csrf"; token.value = document.querySelector('meta[name="_csrf"]').content;
        form.append(token); document.body.append(form); form.submit();
    });
    showView(window.location.hash.slice(1) || (window.location.pathname === "/admin/users" ? "users" : "dashboard"));
    if (window.location.pathname.includes("/src/main/resources/templates/")) return;
    const initialLoadingMarkup = new Map(["management-shelves", "admin-inventory-grid"].map(id => [id, document.getElementById(id).innerHTML]));
    async function loadInitialEquipment() {
        initialLoadingMarkup.forEach((markup, id) => { document.getElementById(id).innerHTML = markup; });
        document.getElementById("admin-load-error").hidden = true;
        try { await loadData(); }
        catch (error) {
            const message = document.getElementById("admin-load-error");
            message.textContent = error.message; message.hidden = false;
            initialLoadingMarkup.forEach((markup, id) => {
                document.getElementById(id).innerHTML = '<div class="equipment-load-failed"><p>Unable to load equipment.</p><button type="button" data-retry-equipment>Try again</button></div>';
            });
        }
    }
    document.addEventListener("click", event => {
        if (event.target.closest("[data-retry-equipment]")) loadInitialEquipment();
    });
    loadInitialEquipment();
});
