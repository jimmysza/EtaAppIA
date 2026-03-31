/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./src/main/resources/templates/**/*.html",
        "./src/main/resources/static/**/*.js",
    ],
    theme: {
        extend: {
            colors: {
                // Colores personalizados de tu proyecto ETA
                'color-main': 'var(--color-main)',
                'color-blue': 'var(--color-blue)',
                'color-medium-blue': 'var(--color-medium-blue)',
                'color-blur': 'var(--color-blur)',
                'color-yellow': 'var(--color-yellow)',
                'color-orange': 'var(--color-orange)',
                'color-green': 'var(--color-green)',
                'color-red': 'var(--color-red)',
            },
            fontFamily: {
                sans: ['Poppins', 'sans-serif'],
            },
        },
    },
    plugins: [],
}
