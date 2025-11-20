document.addEventListener("DOMContentLoaded", function () {
            const dropdowns = document.querySelectorAll('.dropdown');

            dropdowns.forEach(dropdown => {
                const trigger = dropdown.querySelector('a'); // Solo el primer <a> (el que abre el dropdown)
                const container = dropdown.querySelector('.dropdown-contain');

                if (!trigger || !container) return;

                // Solo prevenir comportamiento en el TRIGGER (imagen de perfil), no en los enlaces internos
                trigger.addEventListener('click', function (e) {
                    e.preventDefault(); // Solo aquí, para evitar que el # recargue la página
                    e.stopPropagation();

                    // Cerrar otros dropdowns
                    dropdowns.forEach(d => {
                        if (d !== dropdown) {
                            d.classList.remove('dropdownUp');
                            const c = d.querySelector('.dropdown-contain');
                            if (c) c.classList.remove('flexing');
                        }
                    });

                    // Toggle este dropdown
                    dropdown.classList.toggle('dropdownUp');
                    container.classList.toggle('flexing');
                });
            });

            // Cerrar dropdown si se hace clic fuera
            document.addEventListener('click', function (e) {
                dropdowns.forEach(dropdown => {
                    if (!dropdown.contains(e.target)) {
                        dropdown.classList.remove('dropdownUp');
                        const container = dropdown.querySelector('.dropdown-contain');
                        if (container) container.classList.remove('flexing');
                    }
                });
            });

            // ¡IMPORTANTE! Permitir que los enlaces DENTRO del dropdown funcionen normalmente
            document.querySelectorAll('.dropdown-contain a').forEach(link => {
                link.addEventListener('click', function (e) {
                    // No prevenir el comportamiento aquí → permitir que el enlace funcione
                    // Solo cerrar el dropdown después de navegar (opcional)
                    setTimeout(() => {
                        dropdowns.forEach(d => {
                            d.classList.remove('dropdownUp');
                            const c = d.querySelector('.dropdown-contain');
                            if (c) c.classList.remove('flexing');
                        });
                    }, 100);
                });
            });
        });


        const navbarMenu = document.getElementById("menu");
        const burgerMenu = document.getElementById("burger");

        // Open/Close Navbar Menu on Click Burger
        if (burgerMenu && navbarMenu) {
            burgerMenu.addEventListener("click", () => {
                burgerMenu.classList.toggle("is-active");
                navbarMenu.classList.toggle("is-active");
            });
        }

        // Close Navbar Menu on Click Links
        document.querySelectorAll(".menu-link").forEach((link) => {
            link.addEventListener("click", () => {
                if (burgerMenu && navbarMenu) {
                    burgerMenu.classList.remove("is-active");
                    navbarMenu.classList.remove("is-active");
                }
            });
        });

        // Fixed Navbar Menu on Window Resize
        window.addEventListener("resize", () => {
            if (window.innerWidth >= 768) {
                if (navbarMenu && navbarMenu.classList.contains("is-active")) {
                    navbarMenu.classList.remove("is-active");
                }
            }
        });
