import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { toast } from "sonner"
import {
  BadgePercent,
  ChevronDown,
  LoaderCircle,
  ShieldCheck,
  Truck,
} from "lucide-react"

import { clearAuthSession, getAuthSession } from "@/lib/auth"
import {
  fetchCart,
  incrementCartItem,
  isUnauthorizedError as isCartUnauthorizedError,
} from "@/lib/cart"
import { fetchProducts } from "@/lib/products"
import { fetchUserProfile, getCurrentAddressSummary } from "@/lib/users"
import { Breadcrumbs } from "@/components/app/page"
import { StorefrontHeader } from "@/components/storefront"
import { FilterSection } from "@/components/filter-section"
import { ProductCard } from "@/components/product-card"
import { cn } from "@/lib/utils"

const validSortModes = new Set([
  "relevance",
  "newest",
  "price-asc",
  "price-desc",
  "rating-desc",
])

type ProductSearch = {
  q?: string
  maxPrice?: number
  minRating?: number
  minPrice?: number
  sort?: string
}

function normalizeString(value: unknown) {
  return typeof value === "string" && value.trim().length > 0
    ? value.trim()
    : undefined
}

function normalizeRating(value: unknown) {
  const rating =
    typeof value === "number"
      ? value
      : typeof value === "string"
        ? Number(value)
        : 0

  return Number.isFinite(rating) && rating > 0 ? rating : undefined
}

function normalizePrice(value: unknown) {
  const price =
    typeof value === "number"
      ? value
      : typeof value === "string"
        ? Number(value)
        : undefined

  return typeof price === "number" && Number.isFinite(price) && price >= 0
    ? price
    : undefined
}

function normalizeSort(value: unknown) {
  return typeof value === "string" && validSortModes.has(value)
    ? value
    : undefined
}

function toProductOptions(search: ProductSearch) {
  return {
    category: "All",
    inStock: true,
    maxPrice: search.maxPrice,
    minRating: search.minRating ?? 0,
    minPrice: search.minPrice,
    query: search.q ?? "",
    sort: search.sort ?? "relevance",
  }
}

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>): ProductSearch => ({
    maxPrice: normalizePrice(search.maxPrice),
    minRating: normalizeRating(search.minRating),
    minPrice: normalizePrice(search.minPrice),
    q: normalizeString(search.q),
    sort: normalizeSort(search.sort),
  }),
  loaderDeps: ({ search }) => search,
  loader: async ({ deps }) => {
    const auth = await getAuthSession()

    if (auth.isAuthenticated && auth.role === "MERCHANT") {
      throw redirect({ to: "/admin" })
    }

    const productsPage = await fetchProducts(toProductOptions(deps))
    const [cart, profile] = auth.isAuthenticated
      ? await Promise.all([fetchCart(), fetchUserProfile()])
      : [null, null]

    return {
      cartItemCount:
        cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0,
      profile,
      productsPage,
    }
  },
  component: App,
})

function App() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const loaderData = Route.useLoaderData()
  const [products, setProducts] = useState(loaderData.productsPage.content)
  const [productsPage, setProductsPage] = useState(loaderData.productsPage)
  const [searchQuery, setSearchQuery] = useState(search.q ?? "")
  const [minPriceInput, setMinPriceInput] = useState(
    search.minPrice?.toString() ?? ""
  )
  const [maxPriceInput, setMaxPriceInput] = useState(
    search.maxPrice?.toString() ?? ""
  )
  const [pendingCartIds, setPendingCartIds] = useState<Array<number>>([])
  const [addedCartIds, setAddedCartIds] = useState<Array<number>>([])
  const [cartItemCount, setCartItemCount] = useState(loaderData.cartItemCount)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null)
  const loadMoreTargetRef = useRef<HTMLDivElement | null>(null)

  const hasMoreProducts = productsPage.number + 1 < productsPage.totalPages
  const minimumRating = search.minRating ?? 0
  const minPrice = search.minPrice
  const maxPrice = search.maxPrice
  const sortMode = search.sort ?? "relevance"
  const productQuery = useMemo(() => toProductOptions(search), [search])

  useEffect(() => {
    setProducts(loaderData.productsPage.content)
    setProductsPage(loaderData.productsPage)
  }, [loaderData.productsPage])

  useEffect(() => {
    setSearchQuery(search.q ?? "")
  }, [search.q])

  useEffect(() => {
    setMinPriceInput(search.minPrice?.toString() ?? "")
  }, [search.minPrice])

  useEffect(() => {
    setMaxPriceInput(search.maxPrice?.toString() ?? "")
  }, [search.maxPrice])

  function updateProductSearch(nextSearch: ProductSearch) {
    void navigate({
      to: "/",
      search: (current) => ({
        ...current,
        ...nextSearch,
      }),
    })
  }

  function handleSearchSubmit() {
    updateProductSearch({ q: normalizeString(searchQuery) })
  }

  function handlePriceSubmit() {
    updateProductSearch({
      maxPrice: normalizePrice(maxPriceInput),
      minPrice: normalizePrice(minPriceInput),
    })
  }

  async function handleLogout() {
    await clearAuthSession()
    await navigate({
      to: "/login",
      search: {
        mode: "signin",
        redirect: "/",
      },
    })
  }

  const loadProductsPage = useCallback(
    async (page: number, append = false) => {
      setIsLoadingMore(true)
      setLoadMoreError(null)

      try {
        const nextPage = await fetchProducts({
          ...productQuery,
          limit: productsPage.size,
          page,
        })

        setProductsPage(nextPage)
        setProducts((current) => {
          if (!append) {
            return nextPage.content
          }

          const existingIds = new Set(current.map((product) => product.id))
          const newProducts = nextPage.content.filter(
            (product) => !existingIds.has(product.id)
          )

          return [...current, ...newProducts]
        })
      } catch (error) {
        console.error(error)
        setLoadMoreError(
          error instanceof Error ? error.message : "Could not load products."
        )
      } finally {
        setIsLoadingMore(false)
      }
    },
    [productQuery, productsPage.size]
  )

  async function handleAddToCart(productId: number) {
    setPendingCartIds((current) => [...new Set([...current, productId])])

    try {
      await incrementCartItem(productId)
      setCartItemCount((current) => current + 1)
      setAddedCartIds((current) => [...new Set([...current, productId])])
      setTimeout(() => {
        setAddedCartIds((current) => current.filter((id) => id !== productId))
      }, 1600)
    } catch (error) {
      if (isCartUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: {
            mode: "signin",
            redirect: "/",
          },
        })
        return
      }

      toast.error(
        error instanceof Error ? error.message : "Could not add to cart"
      )
      console.error(error)
    } finally {
      setPendingCartIds((current) => current.filter((id) => id !== productId))
    }
  }

  const loadNextProductsPage = useCallback(
    async (force = false) => {
      if (isLoadingMore || !hasMoreProducts || (loadMoreError && !force)) {
        return
      }

      await loadProductsPage(productsPage.number + 1, true)
    },
    [
      hasMoreProducts,
      isLoadingMore,
      loadProductsPage,
      loadMoreError,
      productsPage.number,
    ]
  )

  useEffect(() => {
    const target = loadMoreTargetRef.current

    if (!target || !hasMoreProducts) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          void loadNextProductsPage()
        }
      },
      { root: null, rootMargin: "0px", threshold: 0.1 }
    )

    observer.observe(target)

    return () => observer.disconnect()
  }, [hasMoreProducts, loadNextProductsPage])

  return (
    <main className="min-h-svh bg-neutral-50 text-neutral-900">
      <StorefrontHeader
        cartItemCount={cartItemCount}
        currentAddress={
          loaderData.profile
            ? getCurrentAddressSummary(loaderData.profile)
            : null
        }
        onLogout={loaderData.profile ? handleLogout : undefined}
        onSearchChange={setSearchQuery}
        onSearchSubmit={handleSearchSubmit}
        searchValue={searchQuery}
        username={loaderData.profile?.username}
      />

      <div className="mx-auto max-w-7xl px-4 py-6 lg:px-6">
        <div className="mb-6 flex flex-col gap-4">
          <Breadcrumbs
            items={[{ label: "Home", href: "/" }, { label: "Products" }]}
          />
          <div className="relative overflow-hidden rounded-lg border border-neutral-200 bg-[linear-gradient(120deg,#fff7ed_0%,#ffffff_46%,#e0f2fe_100%)] px-6 py-8 shadow-sm lg:px-8">
            <div
              aria-hidden="true"
              className="absolute top-0 right-0 hidden h-full w-2/5 overflow-hidden lg:block"
            >
              <div className="absolute top-0 right-28 h-full w-16 rotate-12 bg-brand/90" />
              <div className="absolute top-0 right-10 h-full w-16 rotate-12 bg-cyan-500/80" />
              <div className="absolute top-0 -right-10 h-full w-16 rotate-12 bg-amber-300/90" />
            </div>
            <div className="relative max-w-3xl">
              <p className="text-sm font-semibold text-brand">
                N11 Market Picks
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-normal text-neutral-950 lg:text-4xl">
                Fresh finds for every room, routine, and weekend plan
              </h1>
              <p className="mt-3 max-w-xl text-sm leading-6 text-neutral-600">
                Explore popular products, compare prices quickly, and keep
                shopping from one clean catalog.
              </p>
              <div className="mt-5 flex flex-wrap gap-3 text-sm text-neutral-700">
                <span className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/80 px-3 py-2 shadow-sm">
                  <Truck className="h-4 w-4 text-cyan-700" />
                  Fast delivery options
                </span>
                <span className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/80 px-3 py-2 shadow-sm">
                  <BadgePercent className="h-4 w-4 text-brand" />
                  Daily price drops
                </span>
                <span className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/80 px-3 py-2 shadow-sm">
                  <ShieldCheck className="h-4 w-4 text-emerald-700" />
                  Secure checkout
                </span>
              </div>
            </div>
          </div>

          <div className="flex justify-end">
            <label className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 bg-white px-4 py-3 text-sm">
              <span className="text-neutral-500">Sort</span>
              <select
                className="bg-transparent font-medium outline-none"
                onChange={(event) =>
                  updateProductSearch({
                    sort:
                      event.target.value === "relevance"
                        ? undefined
                        : event.target.value,
                  })
                }
                value={sortMode}
              >
                <option value="relevance">Default</option>
                <option value="newest">Newest</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="rating-desc">Highest Rated</option>
              </select>
              <ChevronDown className="h-4 w-4" />
            </label>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="lg:sticky lg:top-6 lg:self-start">
            <div className="space-y-4">
              <FilterSection title="Price Range">
                <form
                  className="space-y-3"
                  onSubmit={(event) => {
                    event.preventDefault()
                    handlePriceSubmit()
                  }}
                >
                  <div className="grid grid-cols-2 gap-2">
                    <label className="space-y-1 text-xs font-medium text-neutral-500">
                      <span>Min</span>
                      <input
                        className="w-full rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-neutral-900"
                        min={0}
                        onChange={(event) =>
                          setMinPriceInput(event.target.value)
                        }
                        placeholder="0"
                        type="number"
                        value={minPriceInput}
                      />
                    </label>
                    <label className="space-y-1 text-xs font-medium text-neutral-500">
                      <span>Max</span>
                      <input
                        className="w-full rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-neutral-900"
                        min={0}
                        onChange={(event) =>
                          setMaxPriceInput(event.target.value)
                        }
                        placeholder="Any"
                        type="number"
                        value={maxPriceInput}
                      />
                    </label>
                  </div>
                  <div className="flex gap-2">
                    <button
                      className="flex-1 rounded-lg bg-neutral-900 px-3 py-2 text-sm font-medium text-white"
                      type="submit"
                    >
                      Apply
                    </button>
                    {typeof minPrice === "number" ||
                    typeof maxPrice === "number" ? (
                      <button
                        className="rounded-lg border border-neutral-200 px-3 py-2 text-sm font-medium text-neutral-700"
                        onClick={() => {
                          setMinPriceInput("")
                          setMaxPriceInput("")
                          updateProductSearch({
                            maxPrice: undefined,
                            minPrice: undefined,
                          })
                        }}
                        type="button"
                      >
                        Clear
                      </button>
                    ) : null}
                  </div>
                </form>
              </FilterSection>

              <FilterSection title="Customer Rating">
                <div className="space-y-2">
                  {[0, 4, 4.5].map((value) => (
                    <button
                      key={value}
                      className={cn(
                        "w-full rounded-lg border px-3 py-2 text-left text-sm",
                        minimumRating === value
                          ? "border-neutral-900 bg-neutral-900 text-white"
                          : "border-neutral-200 bg-white text-neutral-700"
                      )}
                      onClick={() =>
                        updateProductSearch({
                          minRating: value === 0 ? undefined : value,
                        })
                      }
                      type="button"
                    >
                      {value === 0 ? "All ratings" : `${value}+ stars`}
                    </button>
                  ))}
                </div>
              </FilterSection>
            </div>
          </aside>

          <section>
            {products.length === 0 ? (
              <div className="rounded-xl border border-neutral-200 bg-white px-6 py-12 text-center text-neutral-500">
                No products match your current filters.
              </div>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {products.map((product) => (
                  <ProductCard
                    canAddToCart={Boolean(loaderData.profile)}
                    key={product.id}
                    onAddToCart={handleAddToCart}
                    product={product}
                    wasAdded={addedCartIds.includes(product.id)}
                    isAdding={pendingCartIds.includes(product.id)}
                  />
                ))}
              </div>
            )}
            <div ref={loadMoreTargetRef} className="h-12" />
            <div className="flex min-h-10 items-center justify-center text-sm text-neutral-500">
              {isLoadingMore ? (
                <span className="inline-flex items-center gap-2">
                  <LoaderCircle className="h-4 w-4 animate-spin" />
                  Loading more products
                </span>
              ) : loadMoreError ? (
                <button
                  className="rounded-lg border border-neutral-300 bg-white px-4 py-2 text-neutral-700 transition hover:border-neutral-900 hover:text-neutral-950"
                  onClick={() => void loadNextProductsPage(true)}
                  type="button"
                >
                  Try loading more products again
                </button>
              ) : hasMoreProducts ? null : products.length > 0 ? (
                <span>All products loaded.</span>
              ) : null}
            </div>
          </section>
        </div>
      </div>
    </main>
  )
}
