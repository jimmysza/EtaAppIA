document.addEventListener('DOMContentLoaded', () => {
    const setupModal = (openBtnElements, modalSelector, closeBtnSelector, onOpenCallback) => {
        const modal = document.querySelector(modalSelector);
        const closeBtn = modal?.querySelector(closeBtnSelector);

        if (!modal || !closeBtn) return;

        openBtnElements.forEach(openBtn => {
            openBtn.addEventListener('click', (e) => {
                e.preventDefault();
                if (onOpenCallback) onOpenCallback(openBtn, modal);
                modal.classList.add('show');
            });
        });

        closeBtn.addEventListener('click', () => {
            modal.classList.remove('show');
        });

        window.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('show');
            }
        });
    };

    // Modal de Eliminar con ID dinámico
    setupModal(
        document.querySelectorAll('.text-open-delete'),
        '.modal-delete',
        '.close-btn-modal',
        (openBtn, modal) => {
            const id = openBtn.getAttribute('data-id');
            const deleteBtn = modal.querySelector('#delete-confirm-btn');
            if (deleteBtn && id) {
                deleteBtn.setAttribute('href', `/colaborador/eliminar/${id}`);
            }
        }
    );

    // Modal de Agregar (si lo tienes)
    const addModalBtn = document.querySelector('#open-add-modal');
    if (addModalBtn) {
        setupModal([addModalBtn], '.modal-add', '.close-modal');
    }
});
