import { Button } from "@/components/ui/Button";

interface ReportsErrorPanelProps {
  onRetry: () => void;
}

// Says what failed and what to do — never "Oops", never an apology.
export function ReportsErrorPanel({ onRetry }: ReportsErrorPanelProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-card border border-border bg-surface p-12 text-center">
      <p className="text-body text-ink">Can&apos;t load reports. The server isn&apos;t responding.</p>
      <Button onClick={onRetry}>Try again</Button>
    </div>
  );
}
