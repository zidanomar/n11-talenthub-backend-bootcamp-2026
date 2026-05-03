import { useState } from "react"
import {
  createFileRoute,
  notFound,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import { toast } from "sonner"
import { Check, Info, ShoppingCart } from "lucide-react"

import { clearAuthSession, getAuthSession } from "@/lib/auth"
import {
  fetchCart,
  incrementCartItem,
  isUnauthorizedError as isCartUnauthorizedError,
} from "@/lib/cart"
import { fetchUserProfile, getCurrentAddressSummary } from "@/lib/users"

import {
  buildReviewPreview,
  fetchProduct,
  formatPrice,
  getImageUrl,
  isNotFoundError as isProductNotFoundError,
} from "@/lib/products"
import { Breadcrumbs, Panel } from "@/components/app/page"
import { StockBadge } from "@/components/domain/product"
import { StarRating, StorefrontHeader } from "@/components/storefront"

import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/products/$productId")({
  loader: async ({ params }) => {
    const auth = await getAuthSession()

    if (auth.isAuthenticated && auth.role === "MERCHANT") {
      throw redirect({ to: "/admin" })
    }

    let product
    try {
      product = await fetchProduct(params.productId)
    } catch (error) {
      if (isProductNotFoundError(error)) {
        throw notFound()
      }
      throw error
    }

    const [cart, profile] = auth.isAuthenticated
      ? await Promise.all([fetchCart(), fetchUserProfile()])
      : [null, null]

    return {
      cartItemCount:
        cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0,
      product,
      profile,
    }
  },
  component: ProductDetailPage,
})

function ProductDetailPage() {
  const navigate = useNavigate()
  const loaderData = Route.useLoaderData()
  const product = loaderData.product
  const [activeImage, setActiveImage] = useState(0)
  const [failedImages, setFailedImages] = useState<Array<string>>([])
  const [isAddingToCart, setIsAddingToCart] = useState(false)
  const [wasAddedToCart, setWasAddedToCart] = useState(false)
  const [cartItemCount, setCartItemCount] = useState(loaderData.cartItemCount)
  const isOutOfStock = product.stock <= 0
  const canAddToCart = Boolean(loaderData.profile) && !isOutOfStock
  const activeImageName = product.images[activeImage] ?? product.images[0]
  const activeImageUrl = getImageUrl(activeImageName)
  const activeImageFailed = activeImageName
    ? failedImages.includes(activeImageName)
    : true

  function markImageFailed(image: string) {
    setFailedImages((current) =>
      current.includes(image) ? current : [...current, image]
    )
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

  async function handleAddToCart() {
    setIsAddingToCart(true)

    try {
      await incrementCartItem(product.id)
      setCartItemCount((current) => current + 1)
      setWasAddedToCart(true)
      setTimeout(() => {
        setWasAddedToCart(false)
      }, 1800)
    } catch (error) {
      if (isCartUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: {
            mode: "signin",
            redirect: `/products/${product.id}`,
          },
        })
        return
      }

      toast.error(
        error instanceof Error ? error.message : "Could not add to cart"
      )
      console.error(error)
    } finally {
      setIsAddingToCart(false)
    }
  }

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
        searchValue={product.name}
        username={loaderData.profile?.username}
      />

      <div className="mx-auto max-w-7xl px-4 py-6 lg:px-6">
        <Breadcrumbs
          className="mb-5"
          items={[
            { label: "Home", href: "/" },
            { label: "Products", href: "/" },
            { label: product.name },
          ]}
        />
        <>
          <div
            className={`grid gap-8 lg:grid-cols-[1.1fr_1fr_280px] ${
              isOutOfStock ? "opacity-60 grayscale" : ""
            }`}
          >
            <section className="space-y-4">
              <div className="flex aspect-square items-center justify-center overflow-hidden rounded-3xl border border-neutral-200 bg-white p-5">
                {activeImageUrl && !activeImageFailed ? (
                  <img
                    alt={product.name}
                    className="h-full w-full object-contain"
                    onError={() => {
                      if (activeImageName) {
                        markImageFailed(activeImageName)
                      }
                    }}
                    src={activeImageUrl}
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center rounded-2xl bg-neutral-50 px-6 text-center text-sm font-medium text-neutral-400">
                    {product.brand}
                  </div>
                )}
              </div>

              {product.images.length > 1 ? (
                <div className="flex gap-3 overflow-x-auto pb-1">
                  {product.images.map((image, index) => {
                    const thumbnailUrl = getImageUrl(image)
                    const hasFailed = failedImages.includes(image)

                    return (
                      <button
                        key={`${image}-${index}`}
                        aria-label={`Show product image ${index + 1}`}
                        className={`flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl border bg-white p-2 transition ${
                          activeImage === index
                            ? "border-brand ring-2 ring-brand/20"
                            : "border-neutral-200 hover:border-neutral-400"
                        }`}
                        onClick={() => setActiveImage(index)}
                        type="button"
                      >
                        {thumbnailUrl && !hasFailed ? (
                          <img
                            alt={`${product.name} thumbnail ${index + 1}`}
                            className="h-full w-full object-contain"
                            onError={() => markImageFailed(image)}
                            src={thumbnailUrl}
                          />
                        ) : (
                          <span className="text-xs font-medium text-neutral-400">
                            {index + 1}
                          </span>
                        )}
                      </button>
                    )
                  })}
                </div>
              ) : null}
            </section>

            <section className="space-y-5">
              <div>
                <p className="text-sm text-neutral-500">{product.brand}</p>
                <h1 className="mt-2 text-3xl leading-tight font-semibold">
                  {product.name}
                </h1>
              </div>

              <StarRating
                className="text-sm"
                rating={product.rating}
                reviewCount={product.reviewCount}
                size="md"
              />

              <div>
                <p className="text-sm text-neutral-500">{product.category}</p>
                <p className="mt-2 text-5xl font-semibold">
                  {formatPrice(product.price)}
                </p>
                <div className="mt-3">
                  <StockBadge stock={product.stock} />
                </div>
              </div>

              {canAddToCart ? (
                <Button
                  className="w-full"
                  disabled={isAddingToCart}
                  onClick={() => void handleAddToCart()}
                  size="xl"
                  type="button"
                >
                  {wasAddedToCart ? (
                    <>
                      <Check className="h-5 w-5" />
                      <span>Added to Cart</span>
                    </>
                  ) : (
                    <>
                      <ShoppingCart
                        className={`h-5 w-5 ${isAddingToCart ? "animate-pulse" : ""}`}
                      />
                      <span>
                        {isAddingToCart ? "Adding..." : "Add to Cart"}
                      </span>
                    </>
                  )}
                </Button>
              ) : isOutOfStock ? (
                <Button
                  className="w-full"
                  disabled
                  size="xl"
                  type="button"
                  variant="outline"
                >
                  <ShoppingCart className="h-5 w-5" />
                  <span>Out of Stock</span>
                </Button>
              ) : null}
            </section>

            <Panel className="self-start p-5">
              <p className="text-lg font-semibold">Current Coupon</p>
              <p className="mt-3 text-sm leading-6 text-neutral-600">
                Spend TRY 750 and get 25% off directly in your cart. This is a
                static placeholder until promotions are connected to a real
                service.
              </p>
            </Panel>
          </div>

          <div className="mt-8 space-y-6">
            <DetailSection id="details" title="Product Details">
              <div className="grid gap-4 md:grid-cols-2">
                <InfoRow label="Brand" value={product.brand} />
                <InfoRow label="Category" value={product.category} />
                <div className="rounded-xl border border-neutral-200 px-4 py-4">
                  <p className="text-sm text-neutral-500">Availability</p>
                  <div className="mt-2">
                    <StockBadge stock={product.stock} />
                  </div>
                </div>
                <InfoRow
                  label="Gallery Images"
                  value={String(product.images.length)}
                />
              </div>
            </DetailSection>

            <DetailSection
              actionLabel={`See all ${product.reviewCount} reviews`}
              id="reviews"
              title="Product Reviews"
            >
              <div className="space-y-5">
                <div className="rounded-2xl bg-neutral-100 px-5 py-4">
                  <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    <div className="flex items-center gap-3">
                      <span className="text-4xl font-semibold">
                        {product.rating.toFixed(1)}
                      </span>
                      <StarRating
                        rating={product.rating}
                        showCount={false}
                        size="md"
                      />
                    </div>
                    <div className="flex gap-8 text-sm text-neutral-600">
                      <span>{product.reviewCount} ratings</span>
                      <span>
                        {Math.max(product.reviewCount - 16, 1)} reviews
                      </span>
                    </div>
                  </div>
                </div>

                <div className="grid gap-4 lg:grid-cols-[1.1fr_1fr_1fr]">
                  <Panel className="p-5">
                    <div className="mb-4 flex items-center gap-2">
                      <Info className="h-5 w-5 text-neutral-500" />
                      <h3 className="text-lg font-semibold">
                        What customers say
                      </h3>
                    </div>
                    <p className="text-sm leading-7 text-neutral-600">
                      Most shoppers highlight the quality, finish, and value for
                      money. Review placeholders are shown here until the
                      dedicated review service is integrated.
                    </p>
                  </Panel>

                  {buildReviewPreview(product).map((review) => (
                    <Panel key={review.id} className="p-5">
                      <StarRating rating={review.rating} showCount={false} />
                      <h3 className="mt-4 text-lg font-semibold">
                        {review.title}
                      </h3>
                      <p className="mt-2 text-sm leading-7 text-neutral-600">
                        {review.body}
                      </p>
                      <div className="mt-6 flex items-center justify-between text-xs text-neutral-500">
                        <span>{review.author}</span>
                        <span>{review.date}</span>
                      </div>
                    </Panel>
                  ))}
                </div>
              </div>
            </DetailSection>
          </div>
        </>
      </div>
    </main>
  )
}

function DetailSection({
  actionLabel,
  children,
  id,
  title,
}: {
  id: string
  title: string
  children: React.ReactNode
  actionLabel?: string
}) {
  return (
    <Panel className="scroll-mt-48 p-6" id={id}>
      <div className="mb-6 flex items-center justify-between gap-4">
        <h2 className="text-2xl font-semibold">{title}</h2>
        {actionLabel ? (
          <span className="text-sm font-medium text-brand-600">
            {actionLabel}
          </span>
        ) : null}
      </div>
      {children}
    </Panel>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-neutral-200 px-4 py-4">
      <p className="text-sm text-neutral-500">{label}</p>
      <p className="mt-2 font-medium text-neutral-900">{value}</p>
    </div>
  )
}
