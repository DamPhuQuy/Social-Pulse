/**
 * EmptyState component - Displays empty state UI
 *
 * Shows an icon, title, description, and optional action button
 * when there's no content to display.
 */

import type { LucideIcon } from "lucide-react";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      <div className="rounded-full bg-slate-100 dark:bg-neutral-800 p-6 mb-4">
        <Icon className="w-12 h-12 text-slate-400 dark:text-neutral-500" />
      </div>
      <h3 className="text-lg font-semibold text-slate-900 dark:text-neutral-100 mb-2">
        {title}
      </h3>
      <p className="text-sm text-slate-500 dark:text-neutral-400 max-w-sm mb-6">
        {description}
      </p>
      {action && (
        <button
          onClick={action.onClick}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
