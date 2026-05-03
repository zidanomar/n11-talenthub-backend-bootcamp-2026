import { createServerFn } from "@tanstack/react-start"
import { deleteCookie, getCookie } from "@tanstack/react-start/server"

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  USERNAME_COOKIE,
} from "@/lib/auth"

export type CartItem = {
  productId: number
  name: string | null
  unitPrice: number
  quantity: number
  totalPrice: number
}

export type Cart = {
  userId: string
  items: Array<CartItem>
  grandTotal: number
}

type CartItemInput = {
  productId: number
  quantity: number
}

const UNAUTHORIZED_ERROR = "UNAUTHORIZED"

async function parseApiError(response: Response) {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message ?? "Request failed"
  } catch {
    return "Request failed"
  }
}

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

async function authorizedCartRequest(path: string, init?: RequestInit) {
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

const fetchCartServerFn = createServerFn({ method: "GET" }).handler(
  async () => {
    return withAuthRecovery(async () => {
      const response = await authorizedCartRequest("/api/cart", {
        headers: {},
      })

      return (await response.json()) as Cart
    })
  }
)

export async function fetchCart() {
  return fetchCartServerFn()
}

const addCartItemServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: CartItemInput) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedCartRequest("/api/cart/items", {
        method: "POST",
        body: JSON.stringify(data),
      })

      return (await response.json()) as Cart
    })
  })

export async function addCartItem(data: CartItemInput) {
  return addCartItemServerFn({ data })
}

const incrementCartItemServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { productId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const currentCartResponse = await authorizedCartRequest("/api/cart", {
        headers: {},
      })
      const currentCart = (await currentCartResponse.json()) as Cart
      const currentItem = currentCart.items.find(
        (item) => item.productId === data.productId
      )

      const response = await authorizedCartRequest("/api/cart/items", {
        method: "POST",
        body: JSON.stringify({
          productId: data.productId,
          quantity: (currentItem?.quantity ?? 0) + 1,
        }),
      })

      return (await response.json()) as Cart
    })
  })

export async function incrementCartItem(productId: number) {
  return incrementCartItemServerFn({ data: { productId } })
}

const removeCartItemServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { productId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedCartRequest(
        `/api/cart/items/${data.productId}`,
        {
          method: "DELETE",
        }
      )

      return (await response.json()) as Cart
    })
  })

export async function removeCartItem(productId: number) {
  return removeCartItemServerFn({ data: { productId } })
}

const clearCartServerFn = createServerFn({ method: "POST" }).handler(
  async () => {
    return withAuthRecovery(async () => {
      await authorizedCartRequest("/api/cart", {
        method: "DELETE",
      })

      return { success: true }
    })
  }
)

export async function clearCart() {
  return clearCartServerFn()
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof Error && error.message === UNAUTHORIZED_ERROR
}
