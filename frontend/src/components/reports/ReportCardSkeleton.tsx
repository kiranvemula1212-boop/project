import { Skeleton } from "@/components/ui/Skeleton";

// Mirrors ReportCard's padding and line structure exactly, so the loading grid occupies
// the same space as the loaded grid and nothing shifts when data arrives.
export function ReportCardSkeleton() {
  return (
    <div className="rounded-card border border-border bg-surface p-4">
      <Skeleton className="h-5 w-2/3" />
      <Skeleton className="mt-2 h-[18px] w-full" />
      <Skeleton className="mt-1 h-[18px] w-3/4" />
      <Skeleton className="mt-3 h-4 w-20" />
    </div>
  );
}
