import { createServerFn } from "@tanstack/react-start"
import { deleteCookie, getCookie } from "@tanstack/react-start/server"

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  USERNAME_COOKIE,
} from "@/lib/auth"

export type NotificationType =
  | "ORDER_CREATED"
  | "PAYMENT_ACCEPTED"
  | "PAYMENT_REJECTED"
  | "ORDER_SHIPPED"
  | "ORDER_DELIVERED"
  | "RETURN_INITIATED"
  | "RETURN_SHIPPED"
  | "REFUNDING"
  | "RETURNED"
  | "RETURN_FAILED"

export type Notification = {
  id: number
  type: NotificationType
  title: string
  message: string
  orderId: number | null
  read: boolean
  createdAt: string
}

export type NotificationsPage = {
  content: Array<Notification>
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type FetchNotificationsInput = {
  from?: string
  limit?: number
  page?: number
  to?: string
}

const UNAUTHORIZED_ERROR = "UNAUTHORIZED"

function getInternalApiBaseUrl() {
  if (process.env.INTERNAL_API_BASE_URL) {
    return process.env.INTERNAL_API_BASE_URL
  }

  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL
  }

  return "http://localhost:8080"
}

function clearAuthCookies() {
  deleteCookie(ACCESS_TOKEN_COOKIE, { path: "/" })
  deleteCookie(REFRESH_TOKEN_COOKIE, { path: "/" })
  deleteCookie(USERNAME_COOKIE, { path: "/" })
}

async function parseApiError(response: Response) {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? "Request failed"
  } catch {
    return "Request failed"
  }
}

async function authorizedNotificationRequest(path: string, init?: RequestInit) {
  const token = getCookie(ACCESS_TOKEN_COOKIE)

  if (!token) {
    throw new Error(UNAUTHORIZED_ERROR)
  }

  const response = await fetch(`${getInternalApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  })

  if (response.status === 401 || response.status === 403) {
    throw new Error(UNAUTHORIZED_ERROR)
  }

  if (!response.ok) {
    throw new Error(await parseApiError(response))
  }

  return response
}

async function withAuthRecovery<T>(callback: () => Promise<T>) {
  try {
    return await callback()
  } catch (error) {
    if (isUnauthorizedError(error)) {
      clearAuthCookies()
    }

    throw error
  }
}

function buildNotificationsPath({
  from,
  limit,
  page,
  to,
}: FetchNotificationsInput = {}) {
  const searchParams = new URLSearchParams()

  if (typeof page === "number") {
    searchParams.set("page", String(page))
  }

  if (typeof limit === "number") {
    searchParams.set("limit", String(limit))
  }

  if (from) {
    searchParams.set("from", from)
  }

  if (to) {
    searchParams.set("to", to)
  }

  const query = searchParams.toString()
  return `/api/notifications${query ? `?${query}` : ""}`
}

const fetchNotificationsServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: FetchNotificationsInput) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedNotificationRequest(
        buildNotificationsPath(data),
      )

      return (await response.json()) as NotificationsPage
    })
  })

export async function fetchNotifications(input: FetchNotificationsInput = {}) {
  return fetchNotificationsServerFn({ data: input })
}

const fetchUnreadNotificationsServerFn = createServerFn({
  method: "GET",
}).handler(async () => {
  return withAuthRecovery(async () => {
    const response = await authorizedNotificationRequest(
      buildNotificationsPath({ limit: 100, page: 0 }),
    )
    const notificationsPage = (await response.json()) as NotificationsPage

    return notificationsPage.content
      .filter((notification) => !notification.read)
      .slice(0, 10)
  })
})

export async function fetchUnreadNotifications() {
  return fetchUnreadNotificationsServerFn()
}

const fetchUnreadNotificationCountServerFn = createServerFn({
  method: "GET",
}).handler(async () => {
  return withAuthRecovery(async () => {
    const response = await authorizedNotificationRequest(
      "/api/notifications/unread-count",
    )
    const body = (await response.json()) as { count: number }

    return body.count
  })
})

export async function fetchUnreadNotificationCount() {
  return fetchUnreadNotificationCountServerFn()
}

const markNotificationReadServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { notificationId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedNotificationRequest(
        `/api/notifications/${data.notificationId}/read`,
        { method: "PATCH" },
      )

      return (await response.json()) as Notification
    })
  })

export async function markNotificationRead(notificationId: number) {
  return markNotificationReadServerFn({ data: { notificationId } })
}

const markAllNotificationsReadServerFn = createServerFn({
  method: "POST",
}).handler(async () => {
  return withAuthRecovery(async () => {
    await authorizedNotificationRequest("/api/notifications/read-all", {
      method: "PATCH",
    })

    return { success: true }
  })
})

export async function markAllNotificationsRead() {
  return markAllNotificationsReadServerFn()
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof Error && error.message === UNAUTHORIZED_ERROR
}

export function sortNotificationsByNewest(
  notifications: Array<Notification>,
) {
  return [...notifications].sort(
    (first, second) =>
      new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime(),
  )
}
