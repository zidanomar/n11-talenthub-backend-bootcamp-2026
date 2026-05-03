import { createServerFn } from "@tanstack/react-start"
import { deleteCookie, getCookie } from "@tanstack/react-start/server"

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  USERNAME_COOKIE,
} from "@/lib/auth"

export type OrderStatus =
  | "CHECKOUT"
  | "PENDING"
  | "PAYMENT_FAILED"
  | "PAID"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURNING"
  | "RETURN_SHIPPED"
  | "REFUNDING"
  | "RETURNED"
  | "RETURN_FAILED"
  | "COMPLETED"

export type OrderItem = {
  productId: number
  quantity: number
  unitPrice: number
  totalPrice: number
}

export type OrderStatusHistoryEntry = {
  status: OrderStatus
  changedAt: string
  note?: string | null
}

export type Order = {
  id: number
  userId: string
  status: OrderStatus
  items: Array<OrderItem>
  totalPrice: number
  createdAt: string
  statusHistory: Array<OrderStatusHistoryEntry>
}

export type OrdersPage = {
  content: Array<Order>
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type FetchOrdersInput = {
  from?: string
  page?: number
  size?: number
  to?: string
}

export type PayOrderResponse = {
  order: Order
  paymentPageUrl: string
  token: string
}

export type PayOrderInput = {
  savedCardId?: number | null
  forceThreeDS?: boolean
  paymentWithNewCardEnabled?: boolean
}

const UNAUTHORIZED_ERROR = "UNAUTHORIZED"
const FORBIDDEN_ERROR = "FORBIDDEN"
const NOT_FOUND_ERROR = "NOT_FOUND"

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

async function authorizedOrderRequest(path: string, init?: RequestInit) {
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

  if (response.status === 401) {
    throw new Error(UNAUTHORIZED_ERROR)
  }

  if (response.status === 403) {
    throw new Error(FORBIDDEN_ERROR)
  }

  if (response.status === 404) {
    throw new Error(NOT_FOUND_ERROR)
  }

  if (!response.ok) {
    let message = "Request failed"
    try {
      const body = (await response.json()) as {
        detail?: string
        error?: string
        message?: string
      }
      message = body.message ?? body.detail ?? body.error ?? message
    } catch {}
    throw new Error(normalizeOrderErrorMessage(message))
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

const placeOrderServerFn = createServerFn({ method: "POST" }).handler(
  async () => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest("/api/orders", {
        method: "POST",
        body: JSON.stringify({}),
      })

      return (await response.json()) as Order
    })
  }
)

export async function placeOrder() {
  return placeOrderServerFn()
}

const payOrderServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number; values?: PayOrderInput }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}/pay`,
        {
          method: "POST",
          body: JSON.stringify({
            method: "IYZICO",
            savedCardId: data.values?.savedCardId ?? null,
            forceThreeDS: data.values?.forceThreeDS ?? true,
            paymentWithNewCardEnabled:
              data.values?.paymentWithNewCardEnabled ?? true,
          }),
        }
      )

      return (await response.json()) as PayOrderResponse
    })
  })

export async function payOrder(orderId: number, values?: PayOrderInput) {
  return payOrderServerFn({ data: { orderId, values } })
}

function buildOrdersSearchParams(data: FetchOrdersInput | undefined, defaultSize: number) {
  const params = new URLSearchParams()
  params.set("page", String(data?.page ?? 0))
  params.set("size", String(data?.size ?? defaultSize))

  if (data?.from) {
    params.set("from", data.from)
  }

  if (data?.to) {
    params.set("to", data.to)
  }

  return params
}

const fetchOrdersServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: FetchOrdersInput | undefined) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const params = buildOrdersSearchParams(data, 6)
      const response = await authorizedOrderRequest(`/api/orders?${params}`)

      return (await response.json()) as OrdersPage
    })
  })

export async function fetchOrders(options?: FetchOrdersInput) {
  return fetchOrdersServerFn({ data: options })
}

const fetchAdminOrdersServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: FetchOrdersInput | undefined) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const params = buildOrdersSearchParams(data, 12)
      const response = await authorizedOrderRequest(
        `/api/orders/admin?${params}`
      )

      return (await response.json()) as OrdersPage
    })
  })

export async function fetchAdminOrders(options?: {
  from?: string
  page?: number
  size?: number
  to?: string
}) {
  return fetchAdminOrdersServerFn({ data: options })
}

const updateAdminOrderStatusServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number; status: OrderStatus }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/admin/${data.orderId}/status`,
        {
          method: "PATCH",
          body: JSON.stringify({ status: data.status }),
        }
      )

      return (await response.json()) as Order
    })
  })

export async function updateAdminOrderStatus(
  orderId: number,
  status: OrderStatus
) {
  return updateAdminOrderStatusServerFn({ data: { orderId, status } })
}

const fetchOrderServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: { orderId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}`
      )

      return (await response.json()) as Order
    })
  })

export async function fetchOrder(orderId: number) {
  return fetchOrderServerFn({ data: { orderId } })
}

const returnOrderServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}/return`,
        {
          method: "POST",
          body: JSON.stringify({}),
        }
      )

      return (await response.json()) as Order
    })
  })

export async function returnOrder(orderId: number) {
  return returnOrderServerFn({ data: { orderId } })
}

const finishOrderServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}/finish`,
        {
          method: "POST",
          body: JSON.stringify({}),
        }
      )

      return (await response.json()) as Order
    })
  })

export async function finishOrder(orderId: number) {
  return finishOrderServerFn({ data: { orderId } })
}

const retryRefundServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}/retry-refund`,
        {
          method: "POST",
          body: JSON.stringify({}),
        }
      )

      return (await response.json()) as Order
    })
  })

export async function retryRefund(orderId: number) {
  return retryRefundServerFn({ data: { orderId } })
}

const cancelOrderServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { orderId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedOrderRequest(
        `/api/orders/${data.orderId}/cancel`,
        { method: "POST" }
      )

      return (await response.json()) as Order
    })
  })

export async function cancelOrder(orderId: number) {
  return cancelOrderServerFn({ data: { orderId } })
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof Error && error.message === UNAUTHORIZED_ERROR
}

export function isForbiddenError(error: unknown) {
  return error instanceof Error && error.message === FORBIDDEN_ERROR
}

export function isNotFoundError(error: unknown) {
  return error instanceof Error && error.message === NOT_FOUND_ERROR
}

function normalizeOrderErrorMessage(message: string) {
  if (
    message.includes("/api/payments/initiate") ||
    message.includes("payment") ||
    message.includes("PaymentClient#initiate")
  ) {
    return "Payment could not be started. Please try again."
  }

  return message
}
