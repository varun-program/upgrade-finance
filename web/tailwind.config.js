/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: {
          light: 'var(--theme-bg)',
          dark: 'var(--theme-bg)',
        },
        card: {
          light: 'var(--theme-glass-bg)',
          dark: 'var(--theme-glass-bg)',
        },
        primary: {
          DEFAULT: 'var(--theme-primary)',
          hover: 'var(--theme-primary-hover)',
        },
        accent: {
          green: '#10b981',
          red: '#ef4444',
          yellow: '#f59e0b',
        }
      },
      fontFamily: {
        sans: ['Outfit', 'Inter', 'sans-serif'],
      },
      backdropBlur: {
        xs: '2px',
      }
    },
  },
  plugins: [],
}
