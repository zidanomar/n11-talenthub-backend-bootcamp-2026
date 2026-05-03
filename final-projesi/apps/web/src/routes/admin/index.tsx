import { Link, createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { ClipboardList, Package, WalletCards } from "lucide-react"

import { clearAuthSession, getAuthSession } from "@/lib/auth"
import { fetchAdminOrders } from "@/lib/orders"
import { fetchProducts, formatPrice } from "@/lib/products"
import {
  OrderRevenuePieChart,
  OrderStatusBarChart,
} from "@/components/admin-charts"
import { AdminShell } from "@/components/app/admin"
import { DataTable } from "@/components/app/page"
import { OrderStatusBadge } from "@/components/domain/order-status"
import { MetricTile, StockBadge } from "@/components/domain/product"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/admin/")({
  loader: async ({ location }) => {
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

    const [productsPage, ordersPage] = await Promise.all([
      fetchProducts({ limit: 8, page: 0, sort: "newest" }),
      fetchAdminOrders({ page: 0, size: 8 }),
    ])

    return { auth, ordersPage, productsPage }
  },
  component: AdminDashboardPage,
})

function AdminDashboardPage() {
  const navigate = useNavigate()
  const { auth, ordersPage, productsPage } = Route.useLoaderData()
  const lowStockProducts = productsPage.content.filter(
    (product) => product.stock <= 10,
  )
  const sumByStatus = (statuses: ReadonlyArray<string>) =>
    ordersPage.content
      .filter((order) => statuses.includes(order.status))
      .reduce((total, order) => total + order.totalPrice, 0)

  const revenue = sumByStatus(["COMPLETED"])
  const grossRevenue = sumByStatus([
    "PENDING",
    "PAID",
    "SHIPPED",
    "DELIVERED",
    "RETURNING",
    "RETURN_SHIPPED",
    "REFUNDING",
  ])
  const refund = sumByStatus(["RETURNED"])

  async function handleLogout() {
    await clearAuthSession()
    await navigate({
      to: "/login",
      search: { mode: "signin", redirect: "/admin" },
    })
  }

  return (
    <AdminShell
      active="dashboard"
      onLogout={handleLogout}
      title="Dashboard"
      username={auth.username ?? "Merchant"}
    >
      <div className="grid w-full gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <MetricTile
          icon={<Package className="h-5 w-5" />}
          label="Products"
          value={productsPage.totalElements.toLocaleString("en-US")}
        />
        <MetricTile
          icon={<ClipboardList className="h-5 w-5" />}
          label="Orders"
          value={ordersPage.totalElements.toLocaleString("en-US")}
        />
        <MetricTile
          icon={<WalletCards className="h-5 w-5 text-emerald-600" />}
          label="Net Revenue"
          value={formatPrice(revenue)}
        />
        <MetricTile
          icon={<WalletCards className="h-5 w-5 text-brand" />}
          label="Gross Revenue"
          value={formatPrice(grossRevenue)}
        />
        <MetricTile
          icon={<WalletCards className="h-5 w-5 text-neutral-900" />}
          label="Refund"
          value={formatPrice(refund)}
        />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <OrderStatusBarChart orders={ordersPage.content} />
        <OrderRevenuePieChart orders={ordersPage.content} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
        <section className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          <div className="flex items-center justify-between border-b border-neutral-200 px-5 py-4">
            <div>
              <h2 className="font-semibold">Recent Orders</h2>
              <p className="text-sm text-neutral-500">
                Latest activity across customer orders
              </p>
            </div>
            <Button asChild size="sm" variant="outline">
              <Link to="/admin/orders">View All</Link>
            </Button>
          </div>
          <DataTable className="rounded-none border-0" minWidth="640px">
            <thead className="bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase">
              <tr>
                <th className="px-5 py-3">Order</th>
                <th className="px-5 py-3">Customer</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3 text-right">Total</th>
              </tr>
            </thead>
            <tbody>
              {ordersPage.content.map((order) => (
                <tr className="border-t border-neutral-100" key={order.id}>
                  <td className="px-5 py-4 font-semibold">#{order.id}</td>
                  <td className="px-5 py-4 text-neutral-600">
                    {order.userId}
                  </td>
                  <td className="px-5 py-4">
                    <OrderStatusBadge status={order.status} />
                  </td>
                  <td className="px-5 py-4 text-right font-semibold">
                    {formatPrice(order.totalPrice)}
                  </td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        </section>

        <aside className="rounded-2xl border border-neutral-200 bg-white">
          <div className="flex items-center justify-between border-b border-neutral-200 px-5 py-4">
            <div>
              <h2 className="font-semibold">Inventory Watch</h2>
              <p className="text-sm text-neutral-500">
                Recently added products at or below 10 stock
              </p>
            </div>
          </div>
          <div className="divide-y divide-neutral-100">
            {lowStockProducts.length > 0 ? (
              lowStockProducts.map((product) => (
                <Link
                  className="flex items-center justify-between gap-4 px-5 py-4 hover:bg-neutral-50"
                  key={product.id}
                  params={{ productId: String(product.id) }}
                  to="/admin/products/$productId"
                >
                  <div className="min-w-0">
                    <p className="truncate font-semibold">{product.name}</p>
                    <p className="text-sm text-neutral-500">
                      {product.category}
                    </p>
                  </div>
                  <StockBadge stock={product.stock} />
                </Link>
              ))
            ) : (
              <div className="px-5 py-10 text-sm text-neutral-500">
                No low-stock products in the latest inventory page.
              </div>
            )}
          </div>
        </aside>
      </div>
    </AdminShell>
  )
}
