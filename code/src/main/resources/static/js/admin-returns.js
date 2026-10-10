window.createAdminReturns = function ({ getRequests, getSelectedRequestId, money, escapeHtml, requestImage, requestJson, success, loadData, showError }) {
    const inspectionContainer = document.getElementById("return-inspections");
    function updateDamageEstimate() {
        let total = 0; let missingPrice = false;
        inspectionContainer.querySelectorAll("[data-inspection]").forEach(row => {
            const condition = row.querySelector("[data-return-condition]").value;
            row.classList.toggle("return-item-excluded", !row.querySelector("[data-return-selected]").checked);
            const policy = getRequests().get(String(getSelectedRequestId()))?.feePolicy;
            const rate = Number({ NORMAL: 0, MINOR_SCRATCHES: policy?.scratchRate, DAMAGED: policy?.damageRate, LOST: policy?.lossRate }[condition]);
            if (!row.querySelector("[data-return-selected]").checked) { row.querySelector("textarea").required = false; row.querySelector("output").textContent = "Not included in this return"; return; }
            const price = getRequests().get(String(getSelectedRequestId()))?.items.find(item => String(item.equipmentId) === row.dataset.inspection)?.purchasePrice;
            row.querySelector("textarea").required = rate > 0;
            const unavailable = rate > 0 && price == null;
            missingPrice ||= unavailable;
            const amount = Math.round((Number(price || 0) * rate + Number.EPSILON) * 100) / 100;
            total += amount;
            row.querySelector("output").textContent = unavailable ? "Recorded purchase price is missing." : `Damage charge: ${money(amount)} THB`;
        });
        const loan = getRequests().get(String(getSelectedRequestId()));
        const policy = loan?.feePolicy;
        const dateParts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Bangkok', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
        const part = type => dateParts.find(p => p.type === type).value;
        const today = `${part('year')}-${part('month')}-${part('day')}`;
        const days = Math.max(0, Math.round((Date.parse(today) - Date.parse(loan.dueDate)) / 86400000) - Number(policy?.graceDays || 0));
        const fine = policy?.dailyFine == null ? null : days * Number(policy.dailyFine);
        document.getElementById('return-late-label').textContent = fine == null ? 'Estimated fine (calculated on confirmation)' : `Estimated fine (${days} days \u00d7 ${money(policy.dailyFine)} THB):`;
        document.getElementById('return-late-estimate').textContent = fine == null ? 'Pending' : `${money(fine)} THB`;
        document.getElementById('return-damage-estimate').textContent = missingPrice ? 'Purchase price missing' : `${money(total)} THB`;
        document.getElementById('return-total-estimate').textContent = missingPrice || fine == null ? 'Pending' : `${money(total + fine)} THB`;
        inspectionContainer.querySelectorAll('[data-overdue-days]').forEach(input => input.value = `${days} overdue day(s)`);
        document.querySelector("[data-confirm-return]").disabled = missingPrice || !inspectionContainer.querySelector("[data-return-selected]:checked");
    }
    inspectionContainer.addEventListener("change", updateDamageEstimate);
    function renderInspections(loan) {
        inspectionContainer.innerHTML = loan.items.filter(item => !item.returnedOn).map(item => {
            return `<fieldset data-inspection="${item.equipmentId}"><legend>${escapeHtml(item.equipmentName)} (${escapeHtml(item.assetCode)})</legend>
                <div class="return-inspection-photo">${requestImage(item)}</div>
                <div class="return-inspection-fields">
                    <label class="return-item-toggle"><input type="checkbox" data-return-selected checked><span class="return-toggle-button"><span class="return-toggle-icon" aria-hidden="true"></span><span>Return this item</span></span></label>
                    <div><span class="return-field-title">Late Fee Calculation Rate</span><div class="return-field-pair"><label><span class="visually-hidden">Recorded daily rate</span><input readonly value="${loan.feePolicy?.dailyFine == null ? 'Recorded policy' : money(loan.feePolicy.dailyFine) + ' THB/day'}"></label><label><span class="visually-hidden">Overdue days</span><input data-overdue-days readonly></label></div></div>
                    <div><span class="return-field-title">Inspected Condition</span><div class="return-field-pair"><label><span class="visually-hidden">Condition</span><select data-return-condition><option value="NORMAL">Normal</option><option value="MINOR_SCRATCHES">Minor scratches</option><option value="DAMAGED">Damaged</option><option value="LOST">Lost</option></select></label></div></div>
                    <label class="return-inspection-note"><span class="return-field-title">Inspection note</span><textarea maxlength="500" rows="2" placeholder="Add details about the condition"></textarea></label>
                    <output></output>
                </div></fieldset>`;
        }).join("");
        const policy = loan.feePolicy;
        document.getElementById("return-policy-help").textContent = policy?.dailyFine != null
            ? `Recorded late fee: ${money(policy.dailyFine)} THB/day after ${policy.graceDays || 0} grace day(s). Calculated on confirmation.`
            : "Legacy fee policy: the server will calculate the late fee on confirmation.";
        inspectionContainer.querySelectorAll('[data-return-condition]').forEach(select => {
            for (const [key, rate] of Object.entries({ MINOR_SCRATCHES: policy?.scratchRate, DAMAGED: policy?.damageRate, LOST: policy?.lossRate })) {
                if (rate != null) select.querySelector(`option[value="${key}"]`).textContent = `${key.replaceAll('_', ' ')} - ${Math.round(Number(rate) * 100)}%`;
            }
        });
        updateDamageEstimate();
    }
    document.getElementById("admin-return-form").addEventListener("submit", async event => {
        event.preventDefault();
        const button = event.currentTarget.querySelector('[type="submit"]'); button.disabled = true;
        const items = Array.from(inspectionContainer.querySelectorAll("[data-inspection]")).filter(row => row.querySelector("[data-return-selected]").checked).map(row => ({ equipmentId: Number(row.dataset.inspection), condition: row.querySelector("[data-return-condition]").value, remark: row.querySelector("textarea").value }));
        try {
            const result = await requestJson(`/api/v1/borrow-requests/${getSelectedRequestId()}/return`, { method: "POST", body: JSON.stringify({ partial: true, items, remark: document.getElementById("return-remark").value }) });
            success(`Return recorded. Late fee: ${money(result.fineAmount)} THB. Damage: ${money(result.damageAmount)} THB. Total: ${money(result.totalAmount)} THB.`);
            await loadData();
        } catch (error) { showError(error.message); }
        finally { updateDamageEstimate(); }
    });

    return { renderInspections };
};
