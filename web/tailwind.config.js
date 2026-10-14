/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f5f7fa', 100: '#e9edf3', 200: '#cbd4e2', 300: '#a4b2ca',
          400: '#7388a8', 500: '#4b6387', 600: '#2f486e', 700: '#1e3a66',
          800: '#16294d', 900: '#0e1b33',
        },
        gold: {
          50: '#fbf8ef', 100: '#f7eed2', 200: '#eddaa0', 300: '#e3c368',
          400: '#d6af45', 500: '#c9a227', 600: '#a9840f', 700: '#8c6c10',
          800: '#6f550f', 900: '#533f0c',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
