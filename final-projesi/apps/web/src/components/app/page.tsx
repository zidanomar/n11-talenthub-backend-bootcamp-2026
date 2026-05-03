import { CalendarDays, ChevronLeft, ChevronRight, X } from "lucide-react"
import { Fragment } from "react"
import type { ReactNode } from "react"

import { Button } from "@/components/ui/button"
import {
  Breadcrumb,
  BreadcrumbItem as ShadcnBreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

export type BreadcrumbItem = {
  label: string
  href?: string
}

type PageShellProps = {
  children: ReactNode
  className?: string
  maxWidth?: "5xl" | "7xl"
}

export function PageShell({
  children,
  className,
  maxWidth = "7xl",
}: PageShellProps) {
  return (
    <div
      className={cn(
        "mx-auto px-4 py-8 lg:px-6",
        maxWidth === "5xl" ? "max-w-5xl" : "max-w-7xl",
        className
      )}
    >
      {children}
    </div>
  )
}

export function PageHeader({
  actions,
  breadcrumbs,
  children,
  className,
  description,
  eyebrow,
  title,
}: {
  title: string
  actions?: ReactNode
  breadcrumbs?: Array<BreadcrumbItem>
  children?: ReactNode
  className?: string
  description?: ReactNode
  eyebrow?: string
}) {
  return (
    <div
      className={cn(
        "mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between",
        className
      )}
    >
      <div>
        {breadcrumbs ? (
          <Breadcrumbs className="mb-3" items={breadcrumbs} />
        ) : null}
        {eyebrow ? <p className="text-sm text-neutral-500">{eyebrow}</p> : null}
        <h1 className="mt-1 text-3xl font-semibold tracking-normal">{title}</h1>
        {description ? (
          <p className="mt-2 text-sm text-neutral-500">{description}</p>
        ) : null}
        {children}
      </div>
      {actions ? (
        <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>
      ) : null}
    </div>
  )
}

export function Breadcrumbs({
  className,
  items,
}: {
  items: Array<BreadcrumbItem>
  className?: string
}) {
  return (
    <Breadcrumb className={className}>
      <BreadcrumbList>
        {items.map((item, index) => {
          const isLast = index === items.length - 1

          return (
            <Fragment key={`${item.label}-${index}`}>
              {index > 0 ? <BreadcrumbSeparator /> : null}
              <ShadcnBreadcrumbItem>
                {item.href && !isLast ? (
                  <BreadcrumbLink className="font-medium" href={item.href}>
                    {item.label}
                  </BreadcrumbLink>
                ) : (
                  <BreadcrumbPage>{item.label}</BreadcrumbPage>
                )}
              </ShadcnBreadcrumbItem>
            </Fragment>
          )
        })}
      </BreadcrumbList>
    </Breadcrumb>
  )
}

export function Panel({
  children,
  className,
  ...props
}: React.ComponentProps<"section">) {
  return (
    <section
      className={cn(
        "rounded-2xl border border-neutral-200 bg-white",
        className
      )}
      {...props}
    >
      {children}
    </section>
  )
}

export function AlertMessage({
  children,
  className,
  tone = "danger",
}: {
  children: ReactNode
  className?: string
  tone?: "danger" | "info" | "success" | "warning"
}) {
  const toneClassName = {
    danger: "border-rose-200 bg-rose-50 text-rose-700",
    info: "border-sky-200 bg-sky-50 text-sky-700",
    success: "border-emerald-200 bg-emerald-50 text-emerald-700",
    warning: "border-amber-200 bg-amber-50 text-amber-800",
  }[tone]

  return (
    <div
      className={cn(
        "rounded-lg border px-4 py-3 text-sm",
        toneClassName,
        className
      )}
    >
      {children}
    </div>
  )
}

export function EmptyState({
  action,
  className,
  description,
  icon: Icon,
  title,
}: {
  description: ReactNode
  title: string
  action?: ReactNode
  className?: string
  icon: React.ComponentType<{ className?: string }>
}) {
  return (
    <section
      className={cn(
        "rounded-2xl border border-dashed border-neutral-300 bg-white px-6 py-14 text-center",
        className
      )}
    >
      <Icon className="mx-auto h-10 w-10 text-neutral-400" />
      <h2 className="mt-4 text-xl font-semibold">{title}</h2>
      <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-neutral-500">
        {description}
      </p>
      {action ? <div className="mt-6">{action}</div> : null}
    </section>
  )
}

export function DataTable({
  children,
  className,
  footer,
  minWidth = "760px",
}: {
  children: ReactNode
  className?: string
  footer?: ReactNode
  minWidth?: string
}) {
  return (
    <Panel className={cn("overflow-hidden", className)}>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm" style={{ minWidth }}>
          {children}
        </table>
      </div>
      {footer}
    </Panel>
  )
}

export function TableFooter({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-3 border-t border-neutral-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between",
        className
      )}
    >
      {children}
    </div>
  )
}

export function PaginationControls({
  currentPage,
  itemLabel,
  onPageChange,
  totalItems,
  totalPages,
}: {
  currentPage: number
  totalPages: number
  itemLabel?: string
  totalItems?: number
  onPageChange: (page: number) => void
}) {
  const summary =
    totalItems === undefined
      ? `Page ${currentPage} of ${totalPages}`
      : `Page ${currentPage} of ${totalPages} · ${totalItems.toLocaleString("en-US")} ${itemLabel ?? "items"}`

  return (
    <>
      <p className="text-sm text-neutral-500">{summary}</p>
      <div className="flex items-center gap-2">
        <Button
          disabled={currentPage <= 1}
          onClick={() => onPageChange(currentPage - 1)}
          type="button"
          variant="outline"
        >
          <ChevronLeft className="h-4 w-4" />
          <span>Previous</span>
        </Button>
        <Button
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
          type="button"
          variant="outline"
        >
          <span>Next</span>
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </>
  )
}

export function DateRangeFilter({
  from,
  onApply,
  onFromChange,
  onReset,
  onToChange,
  to,
}: {
  from: string
  to: string
  onApply: () => void
  onFromChange: (value: string) => void
  onReset: () => void
  onToChange: (value: string) => void
}) {
  return (
    <form
      className="mb-4 flex flex-col gap-3 rounded-2xl border border-neutral-200 bg-white p-4 sm:flex-row sm:items-end"
      onSubmit={(event) => {
        event.preventDefault()
        onApply()
      }}
    >
      <div className="flex-1 space-y-1">
        <Label className="text-sm text-neutral-600">From</Label>
        <Input
          onChange={(event) => onFromChange(event.target.value)}
          type="date"
          value={from}
        />
      </div>
      <div className="flex-1 space-y-1">
        <Label className="text-sm text-neutral-600">To</Label>
        <Input
          onChange={(event) => onToChange(event.target.value)}
          type="date"
          value={to}
        />
      </div>
      <div className="flex gap-2">
        <Button type="submit">
          <CalendarDays className="h-4 w-4" />
          <span>Apply</span>
        </Button>
        <Button onClick={onReset} type="button" variant="outline">
          <X className="h-4 w-4" />
          <span>Reset</span>
        </Button>
      </div>
    </form>
  )
}

export function SummaryRow({
  label,
  large = false,
  value,
}: {
  label: string
  value: ReactNode
  large?: boolean
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-neutral-500">{label}</span>
      <span
        className={
          large ? "text-xl font-semibold" : "font-medium text-neutral-900"
        }
      >
        {value}
      </span>
    </div>
  )
}
