import { createServerFn } from "@tanstack/react-start"
import { getCookie } from "@tanstack/react-start/server"

import { ACCESS_TOKEN_COOKIE } from "@/lib/auth"

export type Product = {
  id: number
  name: string
  description: string
  price: number
  category: string
  brand: string
  rating: number
  reviewCount: number
  images: Array<string>
  stock: number
}

type ProductsPage = {
  content: Array<Product>
  totalElements: number
  totalPages: number
  number: number
  size: number
}

type FetchProductsOptions = {
  brands?: Array<string>
  category?: string
  inStock?: boolean
  limit?: number
  maxPrice?: number
  minRating?: number
  minPrice?: number
  page?: number
  query?: string
  sort?: string
}

export type UpdateProductInput = {
  name: string
  description: string
  price: number
  category: string
  brand: string
  stock: number
}

export type UploadProductImageInput = {
  productId: number
  filename: string
  contentType: string
  base64: string
}

export type ProductFilters = {
  categories: Array<string>
  brands: Array<string>
}

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

const NOT_FOUND_ERROR = "NOT_FOUND"

async function publicApiRequest(path: string) {
  const response = await fetch(`${getInternalApiBaseUrl()}${path}`, {
    headers: {},
  })

  if (response.status === 404) {
    throw new Error(NOT_FOUND_ERROR)
  }

  if (!response.ok) {
    throw new Error(await parseApiError(response))
  }

  return response
}

export function isNotFoundError(error: unknown) {
  return error instanceof Error && error.message === NOT_FOUND_ERROR
}

async function authorizedProductRequest(path: string, init?: RequestInit) {
  const token = getCookie(ACCESS_TOKEN_COOKIE)

  if (!token) {
    throw new Error("UNAUTHORIZED")
  }

  const response = await fetch(`${getInternalApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    throw new Error(await parseApiError(response))
  }

  return response
}

const fetchProductsServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: FetchProductsOptions | undefined) => data)
  .handler(async ({ data }) => {
    const url = new URL("/api/products", getInternalApiBaseUrl())

    url.searchParams.set("limit", String(data?.limit ?? 24))
    url.searchParams.set("page", String(data?.page ?? 0))

    if (data?.category && data.category !== "All") {
      url.searchParams.set("category", data.category)
    }

    if (data?.query?.trim()) {
      url.searchParams.set("q", data.query.trim())
    }

    data?.brands?.forEach((brand) => {
      url.searchParams.append("brands", brand)
    })

    if (data?.minRating && data.minRating > 0) {
      url.searchParams.set("minRating", String(data.minRating))
    }

    if (typeof data?.minPrice === "number" && data.minPrice >= 0) {
      url.searchParams.set("minPrice", String(data.minPrice))
    }

    if (typeof data?.maxPrice === "number" && data.maxPrice >= 0) {
      url.searchParams.set("maxPrice", String(data.maxPrice))
    }

    if (data?.inStock) {
      url.searchParams.set("inStock", "true")
    }

    if (data?.sort) {
      url.searchParams.set("sort", data.sort)
    }

    const response = await publicApiRequest(`${url.pathname}${url.search}`)

    return (await response.json()) as ProductsPage
  })

export async function fetchProducts(options?: FetchProductsOptions | string) {
  const data = typeof options === "string" ? { category: options } : options

  return fetchProductsServerFn({
    data,
  })
}

const fetchProductFiltersServerFn = createServerFn({ method: "GET" }).handler(
  async () => {
    const response = await publicApiRequest("/api/products/filters")

    return (await response.json()) as ProductFilters
  }
)

export async function fetchProductFilters() {
  return fetchProductFiltersServerFn()
}

const fetchProductsByIdsServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: { ids: Array<number> }) => data)
  .handler(async ({ data }) => {
    const url = new URL("/api/products", getInternalApiBaseUrl())

    data.ids.forEach((id) => {
      url.searchParams.append("ids", String(id))
    })

    const response = await publicApiRequest(`${url.pathname}${url.search}`)

    return (await response.json()) as ProductsPage
  })

export async function fetchProductsByIds(ids: Array<number>) {
  return fetchProductsByIdsServerFn({ data: { ids } })
}

const fetchProductServerFn = createServerFn({ method: "GET" })
  .inputValidator((data: { productId: string }) => data)
  .handler(async ({ data }) => {
    const response = await publicApiRequest(`/api/products/${data.productId}`)

    return (await response.json()) as Product
  })

export async function fetchProduct(productId: string) {
  return fetchProductServerFn({ data: { productId } })
}

const updateProductStockServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { productId: number; stock: number }) => data)
  .handler(async ({ data }) => {
    const response = await authorizedProductRequest(
      `/api/products/${data.productId}/stock/set`,
      {
        method: "PATCH",
        body: JSON.stringify({ stock: data.stock }),
      }
    )

    return (await response.json()) as Product
  })

export async function updateProductStock(productId: number, stock: number) {
  return updateProductStockServerFn({ data: { productId, stock } })
}

const createProductServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: UpdateProductInput) => data)
  .handler(async ({ data }) => {
    const response = await authorizedProductRequest("/api/products", {
      method: "POST",
      body: JSON.stringify(data),
    })

    return (await response.json()) as Product
  })

export async function createProduct(values: UpdateProductInput) {
  return createProductServerFn({ data: values })
}

const updateProductServerFn = createServerFn({ method: "POST" })
  .inputValidator(
    (data: { productId: number; values: UpdateProductInput }) => data
  )
  .handler(async ({ data }) => {
    const response = await authorizedProductRequest(
      `/api/products/${data.productId}`,
      {
        method: "PATCH",
        body: JSON.stringify(data.values),
      }
    )

    return (await response.json()) as Product
  })

export async function updateProduct(
  productId: number,
  values: UpdateProductInput
) {
  return updateProductServerFn({ data: { productId, values } })
}

const uploadProductImageServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: UploadProductImageInput) => data)
  .handler(async ({ data }) => {
    const token = getCookie(ACCESS_TOKEN_COOKIE)

    if (!token) {
      throw new Error("UNAUTHORIZED")
    }

    const formData = new FormData()
    const bytes = Buffer.from(data.base64, "base64")
    formData.set(
      "image",
      new File([bytes], data.filename, { type: data.contentType })
    )

    const response = await fetch(
      `${getInternalApiBaseUrl()}/api/products/${data.productId}/images`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      }
    )

    if (!response.ok) {
      throw new Error(await parseApiError(response))
    }

    return (await response.json()) as Product
  })

export async function uploadProductImage(data: UploadProductImageInput) {
  return uploadProductImageServerFn({ data })
}

const deleteProductImageServerFn = createServerFn({ method: "POST" })
  .inputValidator((data: { productId: number; filename: string }) => data)
  .handler(async ({ data }) => {
    const response = await authorizedProductRequest(
      `/api/products/${data.productId}/images/${encodeURIComponent(data.filename)}`,
      { method: "DELETE" }
    )

    return (await response.json()) as Product
  })

export async function deleteProductImage(productId: number, filename: string) {
  return deleteProductImageServerFn({ data: { productId, filename } })
}

export function getImageUrl(filename?: string) {
  if (!filename) {
    return undefined
  }

  if (/^(https?:)?\/\//.test(filename) || filename.startsWith("/")) {
    return filename
  }

  return `/api/products/images/${filename}`
}

export function formatPrice(price: number) {
  return `₺${price.toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

export function buildReviewPreview(product: Product) {
  const snippets = [
    {
      title: "Great quality",
      body: `${product.brand} delivered exactly what I expected. The material feels solid and the finish looks great in person.`,
    },
    {
      title: "Worth the price",
      body: `I bought this for everyday use and it has been reliable so far. I would happily recommend it.`,
    },
    {
      title: "Looks even better in person",
      body: `The product matches the photos closely and arrived in good condition. Overall a smooth experience.`,
    },
  ]

  return snippets.map((snippet, index) => ({
    id: `${product.id}-${index}`,
    author: `Customer ${index + 1}`,
    rating: Math.max(4, Math.round(product.rating)),
    date: ["Apr 25, 2026", "Apr 21, 2026", "Apr 18, 2026"][index],
    ...snippet,
  }))
}
