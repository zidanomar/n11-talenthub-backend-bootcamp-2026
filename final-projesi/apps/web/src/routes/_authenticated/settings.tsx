import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import {
  ChevronRight,
  KeyRound,
  LogOut,
  MapPin,
  Package,
} from "lucide-react"

import { clearAuthSession } from "@/lib/auth"
import {
  fetchUserProfile,
  isUnauthorizedError as isUserUnauthorizedError,
} from "@/lib/users"
import { PageHeader, PageShell } from "@/components/app/page"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/_authenticated/settings")({
  loader: async ({ location }) => {
    try {
      const profile = await fetchUserProfile()

      return { profile }
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
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
  component: SettingsPage,
})

const settingsItems = [
  { label: "My Addresses", icon: MapPin, to: "/settings/addresses" },
  { label: "My Orders", icon: Package, to: "/orders" },
  { label: "Change Password", icon: KeyRound, to: "/settings/password" },
] as const

function SettingsPage() {
  const navigate = useNavigate()
  const { profile } = Route.useLoaderData()

  async function handleLogout() {
    await clearAuthSession()
    await navigate({
      to: "/login",
      search: {
        mode: "signin",
        redirect: "/settings",
      },
    })
  }

  return (
    <main className="min-h-svh bg-neutral-50 text-neutral-950">
      <PageShell>
        <PageHeader
          breadcrumbs={[
            { label: "Home", href: "/" },
            { label: "My Account", href: "/settings" },
            { label: "Account Settings" },
          ]}
          description={`Manage preferences for ${profile.firstName} ${profile.lastName}.`}
          eyebrow="My Account"
          title="Account Settings"
        />

        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {settingsItems.map((item) => {
            const Icon = item.icon

            return (
              <Link
                className="group flex min-h-18 items-center justify-between rounded-lg border border-neutral-300 bg-white px-5 py-4 text-neutral-900 transition hover:border-neutral-900 hover:shadow-sm"
                key={item.label}
                to={item.to}
              >
                <span className="flex min-w-0 items-center gap-3">
                  <Icon className="h-5 w-5 shrink-0 text-neutral-600" />
                  <span className="truncate text-sm font-medium">
                    {item.label}
                  </span>
                </span>
                <ChevronRight className="h-5 w-5 shrink-0 text-neutral-800 transition group-hover:translate-x-0.5" />
              </Link>
            )
          })}

          <Button
            className="flex min-h-18 items-center justify-between px-5 py-4 text-left"
            onClick={() => void handleLogout()}
            type="button"
            variant="outline"
          >
            <span className="text-sm font-medium">Log Out</span>
            <LogOut className="h-5 w-5 text-neutral-600" />
          </Button>
        </div>
      </PageShell>
    </main>
  )
}
