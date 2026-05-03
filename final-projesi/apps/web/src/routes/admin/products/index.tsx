import { useState } from "react"
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import { Edit, Package, Plus, Search } from "lucide-react"

import type { Product } from "@/lib/products"
import { clearAuthSession, getAuthSession } from "@/lib/auth"
import { fetchProducts, formatPrice, updateProductStock } from "@/lib/products"
import { AdminShell } from "@/components/app/admin"
import {
  AlertMessage,
  DataTable,
  PaginationControls,
  TableFooter,
} from "@/components/app/page"
import { StockBadge } from "@/components/domain/product"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

type AdminSearch = {
  q?: string
  page?: number
}

function normalizeString(value: unknown) {
  return typeof value === "string" && value.trim().length > 0
    ? value.trim()
    : undefined
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

export const Route = createFileRoute("/admin/products/")({
  validateSearch: (search: Record<string, unknown>): AdminSearch => ({
    page: normalizePage(search.page),
    q: normalizeString(search.q),
  }),
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

    const productsPage = await fetchProducts({
      limit: 12,
      page: (deps.page ?? 1) - 1,
      query: deps.q ?? "",
      sort: "newest",
    })

    return { auth, productsPage }
  },
  component: AdminPage,
})

function AdminPage() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const { auth, productsPage } = Route.useLoaderData()
  const [query, setQuery] = useState(search.q ?? "")
  const [stockValues, setStockValues] = useState<Record<number, string>>({})
  const [busyProductId, setBusyProductId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const currentPage = productsPage.number + 1
  const totalPages = Math.max(productsPage.totalPages, 1)

  async function handleLogout() {
    await clearAuthSession()
    await navigate({
      to: "/login",
      search: { mode: "signin", redirect: "/admin/products" },
    })
  }

  function submitSearch() {
    void navigate({
      to: "/admin/products",
      search: { page: 1, q: normalizeString(query) },
    })
  }

  function goToPage(page: number) {
    void navigate({
      to: "/admin/products",
      search: { ...search, page },
    })
  }

  async function saveStock(product: Product) {
    const nextStock = Number(stockValues[product.id] ?? product.stock)

    if (!Number.isInteger(nextStock) || nextStock < 0) {
      setErrorMessage("Stock must be a non-negative whole number.")
      return
    }

    setBusyProductId(product.id)
    setErrorMessage(null)

    try {
      await updateProductStock(product.id, nextStock)
      await navigate({ to: "/admin/products", search })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Stock could not be updated."
      )
    } finally {
      setBusyProductId(null)
    }
  }

  return (
    <AdminShell
      active="products"
      onLogout={handleLogout}
      title="Product Inventory"
      username={auth.username ?? "Merchant"}
    >
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-neutral-500">
          {productsPage.totalElements.toLocaleString("en-US")} products
        </p>
        <Button asChild>
          <Link to="/admin/products/new">
            <Plus className="h-4 w-4" />
            <span>Add Product</span>
          </Link>
        </Button>
        <form
          className="flex max-w-md min-w-0 flex-1 items-center gap-2 sm:ml-auto"
          onSubmit={(event) => {
            event.preventDefault()
            submitSearch()
          }}
        >
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-neutral-500" />
            <Input
              className="pl-9"
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search products"
              value={query}
            />
          </div>
          <Button type="submit">Search</Button>
        </form>
      </div>

      {errorMessage ? (
        <AlertMessage className="mb-4">{errorMessage}</AlertMessage>
      ) : null}

      <DataTable
        footer={
          <TableFooter>
            <PaginationControls
              currentPage={currentPage}
              itemLabel="products"
              onPageChange={goToPage}
              totalItems={productsPage.totalElements}
              totalPages={totalPages}
            />
          </TableFooter>
        }
      >
        <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase">
          <tr>
            <th className="px-5 py-4">Product</th>
            <th className="px-5 py-4">Category</th>
            <th className="px-5 py-4">Price</th>
            <th className="px-5 py-4">Inventory</th>
            <th className="px-5 py-4">Stock</th>
            <th className="px-5 py-4" />
          </tr>
        </thead>
        <tbody>
          {productsPage.content.map((product) => (
            <tr
              className="border-b border-neutral-100 last:border-0"
              key={product.id}
            >
              <td className="px-5 py-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-neutral-100">
                    <Package className="h-5 w-5 text-neutral-500" />
                  </div>
                  <div>
                    <p className="font-semibold">{product.name}</p>
                    <p className="text-xs text-neutral-500">{product.brand}</p>
                  </div>
                </div>
              </td>
              <td className="px-5 py-4 text-neutral-600">{product.category}</td>
              <td className="px-5 py-4 font-semibold">
                {formatPrice(product.price)}
              </td>
              <td className="px-5 py-4">
                <StockBadge stock={product.stock} />
              </td>
              <td className="px-5 py-4">
                <Input
                  className="w-24"
                  min={0}
                  onChange={(event) =>
                    setStockValues((current) => ({
                      ...current,
                      [product.id]: event.target.value,
                    }))
                  }
                  type="number"
                  value={
                    Object.hasOwn(stockValues, product.id)
                      ? stockValues[product.id]
                      : String(product.stock)
                  }
                />
              </td>
              <td className="px-5 py-4 text-right">
                <div className="flex justify-end gap-2">
                  <Button asChild variant="outline">
                    <Link
                      params={{ productId: String(product.id) }}
                      to="/admin/products/$productId"
                    >
                      <Edit className="h-4 w-4" />
                      <span>Edit</span>
                    </Link>
                  </Button>
                  <Button
                    disabled={busyProductId === product.id}
                    onClick={() => void saveStock(product)}
                    type="button"
                  >
                    {busyProductId === product.id ? "Saving" : "Save"}
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </AdminShell>
  )
}
