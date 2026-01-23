const scrollContainerCatCat = document.getElementById('scrollContainerCat');
const scrollLeftBtnCat = document.getElementById('scrollLeftCat');
const scrollRightBtnCat = document.getElementById('scrollRightCat');

// Calcular el ancho de scroll basado en el viewport
function getScrollAmount() {
    const containerWidth = scrollContainerCat.offsetWidth;
    return containerWidth * 0.8; // Desplazar 80% del ancho visible
}

// Scroll a la izquierda
scrollLeftBtnCat.addEventListener('click', () => {
    scrollContainerCat.scrollBy({
        left: -getScrollAmount(),
        behavior: 'smooth'
    });
});

// Scroll a la derecha
scrollRightBtnCat.addEventListener('click', () => {
    scrollContainerCat.scrollBy({
        left: getScrollAmount(),
        behavior: 'smooth'
    });
});

// Actualizar estado de los botones
function updateButtonStates() {
    const hasOverflow =
        scrollContainerCat.scrollWidth > scrollContainerCat.clientWidth + 1;

    scrollLeftBtnCat.style.display = hasOverflow ? 'flex' : 'none';
    scrollRightBtnCat.style.display = hasOverflow ? 'flex' : 'none';

    scrollLeftBtnCat.disabled = scrollContainerCat.scrollLeft <= 0;
    scrollRightBtnCat.disabled =
        scrollContainerCat.scrollLeft >=
        scrollContainerCat.scrollWidth - scrollContainerCat.clientWidth - 1;
}


// Escuchar el evento de scroll
scrollContainerCat.addEventListener('scroll', updateButtonStates);

// Estado inicial
updateButtonStates();

// Actualizar al cambiar el tamaño de la ventana
window.addEventListener('resize', updateButtonStates);