import type { ComponentProps } from "react";

/**
 * The event type for an HTML form's onSubmit handler.
 * Import this instead of re-declaring it in every page component.
 */
export type FormSubmitEvent = Parameters<
  NonNullable<ComponentProps<"form">["onSubmit"]>
>[0];
