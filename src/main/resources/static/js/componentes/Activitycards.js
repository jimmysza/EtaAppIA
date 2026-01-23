  
const scrollContainer = document.getElementById('scrollContainer');
const scrollLeftBtn = document.getElementById('scrollLeft');
const scrollRightBtn = document.getElementById('scrollRight');

// Función para actualizar el estado de los botones
function updateButtonStates() {
    const isAtStart = scrollContainer.scrollLeft <= 0;
    const isAtEnd = scrollContainer.scrollLeft + scrollContainer.clientWidth >= scrollContainer.scrollWidth - 1;

    scrollLeftBtn.disabled = isAtStart;
    scrollRightBtn.disabled = isAtEnd;
}

// Scroll suave al hacer clic en los botones
scrollLeftBtn.addEventListener('click', () => {
    const cardWidth = scrollContainer.querySelector('.card').offsetWidth;
    const gap = 16; // gap-4 = 16px
    scrollContainer.scrollBy({
        left: -(cardWidth + gap) * 2,
        behavior: 'smooth'
    });
});

scrollRightBtn.addEventListener('click', () => {
    const cardWidth = scrollContainer.querySelector('.card').offsetWidth;
    const gap = 16; // gap-4 = 16px
    scrollContainer.scrollBy({
        left: (cardWidth + gap) * 2,
        behavior: 'smooth'
    });
});

// Actualizar botones cuando se hace scroll
scrollContainer.addEventListener('scroll', updateButtonStates);

// Actualizar botones al cargar y al cambiar tamaño de ventana
window.addEventListener('load', updateButtonStates);
window.addEventListener('resize', updateButtonStates);

// Inicializar estado de botones
updateButtonStates();
