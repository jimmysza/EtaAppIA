document.addEventListener('DOMContentLoaded', () => {
    const closeBtn = document.querySelector('.close-repetido');
    const errorBox = document.querySelector('.error-message');

    if (closeBtn && errorBox) {
        closeBtn.addEventListener('click', () => {
            errorBox.classList.remove('show'); // Elimina la clase 'show' para cerrar el modal
        });
    }
});





