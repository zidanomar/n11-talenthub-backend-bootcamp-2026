import { createServerFn } from "@tanstack/react-start"
import { deleteCookie, getCookie, setCookie } from "@tanstack/react-start/server"

export type SigninPayload = {
  username: string
  password: string
}

export type SignupPayload = {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
}

export type AuthSession = {
  isAuthenticated: boolean
  role: "CUSTOMER" | "MERCHANT"
  username: string | null
}

type PersistSessionInput = {
  accessToken: string
  refreshToken?: string
  username: string
}

type SigninResponse = {
  access_token?: string
  refresh_token?: string
  [key: string]: unknown
}

export const ACCESS_TOKEN_COOKIE = "n11_access_token"
export const REFRESH_TOKEN_COOKIE = "n11_refresh_token"
export const USERNAME_COOKIE = "n11_username"

const baseCookieOptions = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.COOKIE_SECURE === "true",
  path: "/",
}

const accessCookieOptions = { ...baseCookieOptions, maxAge: 60 * 60 }
const refreshCookieOptions = { ...baseCookieOptions, maxAge: 60 * 60 * 24 }

type ApiErrorBody = {
  message?: string
  errors?: Array<{ field?: string; message?: string }>
}

type JwtPayload = {
  exp?: number
  realm_access?: {
    roles?: Array<string>
  }
}

function getApiBaseUrl() {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL
  }

  if (typeof window !== "undefined" && window.location.port === "3000") {
    return "http://localhost:8080"
  }

  return ""
}

async function parseApiError(response: Response) {
  try {
    const body = (await response.json()) as ApiErrorBody

    if (body.errors?.length) {
      return body.errors.map((error) => error.message).filter(Boolean).join(", ")
    }

    return body.message ?? "Request failed"
  } catch {
    return "Request failed"
  }
}

function decodeJwtPayload(token: string): JwtPayload | null {
  const [, payload] = token.split(".")

  if (!payload) {
    return null
  }

  try {
    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/")
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      "=",
    )
    const decodedPayload = atob(paddedPayload)

    return JSON.parse(decodedPayload) as JwtPayload
  } catch {
    return null
  }
}

function isTokenUsable(token: string) {
  const payload = decodeJwtPayload(token)

  if (!payload || typeof payload.exp !== "number") {
    return false
  }

  return payload.exp * 1000 > Date.now()
}

function getRoleFromToken(token: string): AuthSession["role"] {
  const payload = decodeJwtPayload(token)
  const roles = payload?.realm_access?.roles ?? []

  return roles.includes("MERCHANT") ? "MERCHANT" : "CUSTOMER"
}

export async function signinRequest(payload: SigninPayload) {
  const response = await fetch(`${getApiBaseUrl()}/api/users/signin`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error(await parseApiError(response))
  }

  return (await response.json()) as SigninResponse
}

export async function signupRequest(payload: SignupPayload) {
  const response = await fetch(`${getApiBaseUrl()}/api/users/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error(await parseApiError(response))
  }

  return (await response.json()) as {
    id: string
    username: string
    email: string
    firstName: string
    lastName: string
    createdAt: string
  }
}

async function refreshAccessTokenServer(refreshToken: string) {
  const response = await fetch(`${getApiBaseUrl()}/api/users/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })

  if (!response.ok) {
    throw new Error("refresh failed")
  }

  return (await response.json()) as SigninResponse
}

export const getAuthSession = createServerFn({ method: "GET" }).handler(
  async () => {
    let accessToken = getCookie(ACCESS_TOKEN_COOKIE)
    const refreshToken = getCookie(REFRESH_TOKEN_COOKIE)
    let username = getCookie(USERNAME_COOKIE) ?? null

    if (!accessToken || !isTokenUsable(accessToken)) {
      if (refreshToken) {
        try {
          const tokens = await refreshAccessTokenServer(refreshToken)
          if (tokens.access_token) {
            setCookie(ACCESS_TOKEN_COOKIE, tokens.access_token, accessCookieOptions)
            accessToken = tokens.access_token
            if (tokens.refresh_token) {
              setCookie(
                REFRESH_TOKEN_COOKIE,
                tokens.refresh_token,
                refreshCookieOptions,
              )
            }
            const payload = decodeJwtPayload(tokens.access_token)
            const refreshedUsername =
              (payload as { preferred_username?: string } | null)
                ?.preferred_username
            if (refreshedUsername) {
              setCookie(USERNAME_COOKIE, refreshedUsername, refreshCookieOptions)
              username = refreshedUsername
            }
          }
        } catch {
          accessToken = undefined
        }
      }
    }

    if (!accessToken || !isTokenUsable(accessToken)) {
      deleteCookie(ACCESS_TOKEN_COOKIE, { path: "/" })
      deleteCookie(REFRESH_TOKEN_COOKIE, { path: "/" })
      deleteCookie(USERNAME_COOKIE, { path: "/" })

      return {
        isAuthenticated: false,
        role: "CUSTOMER",
        username: null,
      } satisfies AuthSession
    }

    return {
      isAuthenticated: true,
      role: getRoleFromToken(accessToken),
      username,
    } satisfies AuthSession
  },
)

export const persistAuthSession = createServerFn({ method: "POST" })
  .inputValidator((data: PersistSessionInput) => data)
  .handler(({ data }) => {
    setCookie(ACCESS_TOKEN_COOKIE, data.accessToken, accessCookieOptions)
    setCookie(USERNAME_COOKIE, data.username, refreshCookieOptions)

    if (data.refreshToken) {
      setCookie(REFRESH_TOKEN_COOKIE, data.refreshToken, refreshCookieOptions)
    }

    return { success: true }
  })

export const clearAuthSession = createServerFn({ method: "POST" }).handler(() => {
  deleteCookie(ACCESS_TOKEN_COOKIE, { path: "/" })
  deleteCookie(REFRESH_TOKEN_COOKIE, { path: "/" })
  deleteCookie(USERNAME_COOKIE, { path: "/" })

  return { success: true }
})
