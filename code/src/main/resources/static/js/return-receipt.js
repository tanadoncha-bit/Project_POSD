// Share the persisted receipt with both operators and the borrower.
document.addEventListener("click", async event => {
    const trigger = event.target.closest("[data-return-receipt]");
    if (!trigger) return;
    const dialog = document.createElement("dialog");
    dialog.className = "return-receipt";
    const title = document.createElement("h2"); title.textContent = "Return charges";
    const body = document.createElement("div"); body.textContent = "Loading..."; body.setAttribute("aria-live", "polite");
    const close = document.createElement("button"); close.type = "button"; close.textContent = "Close";
    title.id = 'return-receipt-title';
    dialog.setAttribute('aria-labelledby', title.id);
    const header = document.createElement('header'); header.append(title);
    const footer = document.createElement('footer'); footer.append(close);
    dialog.append(header, body, footer); document.body.append(dialog);
    close.addEventListener("click", () => dialog.close());
    dialog.addEventListener("close", () => { dialog.remove(); trigger.focus(); });
    dialog.showModal();
    const money = value => Number(value || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " THB";
    const node = (tag, className, text) => {
        const element = document.createElement(tag); element.className = className;
        if (text != null) element.textContent = text;
        return element;
    };
    const line = text => body.append(node('p', '', text));
    try {
        const response = await fetch(`/api/v1/borrow-requests/${trigger.dataset.returnReceipt}/return`);
        if (!response.ok) throw new Error("Unable to load charges. Please try again.");
        const receipt = await response.json(); body.replaceChildren();
        body.append(node('p', 'receipt-meta', `Request #${receipt.borrowRequestId} / Returned ${receipt.returnDate}`));
        const items = node('div', 'receipt-items'); body.append(items);
        const conditions = { NORMAL: 'Normal', MINOR_SCRATCHES: 'Minor scratches', DAMAGED: 'Damaged', LOST: 'Lost' };
        for (const item of receipt.items || []) {
            const row = node('article', 'receipt-item');
            const heading = node('div', 'receipt-item-heading');
            heading.append(node('h3', '', item.equipmentName), node('strong', '', money(item.damageAmount)));
            row.append(heading, node('span', 'receipt-condition', conditions[item.condition] || item.condition));
            if (Number(item.damageRate) > 0) row.append(node('p', 'receipt-calculation', `${item.purchasePrice == null ? 'Price not recorded' : money(item.purchasePrice)} x ${Math.round(item.damageRate * 100)}% damage rate`));
            if (item.remark) row.append(node('p', 'receipt-note', item.remark));
            items.append(row);
        }
        if (!receipt.items?.length) line('This older return has no per-item inspection breakdown.');
        const summary = node('dl', 'receipt-summary');
        for (const [label, amount] of [['Late fee', receipt.fineAmount], ['Damage charges', receipt.damageAmount], ['Total charges', receipt.totalAmount]]) {
            const row = node('div', label === 'Total charges' ? 'receipt-total' : '');
            row.append(node('dt', '', label), node('dd', '', money(amount))); summary.append(row);
        }
        body.append(summary);
        if (receipt.remark) body.append(node('p', 'receipt-note', `Return note: ${receipt.remark}`));
        const paymentResponse = await fetch(`/api/v1/borrow-requests/${receipt.borrowRequestId}/settlement`);
        if (paymentResponse.ok) {
            const payment = await paymentResponse.json();
            const status = node('p', 'receipt-payment-status', payment.paid ? 'Payment recorded' : !payment.finalized ? 'Provisional charges: more equipment remains to be returned.' : Number(payment.total) > 0 ? 'Payment outstanding' : 'No payment due');
            status.dataset.paid = String(payment.paid || (payment.finalized && Number(payment.total) === 0));
            const paymentSection = node('section', 'receipt-payment-section');
            paymentSection.append(status);
            for (const entry of payment.payments || []) {
                const reference = node('div', 'receipt-payment-reference');
                reference.append(node('span', '', 'Payment reference'), node('strong', '', entry.reference));
                paymentSection.append(reference);
            }
            body.append(paymentSection);
        }
    } catch (error) { body.textContent = error.message; }
});
