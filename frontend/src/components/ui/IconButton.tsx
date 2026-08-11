import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/cn";

export interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: ReactNode;
  "aria-label": string;
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { icon, className, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      type="button"
      className={cn(
        "inline-flex h-10 w-10 items-center justify-center rounded-control text-ink-muted transition-chrome duration-150 ease-out hover:bg-canvas hover:text-ink disabled:pointer-events-none disabled:opacity-50",
        className,
      )}
      {...props}
    >
      {icon}
    </button>
  );
});
