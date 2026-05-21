import {
  Alert02Icon,
  CheckmarkCircle02Icon,
  InformationCircleIcon,
  Loading03Icon,
  MultiplicationSignCircleIcon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Toaster as Sonner, type ToasterProps } from "sonner";

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      className="toaster group"
      icons={{
        success: (
          <HugeiconsIcon
            icon={CheckmarkCircle02Icon}
            strokeWidth={2}
            className="size-4 shrink-0"
          />
        ),
        info: (
          <HugeiconsIcon
            icon={InformationCircleIcon}
            strokeWidth={2}
            className="size-4 shrink-0"
          />
        ),
        warning: (
          <HugeiconsIcon
            icon={Alert02Icon}
            strokeWidth={2}
            className="size-4 shrink-0"
          />
        ),
        error: (
          <HugeiconsIcon
            icon={MultiplicationSignCircleIcon}
            strokeWidth={2}
            className="size-4 shrink-0"
          />
        ),
        loading: (
          <HugeiconsIcon
            icon={Loading03Icon}
            strokeWidth={2}
            className="size-4 shrink-0 animate-spin"
          />
        ),
      }}
      closeButton
      toastOptions={{
        classNames: {
          toast: [
            "cn-toast",
            "!relative",
            // Light mode: white card, dark border, dark text
            "!bg-white !text-slate-900",
            "!border !border-slate-200",
            "!shadow-lg !shadow-slate-200/60",
            // Dark mode: deep charcoal, subtle border, light text
            "dark:!bg-[#1e1e1e] dark:!text-[#e4e6eb]",
            "dark:!border-[#2a2a2a]",
            "dark:!shadow-black/40",
            "!rounded-2xl",
            "!pr-9",
          ].join(" "),
          icon: "!text-slate-700 dark:!text-slate-300",
          title: "!text-slate-900 dark:!text-[#e4e6eb] !font-semibold !text-sm",
          description: "!text-slate-500 dark:!text-neutral-400 !text-xs",
          closeButton: [
            // position: absolute, top-right inside the toast frame
            "!left-auto !right-1 !top-3",
            "!translate-x-0 !translate-y-0",
            // appearance — adaptive to mode
            "!bg-transparent hover:!bg-slate-100 dark:hover:!bg-white/10",
            "!border-0",
            "!text-slate-400 hover:!text-slate-700",
            "dark:!text-slate-400 dark:hover:!text-white",
            // bigger X
            "!rounded-lg !p-1 !w-6 !h-6",
            "transition-colors",
          ].join(" "),
        },
      }}
      {...props}
    />
  );
};

export { Toaster };
