import type { ReactNode } from "react";
import type { ColumnDefinition, ColumnType } from "@/types/api";
import { CurrencyCell } from "./CurrencyCell";
import { DateCell } from "./DateCell";
import { EmailCell } from "./EmailCell";
import { EnumCell } from "./EnumCell";
import { IdCell } from "./IdCell";
import { NumberCell } from "./NumberCell";
import { TextCell } from "./TextCell";

export type CellRenderer = (value: unknown, column: ColumnDefinition) => ReactNode;

// Keyed by ColumnType so adding a new type is one entry here, touching nothing else — a
// switch statement was rejected because it puts every future type in one growing
// function and invites unrelated logic to accumulate there.
const CELL_RENDERERS: Partial<Record<ColumnType, CellRenderer>> = {
  ID: IdCell,
  TEXT: TextCell,
  EMAIL: EmailCell,
  NUMBER: NumberCell,
  CURRENCY: CurrencyCell,
  DATE: DateCell,
  ENUM: EnumCell,
};

// Falls back to TextCell for a type the frontend doesn't recognize — a report the
// backend adds after this frontend was built must render as text, never crash.
export function getCellRenderer(type: ColumnType): CellRenderer {
  return CELL_RENDERERS[type] ?? TextCell;
}
