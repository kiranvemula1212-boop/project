import type { ProblemDetail } from "@/types/api";

/**
 * Distinguishes a server-returned error (4xx/5xx, with a problem+json body) from a
 * network failure (server unreachable) — the UI needs to say something different for
 * each ("that filter isn't valid" vs "can't reach the server").
 */
export type ApiErrorKind = "network" | "http";

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  readonly title?: string;
  readonly detail?: string;
  readonly errors: string[];

  constructor(params: {
    kind: ApiErrorKind;
    message: string;
    status?: number;
    title?: string;
    detail?: string;
    errors?: string[];
  }) {
    super(params.message);
    this.name = "ApiError";
    this.kind = params.kind;
    this.status = params.status;
    this.title = params.title;
    this.detail = params.detail;
    this.errors = params.errors ?? [];
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, init);
  } catch {
    throw new ApiError({ kind: "network", message: "Could not reach the server." });
  }

  if (!response.ok) {
    const problem = await parseProblemDetail(response);
    throw new ApiError({
      kind: "http",
      message: problem.detail ?? problem.title ?? `Request failed with status ${response.status}.`,
      status: response.status,
      title: problem.title,
      detail: problem.detail,
      errors: problem.errors,
    });
  }

  return (await response.json()) as T; // API boundary: trusting the server's declared contract.
}

async function parseProblemDetail(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail; // API boundary
  } catch {
    return { status: response.status };
  }
}
