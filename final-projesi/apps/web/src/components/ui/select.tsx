import * as React from "react"

import { cn } from "@/lib/utils"

function Select({ className, ...props }: React.ComponentProps<"select">) {
  return (
    <select
      data-slot="select"
      className={cn(
        "h-10 w-full min-w-0 rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm transition-colors outline-none focus-visible:border-neutral-900 focus-visible:ring-2 focus-visible:ring-neutral-900/10 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-rose-300 aria-invalid:ring-2 aria-invalid:ring-rose-100",
        className,
      )}
      {...props}
    />
  )
}

export { Select }
