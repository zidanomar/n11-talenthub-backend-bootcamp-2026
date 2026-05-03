import { Link } from "@tanstack/react-router"
import type { ComponentProps, ReactNode } from "react"

import { Badge } from "@/components/ui/badge"

export function StockBadge({ stock }: { stock: number }) {
  if (stock <= 0) {
    return <Badge variant="danger">Out of stock</Badge>
  }

  if (stock <= 10) {
    return <Badge variant="warning">{stock} left</Badge>
  }

  return <Badge variant="success">{stock} in stock</Badge>
}

export function MetricTile({
  icon,
  label,
  value,
}: {
  icon: ReactNode
  label: string
  value: string
}) {
  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="flex items-center justify-between gap-4">
        <p className="text-sm font-medium text-neutral-500">{label}</p>
        <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-neutral-100 text-neutral-600">
          {icon}
        </span>
      </div>
      <p className="mt-4 text-2xl font-semibold">{value}</p>
    </section>
  )
}

export function SettingsTile({
  icon,
  label,
  to,
}: {
  icon: ReactNode
  label: string
  to: ComponentProps<typeof Link>["to"]
}) {
  return (
    <Link
      className="group flex min-h-18 items-center justify-between rounded-2xl border border-neutral-200 bg-white px-5 py-4 text-neutral-900 transition hover:border-neutral-900 hover:shadow-sm"
      to={to}
    >
      <span className="flex min-w-0 items-center gap-3">
        {icon}
        <span className="truncate text-sm font-medium">{label}</span>
      </span>
    </Link>
  )
}
