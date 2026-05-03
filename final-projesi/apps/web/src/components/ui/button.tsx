import * as React from "react"
import { cva } from "class-variance-authority"
import { Slot } from "radix-ui"
import type { VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "group/button inline-flex shrink-0 items-center justify-center rounded-lg border border-transparent bg-clip-padding font-semibold whitespace-nowrap transition-all outline-none select-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/30 active:not-aria-[haspopup]:translate-y-px disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-2 aria-invalid:ring-destructive/20 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-neutral-950 text-white hover:bg-neutral-800",
        outline:
          "border-neutral-200 bg-white text-neutral-800 hover:border-neutral-900 hover:text-neutral-950 aria-expanded:bg-neutral-100 aria-expanded:text-neutral-950",
        secondary:
          "bg-neutral-100 text-neutral-950 hover:bg-neutral-200 aria-expanded:bg-neutral-200",
        brand:
          "bg-brand text-brand-foreground hover:bg-brand-600",
        ghost:
          "text-neutral-700 hover:bg-neutral-100 hover:text-neutral-950 aria-expanded:bg-neutral-100 aria-expanded:text-neutral-950",
        destructive:
          "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 focus-visible:border-rose-300 focus-visible:ring-rose-200",
        link: "h-auto rounded-none p-0 text-brand-600 underline-offset-4 hover:underline",
      },
      size: {
        default:
          "h-10 gap-2 px-4 text-sm has-data-[icon=inline-end]:pr-3 has-data-[icon=inline-start]:pl-3",
        xs: "h-7 gap-1 rounded-md px-2 text-xs [&_svg:not([class*='size-'])]:size-3",
        sm: "h-9 gap-2 px-3 text-sm [&_svg:not([class*='size-'])]:size-4",
        lg: "h-12 gap-2 px-5 text-base [&_svg:not([class*='size-'])]:size-5",
        xl: "h-14 gap-2 rounded-xl px-6 text-base [&_svg:not([class*='size-'])]:size-5",
        icon: "size-10 [&_svg:not([class*='size-'])]:size-4",
        "icon-xs": "size-7 rounded-md [&_svg:not([class*='size-'])]:size-3",
        "icon-sm": "size-9 [&_svg:not([class*='size-'])]:size-4",
        "icon-lg": "size-12 rounded-xl [&_svg:not([class*='size-'])]:size-5",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  loading = false,
  className,
  variant = "default",
  size = "default",
  asChild = false,
  disabled,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
    loading?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      disabled={disabled || loading}
      {...props}
    />
  )
}

export { Button, buttonVariants }
