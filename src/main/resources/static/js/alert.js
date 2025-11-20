const alertSuccess = document.querySelector('.alert-success');
const alertDanger = document.querySelector('.alert-danger');
const alertInfo = document.querySelector('.alert-info');

function showAlert(alertElement) {
    if (!alertElement) return; // Si no existe, no hace nada
    alertElement.classList.add('showAnimationAppearFromTop');
    setTimeout(() => {
        alertElement.style.display = 'none';
    }, 5000); // Ocultar a los 5s
}

// Mostrar animación en cualquiera que exista
showAlert(alertSuccess);
showAlert(alertInfo);
showAlert(alertDanger);
