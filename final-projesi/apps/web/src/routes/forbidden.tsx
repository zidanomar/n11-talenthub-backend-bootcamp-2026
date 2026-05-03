import { Link, createFileRoute } from "@tanstack/react-router"
import { ShieldAlert } from "lucide-react"

import { PageShell, Panel } from "@/components/app/page"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/forbidden")({
  component: ForbiddenPage,
})

function ForbiddenPage() {
  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <PageShell maxWidth="5xl">
        <Panel className="flex flex-col items-center gap-4 p-10 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-600">
            <ShieldAlert className="h-7 w-7" />
          </div>
          <h1 className="text-3xl font-semibold">403 — Forbidden</h1>
          <p className="max-w-md text-sm leading-6 text-neutral-500">
            You do not have permission to view this resource. It may belong to
            another account.
          </p>
          <div className="mt-2 flex gap-3">
            <Button asChild>
              <Link to="/">Go home</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/orders">My Orders</Link>
            </Button>
          </div>
        </Panel>
      </PageShell>
    </main>
  )
}
