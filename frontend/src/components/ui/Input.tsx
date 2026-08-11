import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/cn";

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "size"> {
  leadingIcon?: ReactNode;
  onClear?: () => void;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { leadingIcon, onClear, className, value, ...props },
  ref,
) {
  const showClear = Boolean(onClear) && typeof value === "string" && value.length > 0;

  return (
    <div className={cn("relative flex items-center", className)}>
      {leadingIcon && (
        <span className="pointer-events-none absolute left-3 flex items-center text-ink-faint">{leadingIcon}</span>
      )}
      <input
        ref={ref}
        value={value}
        className={cn(
          "h-10 w-full rounded-control border border-border bg-surface text-body text-ink placeholder:text-ink-faint transition-chrome duration-150 ease-out",
          "hover:border-border-strong focus-visible:border-border-strong",
          leadingIcon ? "pl-9" : "pl-3",
          showClear ? "pr-9" : "pr-3",
        )}
        {...props}
      />
      {showClear && (
        <button
          type="button"
          onClick={onClear}
          aria-label="Clear"
          className="absolute right-2 flex h-6 w-6 items-center justify-center rounded-control text-ink-faint transition-chrome duration-150 ease-out hover:text-ink-muted"
        >
          ×
        </button>
      )}
    </div>
  );
});
