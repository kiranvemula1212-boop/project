import { Link } from "react-router-dom";
import { Button } from "@/components/ui/Button";

interface MessagePanelProps {
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
  linkAction?: { label: string; to: string };
}

// The one shared shape behind every empty/error state on the detail page: what happened,
// and an action right next to it. An empty screen is an invitation to act, not a dead end.
export function MessagePanel({ title, description, action, linkAction }: MessagePanelProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-card border border-border bg-surface p-12 text-center">
      <p className="text-body text-ink">{title}</p>
      {description && <p className="text-table-body text-ink-muted">{description}</p>}
      {action && (
        <Button variant="secondary" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
      {linkAction && (
        <Link to={linkAction.to} className="text-label text-accent underline">
          {linkAction.label}
        </Link>
      )}
    </div>
  );
}
