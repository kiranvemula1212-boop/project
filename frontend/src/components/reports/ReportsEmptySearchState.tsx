import { Button } from "@/components/ui/Button";

interface ReportsEmptySearchStateProps {
  term: string;
  onClear: () => void;
}

// Only reachable via search — an empty screen should be an invitation to act, not a dead end.
export function ReportsEmptySearchState({ term, onClear }: ReportsEmptySearchStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-card border border-border bg-surface p-12 text-center">
      <p className="text-body text-ink">No reports match &apos;{term}&apos;.</p>
      <Button variant="secondary" onClick={onClear}>
        Clear search
      </Button>
    </div>
  );
}
