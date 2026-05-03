import * as React from "react"

import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-10 w-full min-w-0 rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-neutral-400 focus-visible:border-neutral-900 focus-visible:ring-2 focus-visible:ring-neutral-900/10 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-rose-300 aria-invalid:ring-2 aria-invalid:ring-rose-100",
        className
      )}
      {...props}
    />
  )
}

export { Input }
