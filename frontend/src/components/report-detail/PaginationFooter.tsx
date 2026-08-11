import { Button } from "@/components/ui/Button";
import { Select } from "@/components/ui/Select";

interface PaginationFooterProps {
  page: number;
  size: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

export function PaginationFooter({
  page,
  size,
  totalPages,
  hasNext,
  hasPrevious,
  onPageChange,
  onSizeChange,
}: PaginationFooterProps) {
  // Hide the whole footer when there's only one page — dead prev/next buttons and a
  // page-size select nobody needs are worse than no footer at all.
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
      <span className="text-table-body text-ink-muted">
        Page {page + 1} of {totalPages}
      </span>
      <div className="flex items-center gap-2">
        <Select
          value={size}
          onChange={(event) => onSizeChange(Number(event.target.value))}
          aria-label="Rows per page"
        >
          <option value={25}>25 / page</option>
          <option value={50}>50 / page</option>
          <option value={100}>100 / page</option>
        </Select>
        <Button variant="secondary" size="sm" onClick={() => onPageChange(page - 1)} disabled={!hasPrevious}>
          Previous
        </Button>
        <Button variant="secondary" size="sm" onClick={() => onPageChange(page + 1)} disabled={!hasNext}>
          Next
        </Button>
      </div>
    </div>
  );
}
