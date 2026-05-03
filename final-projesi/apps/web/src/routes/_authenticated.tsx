import {
  Outlet,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"

import { clearAuthSession, getAuthSession } from "@/lib/auth"
import {
  fetchCart,
  isUnauthorizedError as isCartUnauthorizedError,
} from "@/lib/cart"
import { StorefrontHeader } from "@/components/storefront"
import {
  fetchUserProfile,
  getCurrentAddressSummary,
  isUnauthorizedError as isUserUnauthorizedError,
} from "@/lib/users"

function sanitizeRedirect(url: string) {
  if (!url.startsWith("/") || url.startsWith("//")) {
    return "/"
  }

  return url
}

export const Route = createFileRoute("/_authenticated")({
  beforeLoad: async ({ location }) => {
    const auth = await getAuthSession()

    if (!auth.isAuthenticated) {
      throw redirect({
        to: "/login",
        search: {
          mode: "signin",
          redirect: sanitizeRedirect(location.href),
        },
      })
    }

    if (auth.role === "MERCHANT") {
      throw redirect({ to: "/admin" })
    }

    return { auth }
  },
  loader: async ({ location }) => {
    try {
      const [cart, profile] = await Promise.all([
        fetchCart(),
        fetchUserProfile(),
      ])

      return {
        cartItemCount: cart.items.reduce((sum, item) => sum + item.quantity, 0),
        profile,
      }
    } catch (error) {
      if (isCartUnauthorizedError(error) || isUserUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: sanitizeRedirect(location.href) },
        })
      }

      throw error
    }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  const navigate = useNavigate()
  const { cartItemCount, profile } = Route.useLoaderData()

  async function handleLogout() {
    await clearAuthSession()
    await navigate({ to: "/login", search: { mode: "signin", redirect: "/" } })
  }

  return (
    <>
      <StorefrontHeader
        cartItemCount={cartItemCount}
        currentAddress={getCurrentAddressSummary(profile)}
        onLogout={handleLogout}
        searchValue=""
        username={profile.username}
      />
      <Outlet />
    </>
  )
}
