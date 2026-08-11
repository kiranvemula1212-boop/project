import { forwardRef, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export type SelectProps = SelectHTMLAttributes<HTMLSelectElement>;

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, children, ...props },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        "h-10 rounded-control border border-border bg-surface px-3 text-body text-ink transition-chrome duration-150 ease-out hover:border-border-strong",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
});
