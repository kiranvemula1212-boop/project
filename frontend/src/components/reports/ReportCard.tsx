import { Link } from "react-router-dom";
import type { ReportSummary } from "@/types/api";

interface ReportCardProps {
  report: ReportSummary;
}

// A real <a> (via Link), not a "View" button inside the card — the whole card is the
// click target, and middle-click / cmd-click open a new tab like any other link.
export function ReportCard({ report }: ReportCardProps) {
  return (
    <Link
      to={`/reports/${report.id}`}
      className="block rounded-card border border-border bg-surface p-4 transition-chrome duration-150 ease-out hover:border-border-strong"
    >
      <h3 className="text-card-title font-medium text-ink">{report.name}</h3>
      <p className="mt-1 line-clamp-2 text-table-body text-ink-muted">{report.description}</p>
      <div className="mt-3 font-mono text-label text-ink-faint">{report.columnCount} columns</div>
    </Link>
  );
}
