import { useEffect, useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"

import type { Order } from "@/lib/orders"
import { clearAuthSession, getAuthSession } from "@/lib/auth"
import { fetchAdminOrders } from "@/lib/orders"
import { formatPrice } from "@/lib/products"
import { AdminShell } from "@/components/app/admin"
import {
  DataTable,
  DateRangeFilter,
  PaginationControls,
  TableFooter,
} from "@/components/app/page"
import { OrderStatusBadge } from "@/components/domain/order-status"
import {
  getLastSevenDaysRange,
  normalizeDateInput,
} from "@/lib/date-range"

type AdminOrdersSearch = {
  from?: string
  page?: number
  to?: string
}

function normalizePage(value: unknown) {
  const page =
    typeof value === "number"
      ? value
      : typeof value === "string"
        ? Number(value)
        : 1

  return Number.isInteger(page) && page > 0 ? page : 1
}

export const Route = createFileRoute("/admin/orders")({
  validateSearch: (search: Record<string, unknown>): AdminOrdersSearch => {
    const defaultRange = getLastSevenDaysRange()

    return {
      from: normalizeDateInput(search.from) ?? defaultRange.from,
      page: normalizePage(search.page),
      to: normalizeDateInput(search.to) ?? defaultRange.to,
    }
  },
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, location }) => {
    const auth = await getAuthSession()

    if (!auth.isAuthenticated) {
      throw redirect({
        to: "/login",
        search: { mode: "signin", redirect: location.href },
      })
    }

    if (auth.role !== "MERCHANT") {
      throw redirect({ to: "/" })
    }

    const ordersPage = await fetchAdminOrders({
      from: deps.from,
      page: (deps.page ?? 1) - 1,
      size: 12,
      to: deps.to,
    })

    return { auth, ordersPage }
  },
  component: AdminOrdersPage,
})

function AdminOrdersPage() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const { auth, ordersPage } = Route.useLoaderData()
  const [fromInput, setFromInput] = useState(search.from ?? "")
  const [toInput, setToInput] = useState(search.to ?? "")

  const currentPage = ordersPage.number + 1
  const totalPages = Math.max(ordersPage.totalPages, 1)

  useEffect(() => {
    setFromInput(search.from ?? "")
  }, [search.from])

  useEffect(() => {
    setToInput(search.to ?? "")
  }, [search.to])

  async function handleLogout() {
    await clearAuthSession()
    await navigate({
      to: "/login",
      search: { mode: "signin", redirect: "/admin/orders" },
    })
  }

  function updateSearch(nextSearch: AdminOrdersSearch) {
    void navigate({
      to: "/admin/orders",
      search: { ...search, ...nextSearch },
    })
  }

  function goToPage(page: number) {
    updateSearch({ page })
  }

  function applyDateFilter() {
    updateSearch({
      from: normalizeDateInput(fromInput),
      page: 1,
      to: normalizeDateInput(toInput),
    })
  }

  function resetDateFilter() {
    const defaultRange = getLastSevenDaysRange()
    setFromInput(defaultRange.from)
    setToInput(defaultRange.to)
    updateSearch({
      from: defaultRange.from,
      page: 1,
      to: defaultRange.to,
    })
  }

  return (
    <AdminShell
      active="orders"
      onLogout={handleLogout}
      title="Order Operations"
      username={auth.username ?? "Merchant"}
    >
      <DateRangeFilter
        from={fromInput}
        onApply={applyDateFilter}
        onFromChange={setFromInput}
        onReset={resetDateFilter}
        onToChange={setToInput}
        to={toInput}
      />

      <DataTable
        footer={
          <TableFooter>
            <PaginationControls
              currentPage={currentPage}
              itemLabel="orders"
              onPageChange={goToPage}
              totalItems={ordersPage.totalElements}
              totalPages={totalPages}
            />
          </TableFooter>
        }
        minWidth="760px"
      >
        <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase">
          <tr>
            <th className="px-5 py-4">Order</th>
            <th className="px-5 py-4">Customer</th>
            <th className="px-5 py-4">Items</th>
            <th className="px-5 py-4">Total</th>
            <th className="px-5 py-4">Placed</th>
            <th className="px-5 py-4">Status</th>
          </tr>
        </thead>
        <tbody>
          {ordersPage.content.map((order: Order) => (
            <tr
              className="border-b border-neutral-100 last:border-0"
              key={order.id}
            >
              <td className="px-5 py-4 font-semibold">#{order.id}</td>
              <td className="px-5 py-4 text-neutral-600">{order.userId}</td>
              <td className="px-5 py-4 text-neutral-600">
                {order.items.reduce((total, item) => total + item.quantity, 0)}
              </td>
              <td className="px-5 py-4 font-semibold">
                {formatPrice(order.totalPrice)}
              </td>
              <td className="px-5 py-4 text-neutral-600">
                {new Date(order.createdAt).toLocaleString()}
              </td>
              <td className="px-5 py-4">
                <OrderStatusBadge status={order.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </AdminShell>
  )
}
