import type { ComponentType } from "react";
import type { ColumnDefinition, FilterType } from "@/types/api";
import { EnumFilterControl } from "./EnumFilterControl";
import { TextFilterControl } from "./TextFilterControl";

export interface FilterControlProps {
  column: ColumnDefinition;
  values: string[];
  onChange: (values: string[]) => void;
}

// Mirrors the cell registry's shape and reasoning: keyed by FilterType, one entry per
// control, closed for a switch statement to grow in.
const FILTER_CONTROLS: Partial<Record<FilterType, ComponentType<FilterControlProps>>> = {
  ENUM: EnumFilterControl,
  TEXT: TextFilterControl,
};

export function getFilterControl(type: FilterType): ComponentType<FilterControlProps> | null {
  return FILTER_CONTROLS[type] ?? null;
}
