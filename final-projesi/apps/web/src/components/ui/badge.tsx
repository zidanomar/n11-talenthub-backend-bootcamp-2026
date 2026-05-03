import * as React from "react"
import { cva } from "class-variance-authority"
import { Slot } from "radix-ui"
import type { VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "group/badge inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden rounded-full border px-2.5 py-1 text-xs font-semibold whitespace-nowrap transition-all focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3.5",
  {
    variants: {
      variant: {
        default: "border-neutral-200 bg-neutral-100 text-neutral-700",
        neutral: "border-neutral-200 bg-neutral-100 text-neutral-700",
        secondary:
          "border-neutral-200 bg-white text-neutral-700",
        success: "border-emerald-200 bg-emerald-50 text-emerald-700",
        warning: "border-amber-200 bg-amber-50 text-amber-800",
        danger: "border-rose-200 bg-rose-50 text-rose-700",
        info: "border-sky-200 bg-sky-50 text-sky-700",
        brand: "border-brand-200 bg-brand-50 text-brand-600",
        violet: "border-violet-200 bg-violet-50 text-violet-700",
        orange: "border-orange-200 bg-orange-50 text-orange-700",
        cyan: "border-cyan-200 bg-cyan-50 text-cyan-700",
        indigo: "border-indigo-200 bg-indigo-50 text-indigo-700",
        teal: "border-teal-200 bg-teal-50 text-teal-700",
        destructive:
          "border-rose-200 bg-rose-50 text-rose-700 focus-visible:ring-rose-200",
        outline:
          "border-neutral-200 bg-white text-neutral-700",
        ghost: "border-transparent bg-transparent text-neutral-600",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        sm: "px-2 py-0.5 text-[11px]",
        md: "px-2.5 py-1 text-xs",
        lg: "px-3 py-1.5 text-sm",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "md",
    },
  }
)

function Badge({
  className,
  variant = "default",
  size = "md",
  asChild = false,
  ...props
}: React.ComponentProps<"span"> &
  VariantProps<typeof badgeVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot.Root : "span"

  return (
    <Comp
      data-slot="badge"
      data-variant={variant}
      className={cn(badgeVariants({ variant, size }), className)}
      {...props}
    />
  )
}

export { Badge, badgeVariants }
