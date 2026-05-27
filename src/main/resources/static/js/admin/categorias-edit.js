document.addEventListener('DOMContentLoaded', () => {
    const modal = document.querySelector('.modal.modal-edit');
    const editId = document.getElementById('edit-id');
    const editNombre = document.getElementById('edit-nombre');
    const editCurrentImage = document.getElementById('edit-current-image');

    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const id = btn.getAttribute('data-id');
            const nombre = btn.getAttribute('data-nombre');
            const imagen = btn.getAttribute('data-imagen');

            if (editId) editId.value = id;
            if (editNombre) editNombre.value = nombre || '';

            if (editCurrentImage) {
                if (imagen && imagen.trim().length > 0) {
                    editCurrentImage.innerHTML = `<img src="${imagen}" alt="${nombre}" style="width:120px;height:120px;object-fit:cover;border-radius:8px;" />`;
                } else {
                    editCurrentImage.innerHTML = '<div style="width:120px;height:120px;display:flex;align-items:center;justify-content:center;background:#f3f4f6;color:#9ca3af;border-radius:8px;">Sin imagen</div>';
                }
            }

            // Open modal
            modal.classList.add('show');
        });
    });
    // preview new file when selected
    const fileInput = document.getElementById('edit-imagenFile');
    if (fileInput) {
        fileInput.addEventListener('change', (e) => {
            const f = e.target.files && e.target.files[0];
            if (!f) return;
            const reader = new FileReader();
            reader.onload = function(ev) {
                if (editCurrentImage) editCurrentImage.innerHTML = `<img src="${ev.target.result}" alt="preview" style="width:120px;height:120px;object-fit:cover;border-radius:8px;" />`;
            };
            reader.readAsDataURL(f);
        });
    }

    const closeEditBtn = document.querySelector('.modal.modal-edit .close-edit');
    if (closeEditBtn && modal) {
        closeEditBtn.addEventListener('click', () => {
            modal.classList.remove('show');
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('show');
        }
    });
});
