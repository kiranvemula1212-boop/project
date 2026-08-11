import { Component, type ReactNode } from "react";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  override render() {
    if (this.state.error) {
      return (
        <div className="flex min-h-screen items-center justify-center p-8">
          <div className="text-center">
            <h1 className="text-lg font-semibold">Something went wrong.</h1>
            <p className="mt-1 text-sm text-gray-500">Reload the page to try again.</p>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
