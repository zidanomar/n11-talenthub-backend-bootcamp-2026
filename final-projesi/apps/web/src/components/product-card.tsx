import { useState } from "react"
import { Link } from "@tanstack/react-router"
import { Check, Plus, ShoppingCart, Star } from "lucide-react"

import type { Product } from "@/lib/products"
import { formatPrice, getImageUrl } from "@/lib/products"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"

export function ProductCard({
  canAddToCart,
  isAdding,
  onAddToCart,
  product,
  wasAdded,
}: {
  product: Product
  canAddToCart: boolean
  isAdding: boolean
  wasAdded: boolean
  onAddToCart: (productId: number) => Promise<void>
}) {
  const imageUrl = getImageUrl(product.images[0])
  const [imageFailed, setImageFailed] = useState(false)

  return (
    <article className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
      <Link
        className="block"
        params={{ productId: String(product.id) }}
        to="/products/$productId"
      >
        <div className="flex aspect-[4/3] items-center justify-center overflow-hidden bg-neutral-50 p-3">
          {imageUrl && !imageFailed ? (
            <img
              alt={product.name}
              className="h-full w-full object-contain"
              loading="lazy"
              onError={() => setImageFailed(true)}
              src={imageUrl}
            />
          ) : (
            <span className="px-4 text-center text-sm font-medium text-neutral-400">
              {product.brand}
            </span>
          )}
        </div>
      </Link>

      <div className="space-y-3 p-4">
        <div>
          <p className="text-sm font-medium text-neutral-900">{product.brand}</p>
          <Link
            className="mt-1 line-clamp-2 text-sm text-neutral-700 hover:text-neutral-950"
            params={{ productId: String(product.id) }}
            to="/products/$productId"
          >
            {product.name}
          </Link>
        </div>

        <div className="flex items-end justify-between gap-3">
          <div className="space-y-1">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-0.5 text-amber-500">
                {Array.from({ length: 5 }).map((_, index) => (
                  <Star
                    className={
                      index < Math.round(product.rating ?? 0)
                        ? "h-3.5 w-3.5 fill-current"
                        : "h-3.5 w-3.5 text-neutral-300"
                    }
                    key={index}
                  />
                ))}
              </div>
              <span className="text-xs font-medium text-neutral-600">
                {(product.rating ?? 0).toFixed(1)}
              </span>
            </div>
            <Badge variant="outline">{product.category}</Badge>
            <p className="text-xl font-semibold text-neutral-950">
              {formatPrice(product.price)}
            </p>
          </div>

          {canAddToCart ? (
            <Button
              aria-label={`Add ${product.name} to cart`}
              disabled={isAdding}
              onClick={() => void onAddToCart(product.id)}
              size="icon"
              type="button"
              variant="outline"
            >
              {wasAdded ? (
                <Check className="h-5 w-5 text-emerald-600" />
              ) : isAdding ? (
                <ShoppingCart className="h-5 w-5 animate-pulse" />
              ) : (
                <Plus className="h-5 w-5" />
              )}
            </Button>
          ) : null}
        </div>
      </div>
    </article>
  )
}
