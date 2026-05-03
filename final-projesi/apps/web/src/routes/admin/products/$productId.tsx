import { useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { ArrowLeft, ImagePlus, Loader2, Save, Trash2 } from "lucide-react"

import { getAuthSession } from "@/lib/auth"
import {
  deleteProductImage,
  fetchProduct,
  formatPrice,
  getImageUrl,
  updateProduct,
  uploadProductImage,
} from "@/lib/products"
import {
  AlertMessage,
  Breadcrumbs,
  EmptyState,
  PageShell,
  Panel,
} from "@/components/app/page"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

type ProductFormState = {
  name: string
  description: string
  price: string
  category: string
  brand: string
  stock: string
}

export const Route = createFileRoute("/admin/products/$productId")({
  loader: async ({ params, location }) => {
    const auth = await getAuthSession()

    if (!auth.isAuthenticated) {
      throw redirect({
        to: "/login",
        search: { mode: "signin", redirect: location.href },
      })
    }

    if (auth.role !== "MERCHANT") {
      throw redirect({ to: "/" })
    }

    const product = await fetchProduct(params.productId)

    return { product }
  },
  component: AdminProductDetailPage,
})

function AdminProductDetailPage() {
  const navigate = useNavigate()
  const { product } = Route.useLoaderData()
  const [form, setForm] = useState<ProductFormState>({
    name: product.name,
    description: product.description,
    price: String(product.price),
    category: product.category,
    brand: product.brand,
    stock: String(product.stock),
  })
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [deletingImage, setDeletingImage] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  function updateField(field: keyof ProductFormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function saveProduct() {
    const price = Number(form.price)
    const stock = Number(form.stock)

    if (!form.name.trim() || !form.category.trim()) {
      setErrorMessage("Name and category are required.")
      return
    }

    if (!Number.isFinite(price) || price < 0) {
      setErrorMessage("Price must be a non-negative number.")
      return
    }

    if (!Number.isInteger(stock) || stock < 0) {
      setErrorMessage("Stock must be a non-negative whole number.")
      return
    }

    setIsSaving(true)
    setMessage(null)
    setErrorMessage(null)

    try {
      await updateProduct(product.id, {
        name: form.name.trim(),
        description: form.description.trim(),
        price,
        category: form.category.trim(),
        brand: form.brand.trim(),
        stock,
      })
      setMessage("Product updated.")
      await navigate({
        to: "/admin/products/$productId",
        params: { productId: String(product.id) },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Product could not be updated."
      )
    } finally {
      setIsSaving(false)
    }
  }

  async function removeImage(filename: string) {
    setDeletingImage(filename)
    setMessage(null)
    setErrorMessage(null)

    try {
      await deleteProductImage(product.id, filename)
      setMessage("Image deleted.")
      await navigate({
        to: "/admin/products/$productId",
        params: { productId: String(product.id) },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Image could not be deleted."
      )
    } finally {
      setDeletingImage(null)
    }
  }

  async function uploadImage(file: File) {
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setErrorMessage("Upload a JPEG, PNG, or WebP image.")
      return
    }

    setIsUploading(true)
    setMessage(null)
    setErrorMessage(null)

    try {
      const base64 = await readFileAsBase64(file)
      await uploadProductImage({
        productId: product.id,
        filename: file.name,
        contentType: file.type,
        base64,
      })
      setMessage("Image uploaded.")
      await navigate({
        to: "/admin/products/$productId",
        params: { productId: String(product.id) },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Image could not be uploaded."
      )
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <header className="border-b border-neutral-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 lg:px-6">
          <Button
            onClick={() => void navigate({ to: "/admin/products" })}
            type="button"
            variant="outline"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Products</span>
          </Button>
          <p className="text-sm font-medium text-neutral-500">
            Current price {formatPrice(product.price)}
          </p>
        </div>
      </header>

      <PageShell>
        <Breadcrumbs
          className="mb-5"
          items={[
            { label: "Admin", href: "/admin" },
            { label: "Products", href: "/admin/products" },
            { label: product.name },
          ]}
        />
        <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
          <Panel className="p-5">
            <div className="mb-5">
              <p className="text-xs font-semibold text-neutral-500 uppercase">
                Product Detail
              </p>
              <h1 className="mt-1 text-2xl font-semibold">{product.name}</h1>
            </div>

            {message ? (
              <AlertMessage className="mb-4" tone="success">
                {message}
              </AlertMessage>
            ) : null}
            {errorMessage ? (
              <AlertMessage className="mb-4">{errorMessage}</AlertMessage>
            ) : null}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label>Name</Label>
                <Input
                  onChange={(event) => updateField("name", event.target.value)}
                  value={form.name}
                />
              </div>
              <div className="grid gap-2">
                <Label>Brand</Label>
                <Input
                  onChange={(event) => updateField("brand", event.target.value)}
                  value={form.brand}
                />
              </div>
              <div className="grid gap-2">
                <Label>Category</Label>
                <Input
                  onChange={(event) =>
                    updateField("category", event.target.value)
                  }
                  value={form.category}
                />
              </div>
              <div className="grid gap-2">
                <Label>Price</Label>
                <Input
                  min={0}
                  onChange={(event) => updateField("price", event.target.value)}
                  step="0.01"
                  type="number"
                  value={form.price}
                />
              </div>
              <div className="grid gap-2">
                <Label>Stock</Label>
                <Input
                  min={0}
                  onChange={(event) => updateField("stock", event.target.value)}
                  type="number"
                  value={form.stock}
                />
              </div>
              <div className="grid gap-2 sm:col-span-2">
                <Label>Description</Label>
                <textarea
                  className="min-h-36 rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm outline-none focus:border-neutral-900 focus:ring-2 focus:ring-neutral-900/10"
                  onChange={(event) =>
                    updateField("description", event.target.value)
                  }
                  value={form.description}
                />
              </div>
            </div>

            <div className="mt-5 flex justify-end">
              <Button
                disabled={isSaving}
                onClick={() => void saveProduct()}
                type="button"
              >
                {isSaving ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                <span>{isSaving ? "Saving" : "Save Product"}</span>
              </Button>
            </div>
          </Panel>

          <Panel className="p-5">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <p className="text-xs font-semibold text-neutral-500 uppercase">
                  Images
                </p>
                <h2 className="text-lg font-semibold">Gallery</h2>
              </div>
              <label className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-neutral-200 bg-white px-4 text-sm font-semibold text-neutral-800 transition hover:border-neutral-900">
                {isUploading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <ImagePlus className="h-4 w-4" />
                )}
                <span>{isUploading ? "Uploading" : "Upload"}</span>
                <input
                  accept="image/jpeg,image/png,image/webp"
                  className="sr-only"
                  disabled={isUploading}
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    event.target.value = ""
                    if (file) void uploadImage(file)
                  }}
                  type="file"
                />
              </label>
            </div>

            <div className="grid gap-3">
              {product.images.length > 0 ? (
                product.images.map((image) => (
                  <div className="relative" key={image}>
                    <img
                      alt={product.name}
                      className="aspect-[4/3] w-full rounded-lg border border-neutral-200 object-cover"
                      src={getImageUrl(image)}
                    />
                    <Button
                      aria-label="Delete image"
                      className="absolute top-2 right-2"
                      disabled={deletingImage === image}
                      onClick={() => void removeImage(image)}
                      size="icon"
                      type="button"
                      variant="destructive"
                    >
                      {deletingImage === image ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                ))
              ) : (
                <EmptyState
                  className="px-4 py-10"
                  description="Upload JPEG, PNG, or WebP images for this product."
                  icon={ImagePlus}
                  title="No images uploaded"
                />
              )}
            </div>
          </Panel>
        </div>
      </PageShell>
    </main>
  )
}

function readFileAsBase64(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error("File could not be read."))
    reader.onload = () => {
      const value = String(reader.result ?? "")
      resolve(value.includes(",") ? value.split(",")[1] : value)
    }
    reader.readAsDataURL(file)
  })
}
