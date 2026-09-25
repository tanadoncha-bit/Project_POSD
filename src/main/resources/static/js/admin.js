document.addEventListener("DOMContentLoaded", () => {
    const isStaticPreview = window.location.pathname.includes("/src/main/resources/templates/");
    const page = document.body;
    const panels = Array.from(document.querySelectorAll("[data-admin-panel]"));
    const navigationLinks = Array.from(document.querySelectorAll("[data-admin-nav]"));
    const inventoryGrid = document.getElementById("admin-inventory-grid");
    const requestList = document.getElementById("admin-request-list");
    const lockerBoard = document.getElementById("admin-locker-board");
    const recentRequestList = document.getElementById("admin-recent-requests");
    const inventoryEmpty = document.getElementById("admin-inventory-empty");
    const requestEmpty = document.getElementById("admin-request-empty");
    const inventorySearch = document.querySelector("[data-admin-inventory-search]");
    const statusFilter = document.querySelector("[data-admin-status-filter]");
    const equipmentModal = document.getElementById("equipment-admin-modal");
    const borrowerStrip = document.getElementById("admin-borrower-strip");
    let selectedEquipmentId = null;
    let selectedRequestId = null;

    function showView(viewName) {
        const nextView = ["dashboard", "inventory", "requests"].includes(viewName)
            ? viewName
            : "dashboard";

        page.dataset.adminView = nextView;
        panels.forEach((panel) => {
            const active = panel.dataset.adminPanel === nextView;
            panel.hidden = !active;
            panel.classList.toggle("active", active);
        });
        navigationLinks.forEach((link) => {
            link.classList.toggle("active", link.dataset.adminNav === nextView);
        });
        page.classList.remove("admin-menu-open");
        if (window.location.hash !== `#${nextView}`) {
            window.history.replaceState(null, "", `#${nextView}`);
        }
    }

    function modalIsOpen() {
        return Array.from(document.querySelectorAll(".admin-modal-backdrop"))
            .some((modal) => !modal.hidden);
    }

    function closeAllModals() {
        document.querySelectorAll(".admin-modal-backdrop").forEach((modal) => {
            modal.hidden = true;
        });
        document.body.classList.remove("modal-open");
    }

    function openModal(id) {
        closeAllModals();
        const modal = document.getElementById(id);
        if (!modal) return;
        modal.hidden = false;
        document.body.classList.add("modal-open");
        modal.querySelector("button, input, select, textarea")?.focus();
    }

    function showSuccess(message) {
        const messageElement = document.getElementById("admin-success-message");
        if (messageElement) messageElement.textContent = message;
        openModal("admin-success-modal");
    }

    function statusLabel(status) {
        const labels = {
            AVAILABLE: "Available",
            IN_USE: "In Use",
            MAINTENANCE: "Maintenance",
            DISPOSED: "Disposed",
            PENDING: "Pending",
            APPROVED: "Approved",
            BORROWED: "In Use",
            OVERDUE: "Overdue",
            RETURNED: "Returned",
            CANCELLED: "Cancelled"
        };
        return labels[status] || status || "Unknown";
    }

    function statusClass(status) {
        if (status === "AVAILABLE") return "available";
        if (["IN_USE", "BORROWED", "APPROVED"].includes(status)) return "in-use";
        if (status === "PENDING") return "pending";
        if (status === "RETURNED") return "returned";
        if (status === "OVERDUE") return "overdue";
        return "maintenance";
    }

    function localEquipmentImage(id) {
        const images = [
            "/images/equipment-macbook.png",
            "/images/equipment-victus.png",
            "/images/equipment-headset.png",
            "/images/equipment-keyboard.png",
            "/images/equipment-mouse.png",
            "/images/equipment-razer.png"
        ];
        return images[Math.abs(Number(id || 1) - 1) % images.length];
    }

    function renderEquipment(equipments) {
        if (!inventoryGrid) return;
        inventoryGrid.replaceChildren();

        equipments.forEach((equipment) => {
            const card = document.createElement("article");
            card.className = "admin-equipment-card";
            card.dataset.id = equipment.id;
            card.dataset.name = equipment.name || "Equipment";
            card.dataset.status = equipment.status || "AVAILABLE";
            card.dataset.code = equipment.assetCode || "-";
            card.innerHTML = `
                <div class="admin-equipment-image">
                    <img src="${localEquipmentImage(equipment.id)}" alt="">
                    <span class="admin-status ${statusClass(equipment.status)}">${statusLabel(equipment.status)}</span>
                </div>
                <div>
                    <h2>${equipment.name || "Equipment"}</h2>
                    <p>Asset: ${equipment.assetCode || "-"}</p>
                    <button type="button" data-open-equipment-admin>View details</button>
                </div>`;
            inventoryGrid.appendChild(card);
        });

        updateEquipmentCounts(equipments);
        renderLockers(equipments);
        filterInventory();
    }

    function renderLockers(equipments) {
        if (!lockerBoard) return;
        lockerBoard.replaceChildren();

        if (equipments.length === 0) {
            lockerBoard.innerHTML = '<p class="admin-empty-inline">No equipment has been added yet.</p>';
            return;
        }

        equipments.slice(0, 8).forEach((equipment, index) => {
            const tile = document.createElement("button");
            const isInUse = equipment.status === "IN_USE";
            tile.type = "button";
            tile.className = `locker-tile${isInUse ? " in-use" : ""}`;
            tile.innerHTML = `<span>${String(index + 1).padStart(2, "0")}</span><strong>${statusLabel(equipment.status)}</strong>`;
            lockerBoard.appendChild(tile);
        });
    }

    function updateEquipmentCounts(equipments) {
        const available = equipments.filter((item) => item.status === "AVAILABLE").length;
        const inUse = equipments.filter((item) => item.status === "IN_USE").length;
        const set = (selector, value) => {
            const element = document.querySelector(selector);
            if (element) element.textContent = String(value);
        };
        set("[data-count-available]", available);
        set("[data-count-in-use]", inUse);
    }

    function renderRequests(requests) {
        if (!requestList) return;
        requestList.replaceChildren();

        requests.forEach((request) => {
            const card = document.createElement("article");
            const status = request.status || "PENDING";
            card.className = "admin-request-card";
            card.dataset.requestStatus = status;
            card.dataset.requestId = request.id;
            const itemNames = (request.items || [])
                .map((item) => `<span>${item.equipmentName || "Equipment"} × ${item.quantity || 1}</span>`)
                .join("");
            const initials = String(request.username || "User")
                .split(/\s+/)
                .slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase())
                .join("");
            let action = '<button class="secondary" type="button" data-open-equipment-admin>Details</button>';
            if (status === "PENDING") action = '<button type="button" data-admin-approve>Approve</button>';
            if (["BORROWED", "OVERDUE"].includes(status)) action = '<button type="button" data-open-admin-modal="return-modal">Return</button>';

            card.innerHTML = `
                <header><div><span class="admin-user-avatar">${initials || "U"}</span><div><h2>${request.username || "User"}</h2><p>Request #${request.id} · Due ${request.dueDate || "-"}</p></div></div><span class="admin-status ${statusClass(status)}">${statusLabel(status)}</span></header>
                <div class="admin-request-items">${itemNames || "<span>No equipment items</span>"}</div>
                <footer><p>${request.note || "No additional note."}</p>${action}</footer>`;
            requestList.appendChild(card);
        });

        const pending = requests.filter((request) => request.status === "PENDING").length;
        const overdue = requests.filter((request) => request.status === "OVERDUE").length;
        const pendingCount = document.querySelector("[data-count-pending]");
        const overdueCount = document.querySelector("[data-count-overdue]");
        if (pendingCount) pendingCount.textContent = String(pending);
        if (overdueCount) overdueCount.textContent = String(overdue);
        renderRecentRequests(requests);
        filterRequests();
    }

    function renderRecentRequests(requests) {
        if (!recentRequestList) return;
        recentRequestList.replaceChildren();

        if (requests.length === 0) {
            recentRequestList.innerHTML = '<p class="admin-empty-inline">No borrow requests yet.</p>';
            return;
        }

        requests.slice(0, 4).forEach((request) => {
            const article = document.createElement("article");
            const initials = String(request.username || "User")
                .split(/\s+/)
                .slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase())
                .join("");
            const equipmentName = request.items?.[0]?.equipmentName || "Equipment request";
            article.innerHTML = `<span class="admin-user-avatar">${initials || "U"}</span><div><strong>${request.username || "User"}</strong><small>${equipmentName}</small></div><span class="admin-status ${statusClass(request.status)}">${statusLabel(request.status)}</span>`;
            recentRequestList.appendChild(article);
        });
    }

    function filterInventory() {
        const keyword = String(inventorySearch?.value || "").trim().toLowerCase();
        const status = statusFilter?.value || "ALL";
        let visible = 0;
        document.querySelectorAll(".admin-equipment-card").forEach((card) => {
            const matchesKeyword = !keyword || `${card.dataset.name} ${card.dataset.code}`.toLowerCase().includes(keyword);
            const matchesStatus = status === "ALL" || card.dataset.status === status;
            card.hidden = !(matchesKeyword && matchesStatus);
            if (!card.hidden) visible += 1;
        });
        if (inventoryEmpty) inventoryEmpty.hidden = visible !== 0;
    }

    function filterRequests() {
        const activeFilter = document.querySelector("[data-admin-request-filter].active")?.dataset.adminRequestFilter || "ALL";
        let visible = 0;
        document.querySelectorAll(".admin-request-card").forEach((card) => {
            const matches = activeFilter === "ALL" || card.dataset.requestStatus === activeFilter;
            card.hidden = !matches;
            if (matches) visible += 1;
        });
        if (requestEmpty) requestEmpty.hidden = visible !== 0;
    }

    function openEquipmentDetails(card) {
        const equipmentCard = card?.closest(".admin-equipment-card");
        selectedEquipmentId = equipmentCard?.dataset.id || null;
        const title = document.getElementById("admin-equipment-title");
        if (title && equipmentCard) title.textContent = equipmentCard.dataset.name;
        if (borrowerStrip) borrowerStrip.hidden = equipmentCard?.dataset.status !== "IN_USE";
        openModal("equipment-admin-modal");
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: { "Content-Type": "application/json", ...(options.headers || {}) }
        });
        if (!response.ok) throw new Error(`Request failed (${response.status})`);
        if (response.status === 204) return null;
        return response.json();
    }

    async function loadServerData() {
        if (isStaticPreview) {
            const previewEquipment = Array.from(document.querySelectorAll(".admin-equipment-card"))
                .map((card) => ({ status: card.dataset.status }));
            updateEquipmentCounts(previewEquipment);
            return;
        }
        try {
            const [equipmentPage, requestPage] = await Promise.all([
                requestJson("/api/v1/equipment?size=100&sort=name,asc"),
                requestJson("/api/v1/borrow-requests?size=100&sort=id,desc")
            ]);
            renderEquipment(equipmentPage.content || []);
            renderRequests(requestPage.content || []);
        } catch (error) {
            console.error("Unable to load management data.", error);
            if (inventoryEmpty) inventoryEmpty.hidden = false;
            if (requestEmpty) requestEmpty.hidden = false;
        }
    }

    navigationLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            showView(link.dataset.adminNav);
        });
    });

    document.querySelector("[data-admin-menu]")?.addEventListener("click", () => {
        page.classList.toggle("admin-menu-open");
    });

    document.addEventListener("click", async (event) => {
        const opener = event.target.closest("[data-open-admin-modal]");
        const equipmentOpener = event.target.closest("[data-open-equipment-admin]");
        const closer = event.target.closest("[data-close-admin-modal]");
        const approveButton = event.target.closest("[data-admin-approve]");
        const returnButton = event.target.closest("[data-confirm-return]");

        if (equipmentOpener) {
            openEquipmentDetails(equipmentOpener);
            return;
        }
        if (opener) {
            selectedRequestId = opener.closest(".admin-request-card")?.dataset.requestId || selectedRequestId;
            openModal(opener.dataset.openAdminModal);
            return;
        }
        if (closer) {
            closeAllModals();
            return;
        }
        if (approveButton) {
            selectedRequestId = approveButton.closest(".admin-request-card")?.dataset.requestId;
            try {
                if (!isStaticPreview && selectedRequestId) {
                    await requestJson(`/api/v1/borrow-requests/${selectedRequestId}/approve`, { method: "PATCH" });
                    await loadServerData();
                }
                showSuccess("Borrow request approved successfully.");
            } catch (error) {
                window.alert(error.message);
            }
            return;
        }
        if (returnButton) {
            try {
                if (!isStaticPreview && selectedRequestId) {
                    await requestJson(`/api/v1/borrow-requests/${selectedRequestId}/return`, {
                        method: "POST",
                        body: JSON.stringify({ returnDate: new Date().toISOString().slice(0, 10), condition: "NORMAL", remark: "" })
                    });
                    await loadServerData();
                }
                showSuccess("Equipment return recorded successfully.");
            } catch (error) {
                window.alert(error.message);
            }
        }
    });

    document.querySelectorAll(".admin-modal-backdrop").forEach((modal) => {
        modal.addEventListener("click", (event) => {
            if (event.target === modal) closeAllModals();
        });
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && modalIsOpen()) closeAllModals();
    });

    inventorySearch?.addEventListener("input", filterInventory);
    statusFilter?.addEventListener("change", filterInventory);

    document.querySelectorAll("[data-admin-request-filter]").forEach((button) => {
        button.addEventListener("click", () => {
            document.querySelectorAll("[data-admin-request-filter]").forEach((item) => item.classList.remove("active"));
            button.classList.add("active");
            filterRequests();
        });
    });

    document.getElementById("admin-add-equipment-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const payload = { assetCode: data.get("assetCode"), name: data.get("name"), status: "AVAILABLE" };
        try {
            if (!isStaticPreview) {
                await requestJson("/api/v1/equipment", { method: "POST", body: JSON.stringify(payload) });
                await loadServerData();
            }
            event.currentTarget.reset();
            showSuccess("New equipment added successfully.");
        } catch (error) {
            window.alert(error.message);
        }
    });

    document.getElementById("admin-edit-equipment-form")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const payload = { name: data.get("name"), status: data.get("status") || "AVAILABLE" };
        try {
            if (!isStaticPreview && selectedEquipmentId) {
                await requestJson(`/api/v1/equipment/${selectedEquipmentId}`, { method: "PUT", body: JSON.stringify(payload) });
                await loadServerData();
            }
            showSuccess("Equipment updated successfully.");
        } catch (error) {
            window.alert(error.message);
        }
    });

    document.querySelector("[data-admin-logout]")?.addEventListener("click", () => {
        window.location.href = isStaticPreview ? "../dashboard/index.html" : "/logout";
    });

    showView(window.location.hash.replace("#", "") || "dashboard");
    loadServerData();
});
