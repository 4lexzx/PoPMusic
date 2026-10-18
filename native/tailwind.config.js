/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./App.tsx', './index.ts', './src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        bg: '#000000',
        'bg-elev': '#0a0a0b',
        surface: '#0d0d0f',
        'surface-2': '#17171a',
        'surface-3': '#232327',
        line: '#1d1d21',
        'line-strong': '#2a2a30',
        ink: '#ffffff',
        muted: '#b4b4bb',
        faint: '#74747c',
        primary: '#f4f4f5',
        'primary-soft': '#ffffff',
        danger: '#ff4d4d',
      },
      fontFamily: {
        sans: ['Inter_400Regular'],
      },
    },
  },
  plugins: [],
}
