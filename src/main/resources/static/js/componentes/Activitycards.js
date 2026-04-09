document.addEventListener('DOMContentLoaded', () => {
    const carousels = document.querySelectorAll('[data-carousel]');

    carousels.forEach((carousel) => {
        const track = carousel.querySelector('[data-carousel-track]');
        const prevBtn = carousel.parentElement?.querySelector('[data-carousel-prev]') || carousel.querySelector('[data-carousel-prev]');
        const nextBtn = carousel.parentElement?.querySelector('[data-carousel-next]') || carousel.querySelector('[data-carousel-next]');

        if (!track || !prevBtn || !nextBtn) {
            return;
        }

        const getScrollAmount = () => {
            const firstCard = track.querySelector('.card');
            if (!firstCard) {
                return track.clientWidth * 0.8;
            }

            const cardStyles = window.getComputedStyle(firstCard);
            const trackStyles = window.getComputedStyle(track);
            const gap = parseFloat(trackStyles.columnGap || trackStyles.gap || '16');
            const cardWidth = firstCard.getBoundingClientRect().width;
            const cardMarginRight = parseFloat(cardStyles.marginRight || '0');

            return (cardWidth + gap + cardMarginRight) * 2;
        };

        const updateButtons = () => {
            const maxScrollLeft = track.scrollWidth - track.clientWidth;
            const hasOverflow = maxScrollLeft > 1;

            prevBtn.disabled = !hasOverflow || track.scrollLeft <= 1;
            nextBtn.disabled = !hasOverflow || track.scrollLeft >= maxScrollLeft - 1;
        };

        prevBtn.addEventListener('click', () => {
            track.scrollBy({
                left: -getScrollAmount(),
                behavior: 'smooth'
            });
        });

        nextBtn.addEventListener('click', () => {
            track.scrollBy({
                left: getScrollAmount(),
                behavior: 'smooth'
            });
        });

        track.addEventListener('scroll', updateButtons, { passive: true });
        window.addEventListener('resize', updateButtons);
        updateButtons();
    });
});
