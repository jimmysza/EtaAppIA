document.querySelectorAll('.preguntas_card').forEach(card => {
    card.addEventListener('click', () => {
        const textContainer = card.querySelector('.preguntas_card_text');
        const icon = card.querySelector('.btn_preguntas_card svg');

        // Alternar la altura del contenido
        textContainer.classList.toggle('expanded');

        // Rotar el icono del botón
        icon.classList.toggle('rotated');
    });
});

  document.querySelector('.container-bookmark').addEventListener('click', function () {
    this.classList.toggle('bookmarked');
  });

