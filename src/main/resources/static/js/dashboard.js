const sidebar = document.querySelector('.side-bar');
const sidebarToggle = document.getElementById('hideBar');

sidebarToggle.addEventListener("click", () => {
    sidebar.classList.toggle("translate-bar");
    sidebarToggleDashboard.classList.toggle("show");
});
