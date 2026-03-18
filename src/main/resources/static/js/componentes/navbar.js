document.addEventListener("DOMContentLoaded", function () {
    const dropdowns = document.querySelectorAll(".dropdown");
    const navbarMenu = document.getElementById("menu");
    const burgerMenu = document.getElementById("burger");

    const closeAllDropdowns = () => {
        dropdowns.forEach((dropdown) => {
            dropdown.classList.remove("dropdownUp");
            const container = dropdown.querySelector(".dropdown-contain");
            if (container) {
                container.classList.remove("flexing");
            }
        });
    };

    dropdowns.forEach((dropdown) => {
        const trigger = dropdown.querySelector(".account-trigger");
        const container = dropdown.querySelector(".dropdown-contain");

        if (!trigger || !container) {
            return;
        }

        trigger.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            const isOpen = container.classList.contains("flexing");
            closeAllDropdowns();

            if (!isOpen) {
                dropdown.classList.add("dropdownUp");
                container.classList.add("flexing");
            }
        });
    });

    document.addEventListener("click", function (event) {
        dropdowns.forEach((dropdown) => {
            if (!dropdown.contains(event.target)) {
                dropdown.classList.remove("dropdownUp");
                const container = dropdown.querySelector(".dropdown-contain");
                if (container) {
                    container.classList.remove("flexing");
                }
            }
        });
    });

    document.querySelectorAll(".dropdown-contain a").forEach((link) => {
        link.addEventListener("click", function () {
            closeAllDropdowns();
        });
    });

    if (burgerMenu && navbarMenu) {
        burgerMenu.addEventListener("click", () => {
            burgerMenu.classList.toggle("is-active");
            navbarMenu.classList.toggle("is-active");
            burgerMenu.setAttribute("aria-expanded", String(navbarMenu.classList.contains("is-active")));
        });
    }

    document.querySelectorAll(".menu-link, .btn-auth").forEach((link) => {
        link.addEventListener("click", () => {
            if (burgerMenu && navbarMenu && window.innerWidth < 910) {
                burgerMenu.classList.remove("is-active");
                navbarMenu.classList.remove("is-active");
                burgerMenu.setAttribute("aria-expanded", "false");
            }
        });
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth >= 910 && burgerMenu && navbarMenu) {
            burgerMenu.classList.remove("is-active");
            navbarMenu.classList.remove("is-active");
            burgerMenu.setAttribute("aria-expanded", "false");
        }
    });
});
