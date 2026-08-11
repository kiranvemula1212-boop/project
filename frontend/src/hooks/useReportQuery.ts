import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import type { SortDirection, SortSpec } from "@/types/api";

const FILTER_PREFIX = "filter.";

export interface UseReportQueryResult {
  page: number;
  size: number;
  sorts: SortSpec[];
  filters: Record<string, string[]>;
  search: string;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  toggleSort: (columnKey: string) => void;
  removeSort: (columnKey: string) => void;
  setFilter: (columnKey: string, values: string[]) => void;
  clearFilter: (columnKey: string) => void;
  setSearch: (value: string) => void;
  clearAll: () => void;
}

/**
 * Reads and writes report query state from the URL's search params — the single source
 * of truth for page/sort/filter/search. This is what makes a filtered view shareable, the
 * back button behave correctly, and a refresh preserve position: there is exactly one
 * place this state can live, so it cannot desync from what the URL shows.
 */
export function useReportQuery(defaultSize: number): UseReportQueryResult {
  const [searchParams, setSearchParams] = useSearchParams();

  const page = parseNonNegativeInt(searchParams.get("page")) ?? 0;
  const size = parseNonNegativeInt(searchParams.get("size")) ?? defaultSize;
  const search = searchParams.get("search") ?? "";

  const sorts = useMemo<SortSpec[]>(
    () =>
      searchParams
        .getAll("sort")
        .map(parseSortParam)
        .filter((spec): spec is SortSpec => spec !== null),
    [searchParams],
  );

  const filters = useMemo<Record<string, string[]>>(() => {
    const result: Record<string, string[]> = {};
    for (const key of searchParams.keys()) {
      if (!key.startsWith(FILTER_PREFIX)) {
        continue;
      }
      const columnKey = key.slice(FILTER_PREFIX.length);
      const raw = searchParams.get(key) ?? "";
      // Each value was percent-encoded before joining (see setFilter) specifically so a
      // value containing a literal comma — e.g. "Austin, TX" — can't be mistaken for two
      // separate values when split back apart.
      result[columnKey] = raw
        .split(",")
        .filter((v) => v !== "")
        .map(decodeURIComponent);
    }
    return result;
  }, [searchParams]);

  // Any change to sort, filter, or search resets page to 0 — otherwise the user can be
  // left on page 5 of what just became a 2-page result, looking at an empty table.
  const mutateAndResetPage = useCallback(
    (mutate: (params: URLSearchParams) => void) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        mutate(next);
        next.set("page", "0");
        return next;
      });
    },
    [setSearchParams],
  );

  const setPage = useCallback(
    (nextPage: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set("page", String(nextPage));
        return next;
      });
    },
    [setSearchParams],
  );

  const setSize = useCallback(
    (nextSize: number) => {
      // A page-size change invalidates the current page position just like a filter
      // change does, so it resets to page 0 too.
      mutateAndResetPage((params) => {
        params.set("size", String(nextSize));
      });
    },
    [mutateAndResetPage],
  );

  const toggleSort = useCallback(
    (columnKey: string) => {
      mutateAndResetPage((params) => {
        const existing = params.getAll("sort");
        const current = existing.find((raw) => raw.split(",")[0] === columnKey);
        const currentDirection = current?.split(",")[1];
        const others = existing.filter((raw) => raw.split(",")[0] !== columnKey);

        params.delete("sort");
        for (const raw of others) {
          params.append("sort", raw);
        }
        // Cycle asc -> desc -> none on repeated clicks of the same column; other active
        // sorts are left untouched, so multiple columns can be sorted at once.
        if (currentDirection === undefined) {
          params.append("sort", `${columnKey},asc`);
        } else if (currentDirection === "asc") {
          params.append("sort", `${columnKey},desc`);
        }
      });
    },
    [mutateAndResetPage],
  );

  // Unlike toggleSort, this always removes the column's sort outright rather than
  // cycling — what a chip's "x" button needs, not what clicking a header needs.
  const removeSort = useCallback(
    (columnKey: string) => {
      mutateAndResetPage((params) => {
        const remaining = params.getAll("sort").filter((raw) => raw.split(",")[0] !== columnKey);
        params.delete("sort");
        for (const raw of remaining) {
          params.append("sort", raw);
        }
      });
    },
    [mutateAndResetPage],
  );

  const setFilter = useCallback(
    (columnKey: string, values: string[]) => {
      mutateAndResetPage((params) => {
        if (values.length === 0) {
          params.delete(`${FILTER_PREFIX}${columnKey}`);
        } else {
          params.set(`${FILTER_PREFIX}${columnKey}`, values.map(encodeURIComponent).join(","));
        }
      });
    },
    [mutateAndResetPage],
  );

  const clearFilter = useCallback(
    (columnKey: string) => {
      mutateAndResetPage((params) => {
        params.delete(`${FILTER_PREFIX}${columnKey}`);
      });
    },
    [mutateAndResetPage],
  );

  const setSearch = useCallback(
    (value: string) => {
      mutateAndResetPage((params) => {
        if (value.trim() === "") {
          params.delete("search");
        } else {
          params.set("search", value);
        }
      });
    },
    [mutateAndResetPage],
  );

  const clearAll = useCallback(() => {
    setSearchParams((prev) => {
      const sizeValue = prev.get("size");
      const next = new URLSearchParams();
      if (sizeValue) {
        next.set("size", sizeValue);
      }
      next.set("page", "0");
      return next;
    });
  }, [setSearchParams]);

  return {
    page,
    size,
    sorts,
    filters,
    search,
    setPage,
    setSize,
    toggleSort,
    removeSort,
    setFilter,
    clearFilter,
    setSearch,
    clearAll,
  };
}

function parseNonNegativeInt(raw: string | null): number | undefined {
  if (raw === null) {
    return undefined;
  }
  const value = Number.parseInt(raw, 10);
  return Number.isNaN(value) || value < 0 ? undefined : value;
}

function parseSortParam(raw: string): SortSpec | null {
  const [columnKey, rawDirection] = raw.split(",");
  if (!columnKey) {
    return null;
  }
  const direction: SortDirection = rawDirection === "desc" ? "desc" : "asc";
  return { columnKey, direction };
}
