// Compensate the page, not the viewport: fixed modal backdrops stay edge-to-edge.
(() => {
    const root = document.documentElement;
    const body = document.body;
    function updateScrollbarWidth() {
        let width = window.innerWidth - root.clientWidth;
        if (body.classList.contains("modal-open")) {
            const probe = document.createElement("div");
            probe.style.cssText = "position:absolute;top:-9999px;width:100px;height:100px;overflow:scroll;visibility:hidden;";
            body.append(probe);
            width = root.scrollHeight > window.innerHeight ? probe.offsetWidth - probe.clientWidth : 0;
            probe.remove();
        }
        root.style.setProperty("--modal-scrollbar-width", `${Math.max(0, width)}px`);
    }
    function updateVisibleViewport() {
        const viewport = window.visualViewport;
        root.style.setProperty("--modal-viewport-height", `${viewport?.height ?? window.innerHeight}px`);
        root.style.setProperty("--modal-viewport-top", `${viewport?.offsetTop ?? 0}px`);
    }
    updateVisibleViewport();
    window.addEventListener("resize", updateVisibleViewport);
    window.visualViewport?.addEventListener("resize", updateVisibleViewport);
    window.visualViewport?.addEventListener("scroll", updateVisibleViewport);
    updateScrollbarWidth();
    window.addEventListener("resize", updateScrollbarWidth);
    window.addEventListener("load", updateScrollbarWidth);
    new ResizeObserver(() => {
        if (!body.classList.contains("modal-open")) updateScrollbarWidth();
    }).observe(body);
})();
