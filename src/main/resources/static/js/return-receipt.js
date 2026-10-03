// Share the persisted receipt with both operators and the borrower.
document.addEventListener("click", async event => {
    const trigger = event.target.closest("[data-return-receipt]");
    if (!trigger) return;
    const dialog = document.createElement("dialog");
    dialog.className = "return-receipt";
    const title = document.createElement("h2"); title.textContent = "Return charges";
    const body = document.createElement("div"); body.textContent = "Loading..."; body.setAttribute("aria-live", "polite");
    const close = document.createElement("button"); close.type = "button"; close.textContent = "Close";
    dialog.append(title, body, close); document.body.append(dialog);
    close.addEventListener("click", () => dialog.close());
    dialog.addEventListener("close", () => { dialog.remove(); trigger.focus(); });
    dialog.showModal();
    const money = value => Number(value || 0).toLocaleString("en-US", {minimumFractionDigits:2, maximumFractionDigits:2}) + " THB";
    const line = text => { const p = document.createElement("p"); p.textContent = text; body.append(p); };
    try {
        const response = await fetch(`/api/v1/borrow-requests/${trigger.dataset.returnReceipt}/return`);
        if (!response.ok) throw new Error("Unable to load charges. Please try again.");
        const receipt = await response.json(); body.replaceChildren();
        line(`Request #${receipt.borrowRequestId} - Returned ${receipt.returnDate}`);
        for (const item of receipt.items || []) {
            line(`${item.equipmentName}: ${item.condition.replaceAll("_", " ")} - ${item.purchasePrice == null ? "Price not recorded" : money(item.purchasePrice)} x ${Math.round(item.damageRate * 100)}% = ${money(item.damageAmount)}`);
            if (item.remark) line(item.remark);
        }
        if (!receipt.items?.length) line("This older return has no per-item inspection breakdown.");
        line(`Late fee: ${money(receipt.fineAmount)}`);
        line(`Damage charges: ${money(receipt.damageAmount)}`);
        line(`Total: ${money(receipt.totalAmount)}`);
        if (receipt.remark) line(`Note: ${receipt.remark}`);
        const paymentResponse = await fetch(`/api/v1/borrow-requests/${receipt.borrowRequestId}/settlement`);
        if (paymentResponse.ok) {
            const payment = await paymentResponse.json();
            line(payment.paid ? 'Payment recorded' : !payment.finalized ? 'Provisional charges: more equipment remains to be returned.' : Number(payment.total) > 0 ? 'Payment outstanding' : 'No payment due');
            for (const entry of payment.payments) line(`Receipt: ${entry.reference}`);
        }
    } catch (error) { body.textContent = error.message; }
});
