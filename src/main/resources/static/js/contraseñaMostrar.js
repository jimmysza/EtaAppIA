document.addEventListener("DOMContentLoaded", () => {
  const passwordInput = document.getElementById("password");
  const passwordConfirmInput = document.getElementById("passwordConfirm");

  // Recoger todos los botones toggle disponibles (puede haber uno por cada campo)
  const toggleButtons = Array.from(document.querySelectorAll('.password-toggle'));
  const eyeSvgs = toggleButtons.map(b => b.querySelector('svg')).filter(Boolean);

  // función para alternar visibilidad de ambos campos si existen
  const togglePassword = () => {
    const targets = [passwordInput, passwordConfirmInput].filter(Boolean);
    if (targets.length === 0) return;
    const isHidden = targets[0].type === "password";
    targets.forEach(t => t.type = isHidden ? 'text' : 'password');

    // Actualizar todos los íconos SVG en los toggles
    eyeSvgs.forEach(svg => {
      svg.innerHTML = isHidden
        ? `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
           <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />`
        : `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.542-7a10.05 10.05 0 011.658-3.248M9.88 9.88A3 3 0 0114.12 14.12M20.12 20.12L3.88 3.88" />`;
    });
  };

  // Click o espacio activan en todos los botones
  toggleButtons.forEach(btn => {
    btn.addEventListener('click', togglePassword);
    btn.addEventListener('keydown', (e) => {
      if (e.key === ' ' || e.key === 'Enter') {
        e.preventDefault();
        togglePassword();
      }
    });
  });

  // Validador util para verificar igualdad de contraseñas. Retorna true si ok, false si no.
  window.validatePasswordsMatch = function(form) {
    const p = passwordInput ? passwordInput.value.trim() : '';
    const c = passwordConfirmInput ? passwordConfirmInput.value.trim() : '';
    // Si no hay campo confirm, nothing to validate here
    if (!passwordConfirmInput) return true;
    if (p !== c) {
      const message = 'Las contraseñas no coinciden';
      if (typeof mostrarToast === 'function') mostrarToast(message, 'error');
      else alert(message);
      passwordConfirmInput.focus();
      return false;
    }
    return true;
  };

  // Añadir validación en submit del formulario si existe (para registroColaborador sin modal)
  const form = document.getElementById('registroForm');
  if (form) {
    form.addEventListener('submit', (e) => {
      // Si la página usa modal para el cliente, su flujo llama a submitFormFinal.
      // Aquí interceptamos para el caso general: validar igualdad y prevenir envío si falla.
      if (!window.validatePasswordsMatch(form)) {
        e.preventDefault();
      }
    });
  }
});
