
document.addEventListener("DOMContentLoaded", () => {
    const items = document.querySelectorAll(".descripcion-hover");
    const modal = document.getElementById("descripcion-modal");
    const modalText = document.getElementById("descripcion-text");

    let timer;

    items.forEach(item => {
        item.addEventListener("mouseenter", (e) => {
            timer = setTimeout(() => {
                // Texto dinámico
                modalText.textContent = item.getAttribute("data-descripcion");

                // Posición cerca del elemento hovered
                const rect = item.getBoundingClientRect();
                modal.style.top = `${rect.bottom + window.scrollY + 10}px`; // debajo de la celda
                modal.style.left = `${rect.left + window.scrollX}px`;

                modal.classList.remove("hidden");
            }, 2000); // 2 segundos
        });

        item.addEventListener("mouseleave", () => {
            clearTimeout(timer);
            modal.classList.add("hidden");
        });
    });
});


