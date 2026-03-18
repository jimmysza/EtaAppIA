const sidebar = document.querySelector('.sb-root');
const sidebarToggle = document.getElementById('hideBar');

sidebarToggle.addEventListener("click", () => {
    sidebar.classList.toggle("translate-bar");
    sidebarToggleDashboard.classList.toggle("show");
});
