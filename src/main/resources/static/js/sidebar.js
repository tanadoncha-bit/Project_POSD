const toggleButton = document.getElementById("sidebar-toggle");
const sidebar = document.getElementById("main-sidebar");
const backdrop = document.getElementById("sidebar-backdrop");

function setSidebarOpen(isOpen) {
    document.body.classList.toggle("sidebar-open", isOpen);
    toggleButton.setAttribute("aria-expanded", String(isOpen));
    toggleButton.setAttribute(
        "aria-label",
        isOpen ? "ปิดเมนู" : "เปิดเมนู"
    );

    // เมื่อเมนูปิด จะไม่ให้กด Tab เข้าไปยังลิงก์ที่ซ่อนอยู่
    sidebar.inert = !isOpen;
}

setSidebarOpen(false);

toggleButton.addEventListener("click", () => {
    const isOpen = document.body.classList.contains("sidebar-open");
    setSidebarOpen(!isOpen);
});

backdrop.addEventListener("click", () => {
    setSidebarOpen(false);
    toggleButton.focus();
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" &&
        document.body.classList.contains("sidebar-open")) {
        setSidebarOpen(false);
        toggleButton.focus();
    }
});