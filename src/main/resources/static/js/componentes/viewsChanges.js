// viewChanges.js
document.addEventListener('DOMContentLoaded', function() {
    const btnViewCards = document.getElementById('btnViewCards');
    const btnViewTable = document.getElementById('btnViewTable');
    const viewCards = document.getElementById('viewCards');
    const viewTable = document.getElementById('viewTable');

    if (!btnViewCards || !btnViewTable || !viewCards || !viewTable) {
        console.error('View toggle elements not found');
        return;
    }

    // Función para cambiar a vista Cards
    function showCards() {
        viewCards.style.display = 'block';
        viewTable.style.display = 'none';
        btnViewCards.classList.add('active');
        btnViewTable.classList.remove('active');
        localStorage.setItem('activityView', 'cards');
    }

    // Función para cambiar a vista Tabla
    function showTable() {
        viewCards.style.display = 'none';
        viewTable.style.display = 'block';
        btnViewTable.classList.add('active');
        btnViewCards.classList.remove('active');
        localStorage.setItem('activityView', 'table');
    }

    // Event listeners
    btnViewCards.addEventListener('click', showCards);
    btnViewTable.addEventListener('click', showTable);

    // Cargar vista guardada
    const savedView = localStorage.getItem('activityView');
    if (savedView === 'table') {
        showTable();
    } else {
        showCards();
    }
});