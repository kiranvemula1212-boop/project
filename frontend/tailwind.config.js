/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "var(--canvas)",
        surface: "var(--surface)",
        border: {
          DEFAULT: "var(--border)",
          strong: "var(--border-strong)",
        },
        ink: {
          DEFAULT: "var(--ink)",
          muted: "var(--ink-muted)",
          faint: "var(--ink-faint)",
        },
        accent: {
          DEFAULT: "var(--accent)",
          soft: "var(--accent-soft)",
        },
        status: {
          positive: { bg: "var(--status-positive-bg)", text: "var(--status-positive-text)" },
          warning: { bg: "var(--status-warning-bg)", text: "var(--status-warning-text)" },
          danger: { bg: "var(--status-danger-bg)", text: "var(--status-danger-text)" },
          neutral: { bg: "var(--status-neutral-bg)", text: "var(--status-neutral-text)" },
          info: { bg: "var(--status-info-bg)", text: "var(--status-info-text)" },
        },
      },
      fontFamily: {
        sans: ["var(--font-sans)"],
        mono: ["var(--font-mono)"],
      },
      fontSize: {
        label: ["12px", { lineHeight: "16px" }],
        "table-body": ["13px", { lineHeight: "18px" }],
        body: ["14px", { lineHeight: "20px" }],
        "card-title": ["15px", { lineHeight: "20px" }],
        "page-title": ["20px", { lineHeight: "26px" }],
      },
      borderRadius: {
        control: "var(--radius-control)",
        card: "var(--radius-card)",
      },
      transitionProperty: {
        chrome: "color, background-color, border-color",
      },
      transitionDuration: {
        DEFAULT: "150ms",
      },
      transitionTimingFunction: {
        DEFAULT: "ease-out",
      },
      keyframes: {
        "indeterminate-progress": {
          "0%": { transform: "translateX(-100%)" },
          "100%": { transform: "translateX(300%)" },
        },
      },
      animation: {
        "indeterminate-progress": "indeterminate-progress 1.2s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};
