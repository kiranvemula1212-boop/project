import { Skeleton } from "@/components/ui/Skeleton";
import type { ColumnDefinition } from "@/types/api";

interface TableSkeletonProps {
  columns: ColumnDefinition[];
}

// Real column headers, shimmer rows underneath — metadata resolves before data, so the
// user sees the report's shape immediately instead of a generic loading block.
export function TableSkeleton({ columns }: TableSkeletonProps) {
  return (
    <table className="w-full border-collapse">
      <thead>
        <tr className="border-b border-border">
          {columns.map((column) => (
            <th key={column.key} scope="col" className="px-3 py-2 text-left text-label font-medium text-ink-muted">
              {column.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {Array.from({ length: 8 }).map((_, rowIndex) => (
          <tr key={rowIndex} className="border-b border-border last:border-b-0">
            {columns.map((column) => (
              <td key={column.key} className="h-11 px-3">
                <Skeleton className="h-4 w-3/4" />
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
