document.addEventListener("DOMContentLoaded", () => {
  const passwordInput = document.getElementById("password");
  const toggleButton = document.getElementById("togglePassLogin");
  const eyeIcon = document.getElementById("eyeClosedLogin");

  if (!passwordInput || !toggleButton || !eyeIcon) return;

  // función para alternar visibilidad
  const togglePassword = () => {
    const isHidden = passwordInput.type === "password";
    passwordInput.type = isHidden ? "text" : "password";

    // Actualizar el ícono SVG
    eyeIcon.innerHTML = isHidden
      ? `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
         <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />`
      : `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.542-7a10.05 10.05 0 011.658-3.248M9.88 9.88A3 3 0 0114.12 14.12M20.12 20.12L3.88 3.88" />`;
  };

  // Click o espacio activan
  toggleButton.addEventListener("click", togglePassword);
  toggleButton.addEventListener("keydown", (e) => {
    if (e.key === " " || e.key === "Enter") { // barra espaciadora o enter
      e.preventDefault(); // evita scroll o submit accidental
      togglePassword();
    }
  });
});
