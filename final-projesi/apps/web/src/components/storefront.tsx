import { Link } from "@tanstack/react-router"
import {
  LogIn,
  LogOut,
  MapPin,
  Search,
  ShoppingCart,
  Star,
  User,
} from "lucide-react"

import type { CurrentAddressSummary } from "@/lib/users"
import { NotificationBell } from "@/components/notification-bell"
import { cn } from "@/lib/utils"

export function StorefrontHeader({
  cartHref = "/cart",
  cartItemCount = 0,
  currentAddress,
  onLogout,
  onSearchChange,
  onSearchSubmit,
  searchValue,
  username,
}: {
  cartHref?: string
  cartItemCount?: number
  currentAddress?: CurrentAddressSummary | null
  searchValue: string
  username?: string | null
  onSearchChange?: (value: string) => void
  onSearchSubmit?: () => void
  onLogout?: () => void
}) {
  const isAuthenticated = Boolean(username)
  const deliveryTitle = currentAddress
    ? currentAddress.label || currentAddress.city
    : "Add Address"
  const deliverySubtitle = currentAddress
    ? [currentAddress.city, currentAddress.zipCode].filter(Boolean).join(", ")
    : "Delivery Address"

  return (
    <div className="border-b border-neutral-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-4 lg:px-6">
        <Link
          aria-label="Go to home"
          className="flex h-12 min-w-12 shrink-0 items-center justify-center rounded-full bg-brand px-3 text-lg font-semibold text-brand-foreground"
          to="/"
        >
          N11
        </Link>

        <form
          className="flex min-w-0 flex-1 items-center gap-3 rounded-full border border-neutral-200 bg-neutral-50 px-4 py-3"
          onSubmit={(event) => {
            event.preventDefault()
            onSearchSubmit?.()
          }}
        >
          <Search className="h-4 w-4 shrink-0 text-neutral-500" />
          <input
            aria-label="Search products"
            className="w-full bg-transparent text-sm outline-none placeholder:text-neutral-500"
            onChange={(event) => onSearchChange?.(event.target.value)}
            placeholder="Search products"
            readOnly={!onSearchChange}
            value={searchValue}
          />
          <button className="sr-only" type="submit">
            Search
          </button>
        </form>

        {isAuthenticated ? (
          <div className="hidden items-center gap-6 text-sm lg:flex">
            <Link
              className="flex max-w-44 items-center gap-2"
              title={
                currentAddress
                  ? `${currentAddress.addressLine}, ${currentAddress.city}, ${currentAddress.zipCode}, ${currentAddress.country}`
                  : "Add delivery address"
              }
              to="/settings/addresses"
            >
              <MapPin className="h-5 w-5 text-neutral-600" />
              <div className="min-w-0">
                <p className="text-xs text-neutral-500">Delivery Address</p>
                <p className="truncate font-medium">{deliveryTitle}</p>
                {currentAddress ? (
                  <p className="truncate text-xs text-neutral-500">
                    {deliverySubtitle}
                  </p>
                ) : null}
              </div>
            </Link>
            <a className="flex items-center gap-2" href={cartHref}>
              <span className="relative inline-flex">
                <ShoppingCart className="h-5 w-5 text-neutral-700" />
                {cartItemCount > 0 ? (
                  <span className="absolute -top-2 -right-2.5 inline-flex min-w-5 items-center justify-center rounded-full bg-brand px-1.5 py-0.5 text-[10px] leading-none font-semibold text-brand-foreground">
                    {cartItemCount > 99 ? "99+" : cartItemCount}
                  </span>
                ) : null}
              </span>
            </a>
            <NotificationBell />
            <Link className="flex items-center gap-2" to="/settings">
              <User className="h-5 w-5 text-neutral-700" />
              <span>{username}</span>
            </Link>
            {onLogout ? (
              <button
                className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 px-3 py-2"
                onClick={onLogout}
                type="button"
              >
                <LogOut className="h-4 w-4" />
                <span>Log Out</span>
              </button>
            ) : null}
          </div>
        ) : (
          <div className="hidden items-center gap-3 text-sm lg:flex">
            <Link
              className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 px-3 py-2 font-medium"
              to="/login"
              search={{ mode: "signin", redirect: "/" }}
            >
              <LogIn className="h-4 w-4" />
              <span>Log In</span>
            </Link>
            <Link
              className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-2 font-medium text-brand-foreground"
              to="/login"
              search={{ mode: "signup", redirect: "/" }}
            >
              <span>Sign Up</span>
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}

export function StarRating({
  className,
  rating,
  reviewCount,
  showCount = true,
  size = "sm",
}: {
  rating: number
  reviewCount?: number
  showCount?: boolean
  size?: "sm" | "md"
  className?: string
}) {
  const iconSize = size === "md" ? "h-5 w-5" : "h-4 w-4"
  const textSize = size === "md" ? "text-sm" : "text-xs"

  return (
    <div
      className={cn(
        "flex items-center gap-2 text-neutral-600",
        textSize,
        className
      )}
    >
      <div className="flex items-center gap-0.5 text-amber-500">
        {Array.from({ length: 5 }).map((_, index) => (
          <Star
            key={index}
            className={iconSize}
            fill={index < Math.round(rating) ? "currentColor" : "none"}
          />
        ))}
      </div>
      <span className="font-medium text-neutral-900">{rating.toFixed(1)}</span>
      {showCount && typeof reviewCount === "number" ? (
        <span>({reviewCount} reviews)</span>
      ) : null}
    </div>
  )
}
