
document.addEventListener("DOMContentLoaded", function () {
    const lenis = new Lenis({
        autoRaf: true,
        smoothWheel: true,
        lerp: 0.1
    });

    function smoothLoop(time) {
        lenis.raf(time);
        requestAnimationFrame(smoothLoop);
    }
    requestAnimationFrame(smoothLoop);
});
