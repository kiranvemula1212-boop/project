const formatter = new Intl.DateTimeFormat("en-US", { dateStyle: "medium" });

export function formatDate(isoDate: string): string {
  // `new Date("2026-08-10")` parses a date-only string as UTC midnight; formatting that
  // in a timezone behind UTC (most of the Americas) rolls it back to the previous day.
  // Parsing the components and constructing a local-time Date avoids that shift.
  const [year, month, day] = isoDate.split("-").map(Number);
  return formatter.format(new Date(year!, month! - 1, day!));
}
