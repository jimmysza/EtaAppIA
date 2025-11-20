const dropdowns = document.querySelectorAll('.dropdown');

dropdowns.forEach(dropdown => {
    const container = dropdown.querySelector('.dropdown-grafica-container');
    dropdown.addEventListener('click', function (e) {
        if (e.target.tagName === 'A') e.preventDefault();

        // Cerrar todos los dropdowns excepto el actual
        dropdowns.forEach(d => {
            if (d !== dropdown) {
                d.classList.remove('dropdownUp');
                const c = d.querySelector('.dropdown-grafica-container');
                if (c) c.classList.remove('flexing');
            }
        });

        // Alternar el dropdown clicado
        dropdown.classList.toggle('dropdownUp');
        if (container) container.classList.toggle('flexing');
    });
});

