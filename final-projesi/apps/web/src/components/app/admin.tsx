import { Link } from "@tanstack/react-router"
import {
  BarChart3,
  ClipboardList,
  LogOut,
  Package,
  ShieldCheck,
} from "lucide-react"
import type { ReactNode } from "react"

import type { BreadcrumbItem } from "@/components/app/page"
import { Breadcrumbs } from "@/components/app/page"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

type AdminSection = "dashboard" | "products" | "orders"

const adminNavItems = [
  { id: "dashboard", label: "Dashboard", to: "/admin", icon: BarChart3 },
  { id: "products", label: "Products", to: "/admin/products", icon: Package },
  { id: "orders", label: "Orders", to: "/admin/orders", icon: ClipboardList },
] as const

export function AdminShell({
  active,
  breadcrumbs,
  children,
  onLogout,
  title,
  username,
}: {
  active: AdminSection
  breadcrumbs?: Array<BreadcrumbItem>
  children: ReactNode
  onLogout: () => void
  title: string
  username: string
}) {
  const defaultBreadcrumbs: Array<BreadcrumbItem> = [
    { label: "Admin", href: "/admin" },
    ...(active === "dashboard"
      ? [{ label: "Dashboard" }]
      : [{ label: active === "products" ? "Products" : "Orders" }]),
  ]

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <header className="border-b border-neutral-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 lg:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-neutral-950 text-white">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-medium text-neutral-500">
                Merchant Admin
              </p>
              <h1 className="text-xl font-semibold">{title}</h1>
            </div>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="hidden text-neutral-500 sm:inline">
              {username}
            </span>
            <Button onClick={onLogout} type="button" variant="outline">
              <LogOut className="h-4 w-4" />
              <span>Log Out</span>
            </Button>
          </div>
        </div>
      </header>

      <section className="mx-auto max-w-7xl px-4 py-8 lg:px-6">
        <Breadcrumbs
          className="mb-4"
          items={breadcrumbs ?? defaultBreadcrumbs}
        />
        <AdminNav active={active} />
        {children}
      </section>
    </main>
  )
}

export function AdminNav({ active }: { active: AdminSection }) {
  return (
    <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <nav className="flex flex-wrap gap-2 text-sm font-semibold">
        {adminNavItems.map((item) => {
          const Icon = item.icon
          const isActive = item.id === active

          return (
            <Link
              className={cn(
                "inline-flex items-center gap-2 rounded-lg border px-3 py-2 transition",
                isActive
                  ? "border-neutral-950 bg-neutral-950 text-white"
                  : "border-neutral-200 bg-white text-neutral-800 hover:border-neutral-900"
              )}
              key={item.id}
              to={item.to}
            >
              <Icon className="h-4 w-4" />
              <span>{item.label}</span>
            </Link>
          )
        })}
      </nav>
    </div>
  )
}
