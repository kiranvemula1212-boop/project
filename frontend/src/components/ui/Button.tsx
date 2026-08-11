import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type ButtonVariant = "primary" | "secondary" | "ghost";
type ButtonSize = "sm" | "md";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "border border-transparent bg-accent text-white hover:bg-accent/90",
  secondary: "border border-border bg-surface text-ink hover:border-border-strong",
  ghost: "border border-transparent bg-transparent text-ink-muted hover:bg-canvas",
};

// h-10 (40px) meets the minimum mobile touch target; "sm" only tightens up from the
// sm: breakpoint, where a mouse — not a thumb — is doing the pointing.
const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: "h-10 px-3 text-table-body sm:h-8",
  md: "h-10 px-4 text-body",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = "primary", size = "md", className, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-control font-medium transition-chrome duration-150 ease-out disabled:pointer-events-none disabled:opacity-50",
        VARIANT_CLASSES[variant],
        SIZE_CLASSES[size],
        className,
      )}
      {...props}
    />
  );
});
