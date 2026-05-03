import { useEffect, useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { Bell, Check, CheckCheck, PackageCheck } from "lucide-react"

import type { Notification } from "@/lib/notifications"
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  isUnauthorizedError as isNotificationUnauthorizedError,
  markAllNotificationsRead,
  markNotificationRead,
} from "@/lib/notifications"
import { formatNotificationDate } from "@/components/notification-bell"
import {
  DateRangeFilter,
  EmptyState,
  PageHeader,
  PageShell,
  PaginationControls,
  Panel,
  TableFooter,
} from "@/components/app/page"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { getLastSevenDaysRange, normalizeDateInput } from "@/lib/date-range"
import { cn } from "@/lib/utils"

type NotificationsSearch = {
  from?: string
  page?: number
  size?: number
  to?: string
}

function normalizePositiveInteger(value: unknown, fallback: number) {
  const numberValue =
    typeof value === "number"
      ? value
      : typeof value === "string"
        ? Number(value)
        : fallback

  return Number.isInteger(numberValue) && numberValue > 0
    ? numberValue
    : fallback
}

export const Route = createFileRoute("/_authenticated/notifications")({
  validateSearch: (search: Record<string, unknown>): NotificationsSearch => {
    const defaultRange = getLastSevenDaysRange()

    return {
      from: normalizeDateInput(search.from) ?? defaultRange.from,
      page: normalizePositiveInteger(search.page, 1),
      size: normalizePositiveInteger(search.size, 10),
      to: normalizeDateInput(search.to) ?? defaultRange.to,
    }
  },
  loaderDeps: ({ search }) => search,
  loader: async ({ deps, location }) => {
    try {
      const page = (deps.page ?? 1) - 1
      const size = deps.size ?? 10
      const [notificationsPage, unreadCount] = await Promise.all([
        fetchNotifications({
          from: deps.from,
          limit: size,
          page,
          to: deps.to,
        }),
        fetchUnreadNotificationCount(),
      ])

      return { notificationsPage, unreadCount }
    } catch (error) {
      if (isNotificationUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: location.href },
        })
      }

      throw error
    }
  },
  component: NotificationsPage,
})

function NotificationsPage() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const { notificationsPage, unreadCount: initialUnreadCount } =
    Route.useLoaderData()
  const [notifications, setNotifications] = useState(notificationsPage.content)
  const [unreadCount, setUnreadCount] = useState(initialUnreadCount)
  const [fromInput, setFromInput] = useState(search.from ?? "")
  const [toInput, setToInput] = useState(search.to ?? "")
  const currentPage = notificationsPage.number + 1
  const totalPages = Math.max(notificationsPage.totalPages, 1)

  useEffect(() => {
    setNotifications(notificationsPage.content)
  }, [notificationsPage])

  useEffect(() => {
    setUnreadCount(initialUnreadCount)
  }, [initialUnreadCount])

  useEffect(() => {
    setFromInput(search.from ?? "")
  }, [search.from])

  useEffect(() => {
    setToInput(search.to ?? "")
  }, [search.to])

  function updateNotificationsSearch(nextSearch: NotificationsSearch) {
    void navigate({
      to: "/notifications",
      search: {
        ...search,
        ...nextSearch,
      },
    })
  }

  function applyDateFilter() {
    updateNotificationsSearch({
      from: normalizeDateInput(fromInput),
      page: 1,
      to: normalizeDateInput(toInput),
    })
  }

  function resetDateFilter() {
    const defaultRange = getLastSevenDaysRange()
    setFromInput(defaultRange.from)
    setToInput(defaultRange.to)
    updateNotificationsSearch({
      from: defaultRange.from,
      page: 1,
      to: defaultRange.to,
    })
  }

  function goToPage(page: number) {
    updateNotificationsSearch({ page })
  }

  async function handleMarkRead(notification: Notification) {
    try {
      const updated = await markNotificationRead(notification.id)
      setNotifications((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      )
      if (!notification.read && updated.read) {
        setUnreadCount((current) => Math.max(current - 1, 0))
      }
    } catch (error) {
      if (isNotificationUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: { mode: "signin", redirect: "/notifications" },
        })
        return
      }

      console.error(error)
    }
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsRead()
      setNotifications((current) =>
        current.map((notification) => ({ ...notification, read: true }))
      )
      setUnreadCount(0)
    } catch (error) {
      if (isNotificationUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: { mode: "signin", redirect: "/notifications" },
        })
        return
      }

      console.error(error)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <PageShell maxWidth="5xl">
        <PageHeader
          actions={
            <Button
              disabled={unreadCount === 0}
              onClick={() => void handleMarkAllRead()}
              type="button"
              variant="outline"
            >
              <CheckCheck className="h-4 w-4" />
              <span>Mark all read</span>
            </Button>
          }
          breadcrumbs={[
            { label: "Home", href: "/" },
            { label: "My Account", href: "/settings" },
            { label: "Notifications" },
          ]}
          description={`${unreadCount} unread · ${notificationsPage.totalElements.toLocaleString("en-US")} total.`}
          eyebrow="My Account"
          title="Notifications"
        />

        <DateRangeFilter
          from={fromInput}
          onApply={applyDateFilter}
          onFromChange={setFromInput}
          onReset={resetDateFilter}
          onToChange={setToInput}
          to={toInput}
        />

        {notifications.length === 0 ? (
          <EmptyState
            description={
              search.from || search.to
                ? "Try widening the date filter."
                : "Order and payment updates will appear here."
            }
            icon={Bell}
            title={
              search.from || search.to
                ? "No notifications match this date range"
                : "No notifications yet"
            }
          />
        ) : (
          <Panel className="overflow-hidden">
            {notifications.map((notification) => (
              <article
                className={cn(
                  "grid gap-4 border-b border-neutral-100 px-5 py-5 last:border-0 sm:grid-cols-[auto_minmax(0,1fr)_auto]",
                  notification.read ? "bg-white" : "bg-brand/5"
                )}
                key={notification.id}
              >
                <div className="flex h-11 w-6 items-start justify-center pt-1 text-neutral-500">
                  <PackageCheck className="h-5 w-5" />
                </div>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="text-base font-semibold">
                      {notification.title}
                    </h2>
                    {!notification.read ? (
                      <Badge variant="default">New</Badge>
                    ) : null}
                  </div>
                  <p className="mt-1 text-sm leading-6 text-neutral-600">
                    {notification.message}
                  </p>
                  <p className="mt-2 text-xs font-medium text-neutral-400">
                    {formatNotificationDate(notification.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-2 sm:justify-end">
                  {notification.orderId ? (
                    <Button
                      onClick={() =>
                        void navigate({
                          to: "/orders/$orderId",
                          params: { orderId: String(notification.orderId) },
                        })
                      }
                      type="button"
                      variant="outline"
                    >
                      Order #{notification.orderId}
                    </Button>
                  ) : null}
                  {!notification.read ? (
                    <Button
                      aria-label="Mark notification as read"
                      onClick={() => void handleMarkRead(notification)}
                      size="icon"
                      type="button"
                      variant="outline"
                    >
                      <Check className="h-4 w-4" />
                    </Button>
                  ) : null}
                </div>
              </article>
            ))}
            <TableFooter>
              <PaginationControls
                currentPage={currentPage}
                onPageChange={goToPage}
                totalPages={totalPages}
              />
            </TableFooter>
          </Panel>
        )}
      </PageShell>
    </main>
  )
}
