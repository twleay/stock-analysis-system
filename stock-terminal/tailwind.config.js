/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                'terminal-bg': '#0f172a', // 深蓝黑背景
                'terminal-card': '#1e293b', // 卡片背景
                'stock-up': '#ef4444',      // 涨 (红)
                'stock-down': '#22c55e',    // 跌 (绿)
                'accent': '#3b82f6',        // 科技蓝
            },
        },
    },
    plugins: [],
}