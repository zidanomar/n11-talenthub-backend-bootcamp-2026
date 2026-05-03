import { useState } from "react"
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import { toast } from "sonner"
import {
  Check,
  ChevronRight,
  CreditCard,
  Minus,
  Plus,
  ShoppingBag,
  Trash2,
} from "lucide-react"

import type { Product } from "@/lib/products"
import {
  addCartItem,
  clearCart,
  fetchCart,
  isUnauthorizedError,
  removeCartItem,
} from "@/lib/cart"
import { fetchProductsByIds, formatPrice, getImageUrl } from "@/lib/products"
import {
  Breadcrumbs,
  EmptyState,
  PageShell,
  Panel,
  SummaryRow,
} from "@/components/app/page"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"

type CartPageData = Awaited<ReturnType<typeof loadCartPageData>>

type CartDisplayItem = CartPageData["items"][number]

async function loadCartPageData() {
  const cart = await fetchCart()
  const productIds = [...new Set(cart.items.map((item) => item.productId))]

  const products =
    productIds.length > 0 ? (await fetchProductsByIds(productIds)).content : []

  const productsById = new Map<number, Product>(
    products.map((product) => [product.id, product])
  )

  return {
    cart,
    items: cart.items.map((item) => ({
      ...item,
      product: productsById.get(item.productId) ?? null,
    })),
  }
}

export const Route = createFileRoute("/_authenticated/cart")({
  loader: async ({ location }) => {
    try {
      return await loadCartPageData()
    } catch (error) {
      if (isUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: {
            mode: "signin",
            redirect: location.href,
          },
        })
      }

      throw error
    }
  },
  component: CartPage,
})

function CartPage() {
  const navigate = useNavigate()
  const initialData = Route.useLoaderData()
  const [cartData, setCartData] = useState<CartPageData>(initialData)
  const [busyProductId, setBusyProductId] = useState<number | null>(null)
  const [isClearingCart, setIsClearingCart] = useState(false)

  const itemCount = cartData.cart.items.reduce(
    (sum, item) => sum + item.quantity,
    0
  )

  async function refreshCart() {
    setCartData(await loadCartPageData())
  }

  async function handleUnauthorized() {
    await navigate({
      to: "/login",
      search: {
        mode: "signin",
        redirect: "/cart",
      },
    })
  }

  async function handleQuantityChange(
    item: CartDisplayItem,
    nextQuantity: number
  ) {
    setBusyProductId(item.productId)

    try {
      if (nextQuantity <= 0) {
        await removeCartItem(item.productId)
      } else {
        await addCartItem({
          productId: item.productId,
          quantity: nextQuantity,
        })
      }

      await refreshCart()
    } catch (error) {
      if (isUnauthorizedError(error)) {
        await handleUnauthorized()
        return
      }

      toast.error(
        error instanceof Error ? error.message : "Could not update cart"
      )
      console.error(error)
    } finally {
      setBusyProductId(null)
    }
  }

  async function handleClearCart() {
    setIsClearingCart(true)

    try {
      await clearCart()
      await refreshCart()
    } catch (error) {
      if (isUnauthorizedError(error)) {
        await handleUnauthorized()
        return
      }

      console.error(error)
    } finally {
      setIsClearingCart(false)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-900">
      <PageShell className="py-6">
        <Breadcrumbs
          className="mb-5"
          items={[{ label: "Home", href: "/" }, { label: "Cart" }]}
        />
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
          <section className="space-y-5">
            <Panel className="p-6">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm text-neutral-500">Cart</p>
                  <h1 className="mt-1 text-3xl font-semibold">
                    My Cart ({itemCount})
                  </h1>
                </div>

                {cartData.items.length > 0 ? (
                  <Button
                    disabled={isClearingCart}
                    onClick={() => void handleClearCart()}
                    type="button"
                    variant="link"
                  >
                    {isClearingCart ? "Clearing..." : "Clear Cart"}
                  </Button>
                ) : null}
              </div>

              {cartData.items.length === 0 ? (
                <EmptyState
                  action={
                    <Button asChild>
                      <Link to="/">Continue Shopping</Link>
                    </Button>
                  }
                  className="mt-10"
                  description="Add products from the listing page to see them here."
                  icon={ShoppingBag}
                  title="Your cart is empty"
                />
              ) : (
                <div className="mt-6 space-y-4">
                  {cartData.items.map((item) => (
                    <CartItemCard
                      busy={busyProductId === item.productId}
                      item={item}
                      key={item.productId}
                      onDecrease={() =>
                        void handleQuantityChange(item, item.quantity - 1)
                      }
                      onIncrease={() =>
                        void handleQuantityChange(item, item.quantity + 1)
                      }
                      onRemove={() => void handleQuantityChange(item, 0)}
                    />
                  ))}
                </div>
              )}
            </Panel>
          </section>

          <aside className="space-y-4 lg:sticky lg:top-6 lg:self-start">
            <Panel className="p-5">
              <h2 className="text-lg font-semibold">Order Summary</h2>

              <div className="mt-4 space-y-3 text-sm">
                <SummaryRow label="Items" value={String(itemCount)} />
                <SummaryRow
                  label="Subtotal"
                  value={formatPrice(cartData.cart.grandTotal)}
                />
                <SummaryRow label="Delivery" value="Free" />
              </div>

              <div className="mt-5 border-t border-neutral-200 pt-4">
                <SummaryRow
                  label="Total"
                  value={formatPrice(cartData.cart.grandTotal)}
                  large
                />
              </div>

              <Button asChild className="mt-5 w-full" size="xl">
                <Link to="/payment">
                  <CreditCard className="h-5 w-5" />
                  <span>Proceed to Payment</span>
                </Link>
              </Button>
            </Panel>
          </aside>
        </div>
      </PageShell>
    </main>
  )
}

function CartItemCard({
  busy,
  item,
  onDecrease,
  onIncrease,
  onRemove,
}: {
  busy: boolean
  item: CartDisplayItem
  onDecrease: () => void
  onIncrease: () => void
  onRemove: () => void
}) {
  const image = item.product?.images[0]
    ? getImageUrl(item.product.images[0])
    : undefined
  const [imageFailed, setImageFailed] = useState(false)

  return (
    <Panel className="p-4">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex min-w-0 gap-4">
          <div className="flex h-28 w-28 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-neutral-200 bg-neutral-50 p-2">
            {image && !imageFailed ? (
              <img
                alt={item.name ?? "Cart item"}
                className="h-full w-full object-contain"
                onError={() => setImageFailed(true)}
                src={image}
              />
            ) : (
              <ShoppingBag className="h-8 w-8 text-neutral-300" />
            )}
          </div>

          <div className="min-w-0 space-y-3">
            <div>
              {item.product ? (
                <p className="text-sm font-medium text-neutral-500">
                  {item.product.brand}
                </p>
              ) : null}
              <Link
                className="mt-1 block text-lg font-medium text-neutral-900"
                params={{ productId: String(item.productId) }}
                to="/products/$productId"
              >
                {item.name ?? "Product"}
              </Link>
              <p className="mt-1 text-sm text-neutral-500">
                Unit price: {formatPrice(item.unitPrice)}
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="inline-flex items-center rounded-xl border border-neutral-200">
                <Button
                  disabled={busy}
                  onClick={onDecrease}
                  size="icon"
                  type="button"
                  variant="ghost"
                >
                  <Minus className="h-4 w-4" />
                </Button>
                <span className="min-w-10 text-center text-sm font-medium">
                  {item.quantity}
                </span>
                <Button
                  disabled={
                    busy ||
                    (typeof item.product?.stock === "number" &&
                      item.quantity >= item.product.stock)
                  }
                  onClick={onIncrease}
                  size="icon"
                  type="button"
                  variant="ghost"
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>

              <Button
                disabled={busy}
                onClick={onRemove}
                type="button"
                variant="ghost"
              >
                <Trash2 className="h-4 w-4" />
                <span>Remove</span>
              </Button>
            </div>
          </div>
        </div>

        <div className="flex flex-col items-start gap-3 lg:items-end">
          <p className="text-2xl font-semibold">
            {formatPrice(item.totalPrice)}
          </p>
          <Badge variant="success">
            <Check className="h-3.5 w-3.5 text-emerald-600" />
            <span>Free shipping</span>
          </Badge>
          <div className="flex items-center gap-2 text-sm text-neutral-500">
            <span>
              {item.product?.stock === 0
                ? "Out of stock"
                : "Store delivery available"}
            </span>
            <ChevronRight className="h-4 w-4" />
          </div>
        </div>
      </div>
    </Panel>
  )
}
