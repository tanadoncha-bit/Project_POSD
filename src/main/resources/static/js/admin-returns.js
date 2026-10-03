window.createAdminReturns = function({getRequests,getSelectedRequestId,money,escapeHtml,requestJson,success,loadData,showError}) {
    const inspectionContainer = document.getElementById("return-inspections");
    function updateDamageEstimate() {
        let total = 0; let missingPrice = false;
        inspectionContainer.querySelectorAll("[data-inspection]").forEach(row => {
            const condition = row.querySelector("select").value;
            const policy=getRequests().get(String(getSelectedRequestId()))?.feePolicy;
            const rate = Number({ NORMAL: 0, MINOR_SCRATCHES: policy?.scratchRate ?? 0.2, DAMAGED: policy?.damageRate ?? 0.5, LOST: policy?.lossRate ?? 1 }[condition]);
            if (!row.querySelector("[data-return-selected]").checked) { row.querySelector("textarea").required = false; row.querySelector("output").textContent = "Not included in this return"; return; }
            const price = getRequests().get(String(getSelectedRequestId()))?.items.find(item => String(item.equipmentId) === row.dataset.inspection)?.purchasePrice;
            row.querySelector("textarea").required = rate > 0;
            const unavailable = rate > 0 && price == null;
            missingPrice ||= unavailable;
            const amount = Math.round((Number(price || 0) * rate + Number.EPSILON) * 100) / 100;
            total += amount;
            row.querySelector("output").textContent = unavailable ? "Recorded purchase price is missing." : `Damage charge: ${money(amount)} THB`;
        });
        document.getElementById("return-damage-estimate").textContent = missingPrice ? "A recorded purchase price is missing. Resolve the legacy valuation before charging damage." : `Estimated damage charges: ${money(total)} THB + late fee (if any)`;
        document.querySelector("[data-confirm-return]").disabled = missingPrice;
    }
    inspectionContainer.addEventListener("change", updateDamageEstimate);
    function renderInspections(loan) {
        inspectionContainer.innerHTML = loan.items.filter(item => !item.returnedOn).map(item => {
            const price = item.purchasePrice;
            return `<fieldset data-inspection="${item.equipmentId}"><legend>${escapeHtml(item.equipmentName)} (${escapeHtml(item.assetCode)})</legend>
                <label><input type="checkbox" data-return-selected checked> Return this item</label><p>Recorded purchase price: ${price == null ? "Not set" : money(price) + " THB"}</p>
                <label>Condition<select><option value="NORMAL">Normal - 0%</option><option value="MINOR_SCRATCHES">Scratch - 20%</option><option value="DAMAGED">Damaged - 50% (maintenance)</option><option value="LOST">Lost - 100% (disposed)</option></select></label>
                <label>Inspection note<textarea maxlength="500" placeholder="Describe any damage or loss"></textarea></label><output></output></fieldset>`;
        }).join("");
        const policy=loan.feePolicy;
        inspectionContainer.querySelectorAll('select').forEach(select=>{
            for(const [key,rate] of Object.entries({MINOR_SCRATCHES:policy?.scratchRate,DAMAGED:policy?.damageRate,LOST:policy?.lossRate})) {
                if(rate!=null) select.querySelector(`option[value="${key}"]`).textContent=`${key.replaceAll('_',' ')} - ${Math.round(Number(rate)*100)}%`;
            }
        });
        updateDamageEstimate();
    }
    document.getElementById("admin-return-form").addEventListener("submit", async event => {
        event.preventDefault();
        const button = event.currentTarget.querySelector('[type="submit"]'); button.disabled = true;
        const items = Array.from(inspectionContainer.querySelectorAll("[data-inspection]")).filter(row => row.querySelector("[data-return-selected]").checked).map(row => ({equipmentId: Number(row.dataset.inspection), condition: row.querySelector("select").value, remark: row.querySelector("textarea").value}));
        try {
            const result = await requestJson(`/api/v1/borrow-requests/${getSelectedRequestId()}/return`, {method:"POST", body:JSON.stringify({partial:true, items, remark:document.getElementById("return-remark").value})});
            success(`Return recorded. Late fee: ${money(result.fineAmount)} THB. Damage: ${money(result.damageAmount)} THB. Total: ${money(result.totalAmount)} THB.`);
            await loadData();
        } catch (error) { showError(error.message); }
        finally { button.disabled = false; }
    });

return {renderInspections};
};
