import { Outlet, createFileRoute, redirect } from "@tanstack/react-router"

import { getAuthSession } from "@/lib/auth"

export const Route = createFileRoute("/admin")({
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

    return { auth }
  },
  component: AdminLayout,
})

function AdminLayout() {
  return <Outlet />
}
