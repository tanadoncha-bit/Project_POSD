window.workflowApi = async function (url, options = {}) {
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const response = await fetch(url, { ...options, headers: { "Content-Type": "application/json", ...(header && token ? { [header]: token } : {}), ...(options.headers || {}) } });
    const text = await response.text(); let body = {}; try { body = text ? JSON.parse(text) : {}; } catch { }
    if (!response.ok) throw new Error(body.message || "Unable to complete this action."); return body;
};
window.workflowForm = function (title, description, label, submit, options = {}) {
    return new Promise(resolve => {
        const dialog = document.createElement('dialog'); dialog.className = 'workflow-dialog';
        if (options.payment) dialog.classList.add('payment-dialog');
        const form = document.createElement('form'); const heading = document.createElement('h2'); heading.textContent = title;
        const intro = document.createElement('p'); intro.textContent = description;
        const field = document.createElement('label'); field.textContent = label;
        const input = document.createElement(options.payment || options.singleLine ? 'input' : 'textarea'); input.required = true; input.maxLength = 500;
        if (options.payment) { input.type = 'text'; input.placeholder = 'e.g. Receipt #2026-001 or transfer reference'; input.autocomplete = 'off'; }
        if (options.singleLine) { input.type='text'; input.autocomplete='one-time-code'; input.maxLength=100; input.placeholder='Paste the code from your email'; input.className='verification-code-input'; }
  field.append(input);
        const error = document.createElement('p'); error.setAttribute('role', 'alert');
        const footer = document.createElement('footer'); const cancel = document.createElement('button'); cancel.type = 'button'; cancel.textContent = options.payment ? 'Cancel' : 'Close'; cancel.onclick = () => dialog.close();
        const confirm = document.createElement('button'); confirm.type = 'submit'; confirm.textContent = options.payment ? 'Confirm payment received' : options.singleLine ? 'Verify email' : 'Confirm'; footer.append(cancel, confirm);
        form.append(heading, intro);
        if (options.payment) {
            const info = options.payment;
            const money = value => Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' THB';
            const summary = document.createElement('section'); summary.className = 'payment-summary';
            const borrower = document.createElement('p'); borrower.className = 'payment-borrower'; borrower.textContent = `Request #${info.borrowRequestId} / ${info.username}`;
            const caption = document.createElement('span'); caption.textContent = 'Amount to collect';
            const amount = document.createElement('strong'); amount.className = 'payment-amount'; amount.textContent = money(info.total);
            const breakdown = document.createElement('p'); breakdown.textContent = `Late fee ${money(info.fineAmount)} / Damage ${money(info.damageAmount)}`;
            summary.append(borrower, caption, amount, breakdown); form.append(summary);
        }
        form.append(field, error, footer); dialog.append(form); document.body.append(dialog);
        const previous = document.activeElement; let completed = false;
        dialog.addEventListener('close', () => { dialog.remove(); previous?.focus(); resolve(completed); }, { once: true });
        form.onsubmit = async event => { event.preventDefault(); if (confirm.disabled) return; if (!input.value.trim()) { input.setCustomValidity(options.singleLine ? 'Enter your verification code.' : 'Enter a reference.'); input.reportValidity(); return; } confirm.disabled = true; cancel.disabled = true; error.textContent = ''; const text = confirm.textContent; confirm.textContent = options.singleLine ? 'Verifying...' : 'Saving...'; try { await submit(input.value.trim()); completed = true; dialog.close(); } catch (e) { error.textContent = e.message; } finally { confirm.disabled = false; cancel.disabled = false; confirm.textContent = text; } };
        input.addEventListener('input', () => input.setCustomValidity(''));
        dialog.addEventListener('cancel', event => { if (confirm.disabled) event.preventDefault(); });
        dialog.showModal(); input.focus();
    });
};
window.workflowAction = async function (kind, id) {
    if (kind === 'settlement') {
        const info = await workflowApi(`/api/v1/borrow-requests/${id}/settlement`);
        if (info.paid) { toast.info('Payment has already been recorded.'); return false; }
        if (!info.finalized) { toast.info('Complete all equipment returns before recording payment.'); return false; }
        if (Number(info.total) <= 0) { toast.info('No payment due. Total charges are 0.00 THB.'); return false; }
        return workflowForm('Record payment', 'Record this payment after you have received the full amount.', 'Receipt / payment reference', reference => workflowApi(`/api/v1/borrow-requests/${id}/settlement`, { method: 'POST', body: JSON.stringify({ reference, amount: info.total }) }), { payment: info });
    }
    const repair = kind === 'repair';
    return workflowForm(repair ? 'Complete repair' : 'Reject request', repair ? 'Confirm the equipment has been repaired and is ready to borrow.' : 'The borrower will see this reason.', 'Reason / details', reason => workflowApi(repair ? `/api/v1/equipment/${id}/repair` : `/api/v1/borrow-requests/${id}/reject`, { method: 'POST', body: JSON.stringify({ reason }) }));
};
