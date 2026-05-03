import { useId, useState } from "react"
import { ChevronDown } from "lucide-react"
import type { ReactNode } from "react"

import { Panel } from "@/components/app/page"
import { Button } from "@/components/ui/button"

export function FilterSection({
  children,
  defaultOpen = true,
  title,
}: {
  title: string
  children: ReactNode
  defaultOpen?: boolean
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen)
  const contentId = useId()

  return (
    <Panel className="overflow-hidden p-0">
      <Button
        aria-controls={contentId}
        aria-expanded={isOpen}
        className="h-auto w-full justify-between rounded-none px-4 py-4 text-left text-neutral-950"
        onClick={() => setIsOpen((current) => !current)}
        type="button"
        variant="ghost"
      >
        <span className="font-medium">{title}</span>
        <ChevronDown
          className={`h-4 w-4 text-neutral-500 transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </Button>
      {isOpen ? (
        <div id={contentId} className="px-4 pb-4">
          {children}
        </div>
      ) : null}
    </Panel>
  )
}
