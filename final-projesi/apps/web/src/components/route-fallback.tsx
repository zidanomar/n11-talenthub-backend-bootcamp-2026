import { Link } from "@tanstack/react-router"
import {
  AlertTriangle,
  ArrowLeft,
  Home,
  RefreshCw,
  Search,
  ShoppingBag,
} from "lucide-react"
import type {
  ErrorComponentProps,
  NotFoundRouteProps,
} from "@tanstack/react-router"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Panel } from "@/components/app/page"

type RouteFallbackProps = {
  actions?: React.ReactNode
  detail?: string
  eyebrow: string
  icon: React.ComponentType<{ className?: string }>
  message: string
  status: string
  title: string
  tone?: "neutral" | "warning"
}

function goBack() {
  if (typeof window !== "undefined" && window.history.length > 1) {
    window.history.back()
  }
}

function RouteFallback({
  actions,
  detail,
  eyebrow,
  icon: Icon,
  message,
  status,
  title,
  tone = "neutral",
}: RouteFallbackProps) {
  return (
    <main className="min-h-svh bg-neutral-50 text-neutral-950">
      <header className="border-b border-neutral-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 lg:px-6">
          <Link
            aria-label="Go to home"
            className="flex h-12 min-w-12 shrink-0 items-center justify-center rounded-full bg-brand px-3 text-lg font-semibold text-brand-foreground"
            to="/"
          >
            N11
          </Link>
          <Button asChild variant="outline">
            <Link to="/">
              <Home className="h-4 w-4" />
              <span>Home</span>
            </Link>
          </Button>
        </div>
      </header>

      <section className="mx-auto grid min-h-[calc(100svh-81px)] max-w-7xl items-center px-4 py-12 lg:px-6">
        <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-center">
          <div>
            <div
              className={cn(
                "mb-6 inline-flex h-14 w-14 items-center justify-center rounded-2xl border",
                tone === "warning"
                  ? "border-amber-200 bg-amber-50 text-amber-700"
                  : "border-neutral-200 bg-white text-neutral-700",
              )}
            >
              <Icon className="h-6 w-6" />
            </div>
            <p className="text-sm font-semibold tracking-wide text-neutral-500 uppercase">
              {eyebrow}
            </p>
            <h1 className="mt-3 max-w-3xl text-4xl font-semibold tracking-normal text-neutral-950 sm:text-5xl">
              {title}
            </h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-neutral-600">
              {message}
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              {actions}
              <Button
                onClick={goBack}
                type="button"
                variant="outline"
              >
                <ArrowLeft className="h-4 w-4" />
                <span>Go back</span>
              </Button>
            </div>

            {detail ? (
              <div className="mt-8 max-w-2xl rounded-lg border border-neutral-200 bg-white p-4">
                <p className="text-xs font-semibold text-neutral-500 uppercase">
                  Details
                </p>
                <p className="mt-2 break-words text-sm leading-6 text-neutral-700">
                  {detail}
                </p>
              </div>
            ) : null}
          </div>

          <div className="hidden lg:block">
            <Panel className="p-6 shadow-sm">
              <p className="text-sm font-medium text-neutral-500">Status</p>
              <p className="mt-3 text-8xl font-semibold leading-none text-neutral-950">
                {status}
              </p>
              <div className="mt-6 space-y-3 border-t border-neutral-100 pt-5 text-sm text-neutral-600">
                <div className="flex items-center justify-between gap-4">
                  <span>Home page</span>
                  <span className="font-medium text-neutral-950">Ready</span>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <span>Product catalog</span>
                  <span className="font-medium text-neutral-950">Available</span>
                </div>
                <div className="flex items-center justify-between gap-4">
                  <span>Current route</span>
                  <span className="font-medium text-neutral-950">Needs attention</span>
                </div>
              </div>
            </Panel>
          </div>
        </div>
      </section>
    </main>
  )
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message
  }

  if (typeof error === "string") {
    return error
  }

  return undefined
}

export function RootErrorPage({ error, reset }: ErrorComponentProps) {
  return (
    <RouteFallback
      actions={
        <>
          <Button
            onClick={reset}
            type="button"
          >
            <RefreshCw className="h-4 w-4" />
            <span>Try again</span>
          </Button>
          <Button asChild variant="outline">
            <Link to="/">
              <ShoppingBag className="h-4 w-4" />
              <span>Browse products</span>
            </Link>
          </Button>
        </>
      }
      detail={getErrorMessage(error)}
      eyebrow="Something went wrong"
      icon={AlertTriangle}
      message="The page could not finish loading. You can retry the request, return home, or continue browsing the storefront."
      status="500"
      title="We hit a problem loading this page."
      tone="warning"
    />
  )
}

export function RootNotFoundPage(_: NotFoundRouteProps) {
  return (
    <RouteFallback
      actions={
        <Button asChild>
          <Link to="/">
            <Search className="h-4 w-4" />
            <span>Browse products</span>
          </Link>
        </Button>
      }
      eyebrow="Page not found"
      icon={Search}
      message="The page may have moved, the link may be outdated, or the address may have a typo. The storefront is still just one step away."
      status="404"
      title="This page is not on the shelf."
    />
  )
}
