import { useEffect, useMemo, useState } from "react"
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table"
import { Eye, ShoppingBag } from "lucide-react"
import type { ColumnDef } from "@tanstack/react-table"

import type { Order } from "@/lib/orders"
import {
  fetchOrders,
  isUnauthorizedError as isOrderUnauthorizedError,
} from "@/lib/orders"
import { formatPrice } from "@/lib/products"
import {
  DataTable,
  DateRangeFilter,
  EmptyState,
  PageHeader,
  PageShell,
  PaginationControls,
  TableFooter,
} from "@/components/app/page"
import {
  OrderStatusBadge,
  formatOrderStatus as formatOrderStatusLabel,
} from "@/components/domain/order-status"
import { Button } from "@/components/ui/button"
import { getLastSevenDaysRange, normalizeDateInput } from "@/lib/date-range"

type OrdersSearch = {
  from?: string
  page?: number
  size?: number
  to?: string
}

function normalizePositiveInteger(value: unknown, fallback: number) {
  const numberValue =
    typeof value === "number"
      ? value
      : typeof value === "string"
        ? Number(value)
        : fallback

  return Number.isInteger(numberValue) && numberValue > 0
    ? numberValue
    : fallback
}

export const Route = createFileRoute("/_authenticated/orders")({
  validateSearch: (search: Record<string, unknown>): OrdersSearch => {
    const defaultRange = getLastSevenDaysRange()

    return {
      from: normalizeDateInput(search.from) ?? defaultRange.from,
      page: normalizePositiveInteger(search.page, 1),
      size: normalizePositiveInteger(search.size, 6),
      to: normalizeDateInput(search.to) ?? defaultRange.to,
    }
  },
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, location }) => {
    try {
      const page = (deps.page ?? 1) - 1
      const size = deps.size ?? 6
      const ordersPage = await fetchOrders({
        from: deps.from,
        page,
        size,
        to: deps.to,
      })

      return { ordersPage }
    } catch (error) {
      if (isOrderUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: location.href },
        })
      }

      throw error
    }
  },
  component: OrdersPage,
})

function OrdersPage() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const { ordersPage } = Route.useLoaderData()
  const [fromInput, setFromInput] = useState(search.from ?? "")
  const [toInput, setToInput] = useState(search.to ?? "")
  const orders = ordersPage.content
  const currentPage = ordersPage.number + 1
  const totalPages = Math.max(ordersPage.totalPages, 1)

  useEffect(() => {
    setFromInput(search.from ?? "")
  }, [search.from])

  useEffect(() => {
    setToInput(search.to ?? "")
  }, [search.to])

  const columns = useMemo<Array<ColumnDef<Order>>>(
    () => [
      {
        accessorKey: "id",
        cell: ({ row }) => (
          <span className="font-semibold">#{row.original.id}</span>
        ),
        header: "Order",
      },
      {
        accessorKey: "createdAt",
        cell: ({ row }) => formatOrderDate(row.original.createdAt),
        header: "Date",
      },
      {
        accessorKey: "status",
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
        header: "Status",
      },
      {
        id: "items",
        cell: ({ row }) =>
          `${row.original.items.length} item${row.original.items.length === 1 ? "" : "s"}`,
        header: "Items",
      },
      {
        accessorKey: "totalPrice",
        cell: ({ row }) => (
          <span className="font-semibold">
            {formatPrice(row.original.totalPrice)}
          </span>
        ),
        header: "Total",
      },
      {
        id: "actions",
        cell: ({ row }) => (
          <Button asChild size="sm" variant="outline">
            <Link
              params={{ orderId: String(row.original.id) }}
              to="/orders/$orderId"
            >
              <Eye className="h-4 w-4" />
              <span>Details</span>
            </Link>
          </Button>
        ),
        header: "",
      },
    ],
    []
  )

  const table = useReactTable({
    columns,
    data: orders,
    getCoreRowModel: getCoreRowModel(),
  })

  function goToPage(page: number) {
    void navigate({
      to: "/orders",
      search: {
        ...search,
        page,
      },
    })
  }

  function updateOrdersSearch(nextSearch: OrdersSearch) {
    void navigate({
      to: "/orders",
      search: {
        ...search,
        ...nextSearch,
      },
    })
  }

  function applyDateFilter() {
    updateOrdersSearch({
      from: normalizeDateInput(fromInput),
      page: 1,
      to: normalizeDateInput(toInput),
    })
  }

  function resetDateFilter() {
    const defaultRange = getLastSevenDaysRange()
    setFromInput(defaultRange.from)
    setToInput(defaultRange.to)
    updateOrdersSearch({
      from: defaultRange.from,
      page: 1,
      to: defaultRange.to,
    })
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <PageShell>
        <PageHeader
          breadcrumbs={[
            { label: "Home", href: "/" },
            { label: "My Account", href: "/settings" },
            { label: "My Orders" },
          ]}
          description="Showing orders from the selected date range."
          eyebrow="My Account"
          title="My Orders"
        />

        <DateRangeFilter
          from={fromInput}
          onApply={applyDateFilter}
          onFromChange={setFromInput}
          onReset={resetDateFilter}
          onToChange={setToInput}
          to={toInput}
        />

        {ordersPage.totalElements === 0 ? (
          <EmptyState
            description="Try widening the date filter or place a new order."
            icon={ShoppingBag}
            title="No orders in this date range"
          />
        ) : (
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
          >
            <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase">
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th className="px-5 py-4" key={header.id}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {table.getRowModel().rows.map((row) => (
                <tr
                  className="border-b border-neutral-100 last:border-0"
                  key={row.id}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td className="px-5 py-4" key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </DataTable>
        )}
      </PageShell>
    </main>
  )
}

export function StatusBadge({ status }: { status: Order["status"] }) {
  return <OrderStatusBadge status={status} />
}

export function formatOrderStatus(status: Order["status"]) {
  return formatOrderStatusLabel(status)
}

export function formatOrderDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value))
}
