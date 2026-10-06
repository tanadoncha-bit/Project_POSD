(() => {
    async function loadPins() {
        const parts = new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Bangkok',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
        const part = type => parts.find(p => p.type === type).value;
        const today = `${part('year')}-${part('month')}-${part('day')}`;
        await Promise.all(Array.from(document.querySelectorAll('[data-locker-pin]')).map(async label => {
            if (label.dataset.loading) return;
            label.dataset.loading = 'true';
            const button = label.closest('[data-request-card]')?.querySelector('[data-confirm-pickup]');
            try {
                const response = await fetch(`/api/v1/borrow-requests/${label.dataset.lockerPin}/locker`, {cache:'no-store'});
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'PIN unavailable. Refresh to retry.');
                label.textContent = data.opened ? 'Locker opened' : `PIN: ${data.pin}`;
                if (button) {
                    button.disabled = today < data.availableFrom || today > data.expiresOn;
                    button.title = button.disabled ? `Pickup: ${data.availableFrom} to ${data.expiresOn}` : '';
                }
            } catch (error) { label.textContent = error.message; }
        }));
    }
    document.addEventListener('DOMContentLoaded', loadPins);
    document.addEventListener('requests:updated', loadPins);
    document.addEventListener('click', async event => {
        const button = event.target.closest('[data-confirm-pickup]');
        if (!button || button.disabled) return;
        if (!confirm('Confirm that you have collected all equipment in this request?')) return;
        button.disabled = true;
        try {
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            const token = document.querySelector('meta[name="_csrf"]').content;
            const response = await fetch(`/api/v1/borrow-requests/${button.dataset.confirmPickup}/pickup`, {method:'PATCH',headers:{[header]:token}});
            if (!response.ok) {
                const data = await response.json().catch(() => ({}));
                throw new Error(data.message || 'Unable to confirm pickup. Refresh and retry.');
            }
            location.reload();
        } catch (error) { window.toast?.error(error.message); button.disabled = false; }
    });
})();
