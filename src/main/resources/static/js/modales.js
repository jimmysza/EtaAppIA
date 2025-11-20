// Espera a que el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {

    const setupModal = (openBtnElements, modalSelector, closeBtnSelector) => {
        const modal = document.querySelector(modalSelector);
        const closeBtn = modal?.querySelector(closeBtnSelector);

        if (!modal || !closeBtn) return;

        // Recorre cada botón que abre el modal
        openBtnElements.forEach(openBtn => {
            openBtn.addEventListener('click', (e) => {
                e.preventDefault();
                modal.classList.add('show'); // 🔓 abrir
            });
        });

        // Cierra con el botón X
        closeBtn.addEventListener('click', () => {
            modal.classList.remove('show'); // 🔒 cerrar
        });

        // Cierra si se hace click fuera del modal
        window.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('show'); // 🔒 cerrar
            }
        });
    };

    // ⚡ Configura los modales que quieras abrir
    /*setupModal(document.querySelectorAll('.text-open-delete'), '.modal-delete', '.close-btn-modal');*/
    setupModal([document.querySelector('#open-add-modal')], '.modal-add', '.close-btn-modal');
    setupModal(document.querySelectorAll('.edit-btn'), '.modal-edit', '.close-edit');

});
