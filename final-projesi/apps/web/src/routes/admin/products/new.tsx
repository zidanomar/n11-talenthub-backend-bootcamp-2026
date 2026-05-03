import { useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { ArrowLeft, Loader2, Save } from "lucide-react"

import { getAuthSession } from "@/lib/auth"
import { createProduct } from "@/lib/products"
import {
  AlertMessage,
  Breadcrumbs,
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

export const Route = createFileRoute("/admin/products/new")({
  loader: async ({ location }) => {
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

    return { auth }
  },
  component: AdminProductCreatePage,
})

function AdminProductCreatePage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<ProductFormState>({
    name: "",
    description: "",
    price: "",
    category: "",
    brand: "",
    stock: "0",
  })
  const [isSaving, setIsSaving] = useState(false)
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
    setErrorMessage(null)

    try {
      const created = await createProduct({
        name: form.name.trim(),
        description: form.description.trim(),
        price,
        category: form.category.trim(),
        brand: form.brand.trim(),
        stock,
      })
      await navigate({
        to: "/admin/products/$productId",
        params: { productId: String(created.id) },
      })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Product could not be created."
      )
    } finally {
      setIsSaving(false)
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
        </div>
      </header>

      <PageShell>
        <Breadcrumbs
          className="mb-5"
          items={[
            { label: "Admin", href: "/admin" },
            { label: "Products", href: "/admin/products" },
            { label: "New" },
          ]}
        />
        <Panel className="p-5">
          <div className="mb-5">
            <p className="text-xs font-semibold text-neutral-500 uppercase">
              New Product
            </p>
            <h1 className="mt-1 text-2xl font-semibold">Create Product</h1>
            <p className="mt-1 text-sm text-neutral-500">
              Save first, then upload images on the detail page.
            </p>
          </div>

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
              <span>{isSaving ? "Creating" : "Create Product"}</span>
            </Button>
          </div>
        </Panel>
      </PageShell>
    </main>
  )
}
