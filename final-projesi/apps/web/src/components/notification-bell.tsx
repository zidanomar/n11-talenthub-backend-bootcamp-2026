import { useEffect, useMemo, useRef, useState } from "react"
import { Link, useNavigate } from "@tanstack/react-router"
import { Bell, CheckCheck, ExternalLink } from "lucide-react"

import type { Notification } from "@/lib/notifications"
import { Panel } from "@/components/app/page"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  fetchUnreadNotifications,
  isUnauthorizedError,
  markAllNotificationsRead,
  markNotificationRead,
  sortNotificationsByNewest,
} from "@/lib/notifications"
import { cn } from "@/lib/utils"

export function NotificationBell() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [notifications, setNotifications] = useState<Array<Notification>>([])
  const [isLoading, setIsLoading] = useState(true)
  const menuRef = useRef<HTMLDivElement | null>(null)

  const visibleNotifications = useMemo(
    () =>
      sortNotificationsByNewest(
        notifications.filter((notification) => !notification.read),
      ).slice(0, 10),
    [notifications],
  )

  useEffect(() => {
    let isMounted = true

    fetchUnreadNotifications()
      .then((data) => {
        if (isMounted) {
          setNotifications(data)
        }
      })
      .catch((error) => {
        if (isUnauthorizedError(error)) {
          return
        }

        console.error(error)
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  useEffect(() => {
    const eventSource = new EventSource("/notification-stream")

    eventSource.addEventListener("notification", (event) => {
      const notification = JSON.parse(event.data) as Notification

      setNotifications((current) =>
        sortNotificationsByNewest([
          notification,
          ...current.filter((item) => item.id !== notification.id),
        ]).slice(0, 10),
      )
    })

    return () => {
      eventSource.close()
    }
  }, [])

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (
        menuRef.current &&
        event.target instanceof Node &&
        !menuRef.current.contains(event.target)
      ) {
        setIsOpen(false)
      }
    }

    document.addEventListener("pointerdown", handlePointerDown)

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown)
    }
  }, [])

  async function handleNotificationClick(notification: Notification) {
    try {
      await markNotificationRead(notification.id)
      setNotifications((current) =>
        current.filter((item) => item.id !== notification.id),
      )
      setIsOpen(false)

      if (notification.orderId) {
        await navigate({
          to: "/orders/$orderId",
          params: { orderId: String(notification.orderId) },
        })
      } else {
        await navigate({ to: "/notifications" })
      }
    } catch (error) {
      if (isUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: { mode: "signin", redirect: "/" },
        })
        return
      }

      console.error(error)
    }
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsRead()
      setNotifications([])
    } catch (error) {
      if (isUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: { mode: "signin", redirect: "/" },
        })
        return
      }

      console.error(error)
    }
  }

  return (
    <div className="relative" ref={menuRef}>
      <Button
        aria-label="Open notifications"
        className="relative rounded-full"
        onClick={() => setIsOpen((current) => !current)}
        size="icon"
        type="button"
        variant="outline"
      >
        <Bell className="h-5 w-5" />
        {visibleNotifications.length > 0 ? (
          <Badge
            className="absolute -top-1.5 -right-1.5 h-5 min-w-5 px-1.5 py-0 text-[10px] leading-none"
            size="sm"
            variant="brand"
          >
            {visibleNotifications.length > 9 ? "9+" : visibleNotifications.length}
          </Badge>
        ) : null}
      </Button>

      {isOpen ? (
        <Panel className="absolute top-12 right-0 z-30 w-[min(360px,calc(100vw-2rem))] overflow-hidden p-0 shadow-xl">
          <div className="flex items-center justify-between gap-3 border-b border-neutral-100 px-4 py-3">
            <div>
              <p className="text-sm font-semibold text-neutral-950">
                Notifications
              </p>
              <p className="text-xs text-neutral-500">
                {visibleNotifications.length} unseen
              </p>
            </div>
            <Button
              aria-label="Mark all notifications as read"
              className="rounded-full"
              disabled={visibleNotifications.length === 0}
              onClick={() => void handleMarkAllRead()}
              size="icon-xs"
              type="button"
              variant="outline"
            >
              <CheckCheck className="h-4 w-4" />
            </Button>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <p className="px-4 py-6 text-sm text-neutral-500">
                Loading notifications...
              </p>
            ) : visibleNotifications.length === 0 ? (
              <p className="px-4 py-6 text-sm text-neutral-500">
                No unseen notifications.
              </p>
            ) : (
              visibleNotifications.map((notification) => (
                <button
                  className="block w-full border-b border-neutral-100 px-4 py-3 text-left last:border-0 hover:bg-neutral-50"
                  key={notification.id}
                  onClick={() => void handleNotificationClick(notification)}
                  type="button"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-neutral-950">
                        {notification.title}
                      </p>
                      <p className="mt-1 line-clamp-2 text-xs leading-5 text-neutral-600">
                        {notification.message}
                      </p>
                    </div>
                    <span
                      className={cn(
                        "mt-1 h-2 w-2 shrink-0 rounded-full",
                        notification.type === "PAYMENT_REJECTED"
                          ? "bg-red-500"
                          : "bg-brand",
                      )}
                    />
                  </div>
                  <p className="mt-2 text-[11px] font-medium text-neutral-400">
                    {formatNotificationDate(notification.createdAt)}
                  </p>
                </button>
              ))
            )}
          </div>

          <Link
            className="flex items-center justify-center gap-2 border-t border-neutral-100 px-4 py-3 text-sm font-semibold text-neutral-950 hover:bg-neutral-50"
            onClick={() => setIsOpen(false)}
            to="/notifications"
          >
            <span>View all notifications</span>
            <ExternalLink className="h-4 w-4" />
          </Link>
        </Panel>
      ) : null}
    </div>
  )
}

export function formatNotificationDate(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value))
}
