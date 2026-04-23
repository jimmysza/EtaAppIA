// === NAVBAR ===
document.addEventListener("DOMContentLoaded", () => {
    const sections = document.querySelectorAll("section");
    const navLinks = document.querySelectorAll(".nav-link");
    const sticky = document.querySelector(".sticky-container");
    const navbar = document.querySelector("#navbar");

    // --- Efecto de sombra y mostrar/ocultar enlaces ---
    window.addEventListener("scroll", () => {
        if (window.scrollY > 195) {
            sticky?.classList.add("shadow");
            navbar.classList.add("border-none");
            navbar.classList.add("bg-white");

            // Mostrar enlaces cuando se hace scroll
            navLinks.forEach(nav => {
                nav.classList.remove("nav-hidden");
            });
        } else {
            sticky?.classList.remove("shadow");
            navbar.classList.remove("border-none");

            // Ocultar enlaces cuando vuelve al tope
            navLinks.forEach(nav => {
                nav.classList.add("nav-hidden");
                navbar.classList.remove("bg-white");
            });
        }
    });

    // --- Resaltar enlace activo según la sección visible ---
    window.addEventListener("scroll", () => {
        let current = "";

        sections.forEach((section) => {
            const sectionTop = section.offsetTop - 120;
            const sectionHeight = section.offsetHeight;

            if (window.scrollY >= sectionTop && window.scrollY < sectionTop + sectionHeight) {
                current = section.getAttribute("id");
            }
        });

        navLinks.forEach((link) => {
            link.classList.remove("active");
            if (link.getAttribute("href") === `#${current}`) {
                link.classList.add("active");
            }
        });

    });
});


// === MODAL DE COMENTARIOS (adaptado a modal-add) ===
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector(".modal.modal-add");
    const openModalBtn = document.getElementById("open-add-modal");
    const closeModalBtn = modal ? modal.querySelector(".close-modal, .close-btn-modal") : null;

    if (!modal || !openModalBtn || !closeModalBtn) {
        console.warn("⚠️ No se encontraron los elementos del modal de comentarios (modal-add)");
        return;
    }

    openModalBtn.addEventListener("click", (e) => {
        e.preventDefault();
        modal.classList.add("show");
        document.body.style.overflow = "hidden"; // evita scroll del fondo
    });

    closeModalBtn.addEventListener("click", () => {
        modal.classList.remove("show");
        document.body.style.overflow = "auto";
    });

    // Cerrar modal al hacer clic fuera
    window.addEventListener("click", (e) => {
        if (e.target === modal) {
            modal.classList.remove("show");
            document.body.style.overflow = "auto";
        }
    });
});
