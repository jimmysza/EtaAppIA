document.addEventListener('DOMContentLoaded', () => {
    const closeBtn = document.getElementById('closeErrorBtn');
    const errorModal = document.getElementById('errorModal');

    if (closeBtn && errorModal) {
        closeBtn.addEventListener('click', () => {
            errorModal.classList.remove('show');
        });
    }

    // Verificar si hay un error al cargar la página
    if (errorModal && errorModal.classList.contains('show')) {
        // Asegurarse de que el modal sea visible
        errorModal.style.display = 'block';
    }
});