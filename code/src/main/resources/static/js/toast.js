/* Shared notifications: toast.success(message), toast.error(message, { duration: 6000 }). */
(() => {
    const config = { success: 3500, error: 5500, info: 3500, warning: 5500, limit: 4 };
    const active = new Map();
    let region;
    function show(type, message, options = {}) {
        const text = String(message || "").trim();
        if (!text) return;
        const key = type + ":" + text;

        if (!region) {
            region = document.createElement("div"); region.className = "toast-region";
            region.setAttribute("aria-label", "Notifications"); document.body.append(region);
        }
        const host = document.querySelector("dialog[open]") || document.body;
        if (region.parentElement !== host) host.append(region);
        if (active.has(key)) { active.get(key).restart(); return; }
        while (active.size >= config.limit) active.values().next().value.dismiss();
        const item = document.createElement("div"); item.className = `app-toast app-toast-${type}`;
        item.setAttribute("role", type === "error" ? "alert" : "status"); item.setAttribute("aria-atomic", "true");
        const icon = document.createElement("span"); icon.className = "toast-icon"; icon.setAttribute("aria-hidden", "true");
        const symbols = {
            success: '<circle cx="14" cy="14" r="13" fill="currentColor"/><path d="m7.5 14 4.5 4.5 8.5-10" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>',
            error: '<circle cx="14" cy="14" r="13" fill="currentColor"/><path d="M14 7v9" stroke="white" stroke-width="2.5" stroke-linecap="round"/><circle cx="14" cy="21" r="1.5" fill="white"/>',
            warning: '<path d="M11.4 3.8a3 3 0 0 1 5.2 0l10 18A3 3 0 0 1 24 26H4a3 3 0 0 1-2.6-4.2z" fill="currentColor"/><path d="M14 10v7" stroke="white" stroke-width="2.5" stroke-linecap="round"/><circle cx="14" cy="21.5" r="1.5" fill="white"/>',
            info: '<circle cx="14" cy="14" r="13" fill="currentColor"/><circle cx="14" cy="7.5" r="1.5" fill="white"/><path d="M12 12h2v8h2m-4 0h4" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"/>'
        };
        icon.innerHTML = '<svg viewBox="0 0 28 28" width="28" height="28">' + (symbols[type] || symbols.info) + '</svg>';
        const label = document.createElement("span"); label.className = "toast-message"; label.textContent = text;
        const close = document.createElement("button"); close.type = "button"; close.className = "toast-close";
        close.setAttribute("aria-label", "Dismiss notification"); close.textContent = "\u00d7";
        item.append(icon, label, close); region.append(item);
        const duration = options.duration ?? config[type];
        const progress = document.createElement("span"); progress.className = "toast-progress";
        progress.setAttribute("aria-hidden", "true");
        if (duration > 0) item.append(progress);
        const progressAnimation = duration > 0 && !window.matchMedia("(prefers-reduced-motion: reduce)").matches
            ? progress.animate([{ transform: "scaleX(1)" }, { transform: "scaleX(0)" }], { duration, fill: "forwards" }) : null;
        progressAnimation?.pause();
        let remaining = duration, started = 0, timer, hovered = false, focused = false, removed = false;
        function dismiss() {
            if (removed) return;
            removed = true; clearTimeout(timer); progressAnimation?.pause(); active.delete(key);
            item.classList.add("toast-leaving"); setTimeout(() => item.remove(), 160);
        }
        function pause() { progressAnimation?.pause(); if (timer) { clearTimeout(timer); timer = null; remaining = Math.max(0, remaining - (Date.now() - started)); } }
        function resume() { if (!removed && !hovered && !focused && duration > 0) { started = Date.now(); progressAnimation?.play(); timer = setTimeout(dismiss, remaining); } }
        function restart() { pause(); remaining = duration; if (progressAnimation) progressAnimation.currentTime = 0; resume(); }
        item.addEventListener("pointerenter", () => { hovered = true; pause(); });
        item.addEventListener("pointerleave", () => { hovered = false; resume(); });
        item.addEventListener("focusin", () => { focused = true; pause(); });
        item.addEventListener("focusout", event => { if (!item.contains(event.relatedTarget)) { focused = false; resume(); } });
        close.addEventListener("click", dismiss);
        active.set(key, { dismiss, restart }); resume();
        return dismiss;
    }
    window.toast = {
        config,
        success: (message, options) => show("success", message, options),
        error: (message, options) => show("error", message, options),
        warning: (message, options) => show("warning", message, options),
        info: (message, options) => show("info", message, options)
    };
    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("[data-toast]").forEach(source => {
            const message = source.textContent.trim();
            source.hidden = true;
            if (message) show(source.dataset.toast, message);
        });
    });
})();
