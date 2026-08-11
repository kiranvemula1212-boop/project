import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="p-8 text-center">
      <h1 className="text-xl font-semibold">Page not found</h1>
      <Link to="/" className="mt-2 inline-block text-sm text-blue-600 underline">
        Back to all reports
      </Link>
    </div>
  );
}
