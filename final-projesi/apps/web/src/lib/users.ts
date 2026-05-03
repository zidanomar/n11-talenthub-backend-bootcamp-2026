import { createServerFn } from "@tanstack/react-start"
import { deleteCookie, getCookie } from "@tanstack/react-start/server"

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  USERNAME_COOKIE,
} from "@/lib/auth"

export type Address = {
  id: number
  label: string | null
  phone: string
  identityNumber: string
  addressLine: string
  city: string
  country: string
  zipCode: string
  createdAt: string
}

export type SavedCard = {
  id: number
  label: string | null
  cardHolderName: string
  lastFourDigits: string
  expiryMonth: string
  expiryYear: string
  cardType: string | null
  iyzipayCardToken: string | null
  iyzipayCardUserKey: string | null
  createdAt: string
}

export type SaveCardInput = {
  label?: string
  cardHolderName: string
  lastFourDigits: string
  expiryMonth: string
  expiryYear: string
  cardType?: string
  iyzipayCardToken?: string
  iyzipayCardUserKey?: string
}

export type AddressInput = {
  label: string
  phone: string
  identityNumber: string
  addressLine: string
  city: string
  country: string
  zipCode: string
}

export type UserProfile = {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  role: "CUSTOMER" | "MERCHANT"
  currentAddressId: number | null
  addresses: Array<Address>
  defaultCardId: number | null
  cards: Array<SavedCard>
  phone: string | null
  identityNumber: string | null
  addressLine: string | null
  city: string | null
  country: string | null
  zipCode: string | null
  createdAt: string
}

export type CurrentAddressSummary = {
  label: string | null
  addressLine: string
  city: string
  country: string
  zipCode: string
}

const UNAUTHORIZED_ERROR = "UNAUTHORIZED"

type ApiErrorBody = {
  message?: string
  errors?: Array<{ field?: string; message?: string }> | Record<string, string>
}

async function parseApiError(response: Response) {
  try {
    const body = (await response.json()) as ApiErrorBody

    if (Array.isArray(body.errors) && body.errors.length > 0) {
      return body.errors.map((error) => error.message).filter(Boolean).join(", ")
    }

    if (body.errors && !Array.isArray(body.errors)) {
      return Object.entries(body.errors)
        .map(([field, message]) => `${field}: ${message}`)
        .join(", ")
    }

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

async function authorizedUserRequest(path: string, init?: RequestInit) {
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

const fetchUserProfileServerFn = createServerFn({ method: "GET" }).handler(
  async () => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest("/api/users/me")

      return (await response.json()) as UserProfile
    })
  },
)

export async function fetchUserProfile() {
  return fetchUserProfileServerFn()
}

const fetchAddressesServerFn = createServerFn({ method: "GET" }).handler(async () => {
  return withAuthRecovery(async () => {
    const response = await authorizedUserRequest("/api/users/me/addresses")

    return (await response.json()) as Array<Address>
  })
})

export async function fetchAddresses() {
  return fetchAddressesServerFn()
}

const createAddressServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: AddressInput) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest("/api/users/me/addresses", {
        method: "POST",
        body: JSON.stringify(normalizeAddressInput(data)),
      })

      return (await response.json()) as Address
    })
  })

export async function createAddress(data: AddressInput) {
  return createAddressServerFn({ data })
}

const updateAddressServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { addressId: number; values: AddressInput }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest(
        `/api/users/me/addresses/${data.addressId}`,
        {
          method: "PUT",
          body: JSON.stringify(normalizeAddressInput(data.values)),
        },
      )

      return (await response.json()) as Address
    })
  })

export async function updateAddress(addressId: number, values: AddressInput) {
  return updateAddressServerFn({ data: { addressId, values } })
}

const deleteAddressServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { addressId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      await authorizedUserRequest(`/api/users/me/addresses/${data.addressId}`, {
        method: "DELETE",
      })

      return { success: true }
    })
  })

export async function deleteAddress(addressId: number) {
  return deleteAddressServerFn({ data: { addressId } })
}

const setCurrentAddressServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { addressId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest(
        `/api/users/me/addresses/${data.addressId}/current`,
        {
          method: "PATCH",
        },
      )

      return (await response.json()) as UserProfile
    })
  })

export async function setCurrentAddress(addressId: number) {
  return setCurrentAddressServerFn({ data: { addressId } })
}

const saveCardServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: SaveCardInput) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest("/api/users/me/cards", {
        method: "POST",
        body: JSON.stringify(data),
      })

      return (await response.json()) as SavedCard
    })
  })

export async function saveCard(data: SaveCardInput) {
  return saveCardServerFn({ data })
}

const deleteCardServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { cardId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      await authorizedUserRequest(`/api/users/me/cards/${data.cardId}`, {
        method: "DELETE",
      })

      return { success: true }
    })
  })

export async function deleteCard(cardId: number) {
  return deleteCardServerFn({ data: { cardId } })
}

const setDefaultCardServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { cardId: number }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      const response = await authorizedUserRequest(
        `/api/users/me/cards/${data.cardId}/default`,
        { method: "PATCH" },
      )

      return (await response.json()) as UserProfile
    })
  })

export async function setDefaultCard(cardId: number) {
  return setDefaultCardServerFn({ data: { cardId } })
}

const changePasswordServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { oldPassword: string; newPassword: string }) => data)
  .handler(async ({ data }) => {
    return withAuthRecovery(async () => {
      await authorizedUserRequest("/api/users/me/password", {
        method: "POST",
        body: JSON.stringify(data),
      })

      return { success: true }
    })
  })

export async function changePassword(input: {
  oldPassword: string
  newPassword: string
}) {
  return changePasswordServerFn({ data: input })
}

export function isUnauthorizedError(error: unknown) {
  return error instanceof Error && error.message === UNAUTHORIZED_ERROR
}

export function getCurrentAddressSummary(
  profile: UserProfile,
): CurrentAddressSummary | null {
  const currentAddress = profile.addresses.find(
    (address) => address.id === profile.currentAddressId,
  )

  if (currentAddress) {
    return {
      label: currentAddress.label,
      addressLine: currentAddress.addressLine,
      city: currentAddress.city,
      country: currentAddress.country,
      zipCode: currentAddress.zipCode,
    }
  }

  if (
    profile.addressLine &&
    profile.city &&
    profile.country &&
    profile.zipCode
  ) {
    return {
      label: null,
      addressLine: profile.addressLine,
      city: profile.city,
      country: profile.country,
      zipCode: profile.zipCode,
    }
  }

  return null
}

function normalizeAddressInput(input: AddressInput) {
  return {
    ...input,
    label: input.label.trim(),
    phone: input.phone.trim(),
    identityNumber: input.identityNumber.trim(),
    addressLine: input.addressLine.trim(),
    city: input.city.trim(),
    country: input.country.trim(),
    zipCode: input.zipCode.trim(),
  }
}
