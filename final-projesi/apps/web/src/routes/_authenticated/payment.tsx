import { useMemo, useState } from "react"
import {
  Link,
  createFileRoute,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import {
  Check,
  Home,
  MapPin,
  PackageCheck,
  Plus,
  ShieldCheck,
  X,
} from "lucide-react"

import type { Address, AddressInput, UserProfile } from "@/lib/users"
import {
  fetchCart,
  isUnauthorizedError as isCartUnauthorizedError,
} from "@/lib/cart"
import { payOrder, placeOrder } from "@/lib/orders"
import { formatPrice } from "@/lib/products"
import {
  createAddress,
  fetchUserProfile,
  getCurrentAddressSummary,
  isUnauthorizedError as isUserUnauthorizedError,
  setCurrentAddress,
} from "@/lib/users"
import { cn } from "@/lib/utils"
import {
  AlertMessage,
  Breadcrumbs,
  PageShell,
  Panel,
} from "@/components/app/page"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"

const emptyAddressForm: AddressInput = {
  label: "",
  phone: "",
  identityNumber: "",
  addressLine: "",
  city: "",
  country: "Türkiye",
  zipCode: "",
}

export const Route = createFileRoute("/_authenticated/payment")({
  loader: async ({ location }) => {
    try {
      const [cart, profile] = await Promise.all([
        fetchCart(),
        fetchUserProfile(),
      ])

      if (cart.items.length === 0) {
        throw redirect({ to: "/cart" })
      }

      return { cart, profile }
    } catch (error) {
      if (isCartUnauthorizedError(error) || isUserUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: location.href },
        })
      }

      throw error
    }
  },
  component: PaymentPage,
})

function PaymentPage() {
  const navigate = useNavigate()
  const { cart, profile } = Route.useLoaderData()
  const [currentProfile, setCurrentProfile] = useState<UserProfile>(profile)
  const [isAddressDrawerOpen, setIsAddressDrawerOpen] = useState(false)
  const [termsAccepted, setTermsAccepted] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const deliveryAddress = getCurrentAddressSummary(currentProfile)
  const summary = useMemo(
    () => buildSummary(cart.grandTotal),
    [cart.grandTotal]
  )
  const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)

  async function handleCompletePayment() {
    setIsSubmitting(true)
    setSubmitError(null)

    if (!deliveryAddress) {
      setSubmitError("Please add or select a delivery address before payment.")
      setIsSubmitting(false)
      return
    }

    try {
      const order = await placeOrder()
      const { paymentPageUrl } = await payOrder(order.id, {
        forceThreeDS: true,
        paymentWithNewCardEnabled: true,
        savedCardId: null,
      })
      window.location.href = paymentPageUrl
    } catch (error) {
      if (isCartUnauthorizedError(error) || isUserUnauthorizedError(error)) {
        await navigate({
          to: "/login",
          search: { mode: "signin", redirect: "/payment" },
        })
        return
      }

      setSubmitError(error instanceof Error ? error.message : "Payment failed")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <header className="bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 lg:px-6">
          <Link
            className="text-4xl font-black tracking-tight text-neutral-950"
            to="/"
          >
            n11
          </Link>
          <div className="flex items-center gap-2 text-lg font-semibold">
            <span>Secure Payment</span>
            <ShieldCheck className="h-6 w-6 fill-neutral-950 text-neutral-950" />
          </div>
        </div>
      </header>

      <PageShell className="py-5">
        <Breadcrumbs
          className="mb-4"
          items={[
            { label: "Home", href: "/" },
            { label: "Cart", href: "/cart" },
            { label: "Payment" },
          ]}
        />

        <div className="mt-4 grid gap-5 lg:grid-cols-[minmax(0,1fr)_280px]">
          <div className="space-y-5">
            <DeliveryAddressPanel
              address={deliveryAddress}
              onChangeAddress={() => setIsAddressDrawerOpen(true)}
            />
            <OrderItemsPanel items={cart.items} />
          </div>

          <aside className="space-y-5 lg:sticky lg:top-5 lg:self-start">
            <OrderSummary
              isSubmitting={isSubmitting}
              itemCount={itemCount}
              onComplete={() => void handleCompletePayment()}
              setTermsAccepted={setTermsAccepted}
              submitError={submitError}
              summary={summary}
              termsAccepted={termsAccepted}
            />
          </aside>
        </div>
      </PageShell>

      {isAddressDrawerOpen ? (
        <AddressDrawer
          currentAddressId={currentProfile.currentAddressId}
          addresses={currentProfile.addresses}
          onClose={() => setIsAddressDrawerOpen(false)}
          onProfileChange={setCurrentProfile}
        />
      ) : null}
    </main>
  )
}

function DeliveryAddressPanel({
  address,
  onChangeAddress,
}: {
  address: ReturnType<typeof getCurrentAddressSummary>
  onChangeAddress: () => void
}) {
  return (
    <Panel className="px-6 py-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <MapPin className="h-5 w-5 text-brand-600" />
            <h1 className="text-lg font-semibold">Delivery Address</h1>
          </div>
          {address ? (
            <p className="mt-3 text-sm leading-6 text-neutral-600">
              <span className="font-semibold text-neutral-950">
                {address.label ?? "Selected address"}
              </span>{" "}
              - {address.addressLine} {address.city} / {address.country}
            </p>
          ) : (
            <p className="mt-3 text-sm text-neutral-600">
              No delivery address selected.
            </p>
          )}
        </div>
        <Button
          className="shrink-0"
          onClick={onChangeAddress}
          type="button"
          variant="outline"
        >
          <Plus className="h-4 w-4" />
          <span>{address ? "Change / Add Address" : "Add Address"}</span>
        </Button>
      </div>
    </Panel>
  )
}

function OrderItemsPanel({
  items,
}: {
  items: Array<{
    productId: number
    name: string | null
    quantity: number
    unitPrice: number
    totalPrice: number
  }>
}) {
  return (
    <Panel className="p-6">
      <div className="flex items-center gap-2">
        <PackageCheck className="h-5 w-5 text-brand-600" />
        <h2 className="text-lg font-semibold">Order Item Details</h2>
      </div>

      <div className="mt-5 divide-y divide-neutral-100">
        {items.map((item) => (
          <article
            className="grid gap-3 py-4 first:pt-0 last:pb-0 sm:grid-cols-[minmax(0,1fr)_auto]"
            key={item.productId}
          >
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">
                {item.name ?? `Product #${item.productId}`}
              </p>
              <p className="mt-1 text-xs text-neutral-500">
                Product #{item.productId} • Quantity: {item.quantity}
              </p>
            </div>
            <div className="sm:text-right">
              <p className="text-sm font-semibold">
                {formatPrice(item.totalPrice)}
              </p>
              <p className="mt-1 text-xs text-neutral-500">
                {formatPrice(item.unitPrice)} each
              </p>
            </div>
          </article>
        ))}
      </div>
    </Panel>
  )
}

function OrderSummary({
  isSubmitting,
  itemCount,
  onComplete,
  setTermsAccepted,
  submitError,
  summary,
  termsAccepted,
}: {
  isSubmitting: boolean
  itemCount: number
  onComplete: () => void
  setTermsAccepted: (value: boolean) => void
  submitError: string | null
  summary: ReturnType<typeof buildSummary>
  termsAccepted: boolean
}) {
  return (
    <Panel className="p-6">
      <h2 className="text-lg font-semibold">
        Order Summary{" "}
        <span className="text-sm font-normal">({itemCount} items)</span>
      </h2>

      <div className="mt-5 space-y-3 text-sm">
        <SummaryLine
          label="Order Amount"
          value={formatPrice(summary.subtotal)}
        />
        <SummaryLine
          label="Shipping Amount"
          value={formatPrice(summary.shipping)}
        />
        <SummaryLine
          label="Shipping Discount"
          value={`-${formatPrice(summary.shippingDiscount)}`}
        />
      </div>

      <div className="mt-4 border-t border-neutral-200 pt-4">
        <SummaryLine
          label="Amount to Pay"
          large
          value={formatPrice(summary.total)}
        />
      </div>

      <CheckboxRow
        checked={termsAccepted}
        className="mt-5 items-start text-xs leading-5"
        label="I have read and approve the Preliminary Information Form and Distance Sales Agreement."
        onChange={setTermsAccepted}
      />

      {submitError ? (
        <AlertMessage className="mt-3">{submitError}</AlertMessage>
      ) : null}

      <Button
        className="mt-5 w-full"
        disabled={!termsAccepted || isSubmitting}
        onClick={onComplete}
        size="xl"
        type="button"
      >
        {isSubmitting ? "Processing..." : "Complete Payment"}
      </Button>
    </Panel>
  )
}

function AddressDrawer({
  addresses,
  currentAddressId,
  onClose,
  onProfileChange,
}: {
  addresses: Array<Address>
  currentAddressId: number | null
  onClose: () => void
  onProfileChange: (profile: UserProfile) => void
}) {
  const [form, setForm] = useState<AddressInput>(emptyAddressForm)
  const [isAddingAddress, setIsAddingAddress] = useState(addresses.length === 0)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function updateForm<TKey extends keyof AddressInput>(
    key: TKey,
    value: AddressInput[TKey]
  ) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function selectAddress(addressId: number) {
    setIsSaving(true)
    setError(null)

    try {
      const updated = await setCurrentAddress(addressId)
      onProfileChange(updated)
      onClose()
    } catch (caughtError) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Address could not be selected."
      )
    } finally {
      setIsSaving(false)
    }
  }

  async function createAndSelectAddress() {
    setIsSaving(true)
    setError(null)

    try {
      const created = await createAddress(form)
      const updated = await setCurrentAddress(created.id)
      onProfileChange(updated)
      onClose()
    } catch (caughtError) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Address could not be saved."
      )
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-neutral-950/70">
      <aside className="ml-auto flex h-full w-full max-w-lg flex-col bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-neutral-200 px-6 py-5">
          <h2 className="text-xl font-semibold">Delivery Address</h2>
          <button
            aria-label="Close address drawer"
            onClick={onClose}
            type="button"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          <Button
            className="w-full"
            onClick={() => setIsAddingAddress((current) => !current)}
            size="xl"
            type="button"
            variant="outline"
          >
            <Plus className="h-5 w-5" />
            <span>
              {isAddingAddress ? "Choose Saved Address" : "Add New Address"}
            </span>
          </Button>

          {error ? <AlertMessage className="mt-4">{error}</AlertMessage> : null}

          {isAddingAddress ? (
            <AddressForm
              form={form}
              isSaving={isSaving}
              onSave={() => void createAndSelectAddress()}
              updateForm={updateForm}
            />
          ) : (
            <div className="mt-5 space-y-3">
              {addresses.map((address) => (
                <button
                  className={cn(
                    "w-full rounded-lg border p-4 text-left",
                    currentAddressId === address.id
                      ? "border-brand bg-brand-50"
                      : "border-neutral-200 bg-white"
                  )}
                  disabled={isSaving}
                  key={address.id}
                  onClick={() => void selectAddress(address.id)}
                  type="button"
                >
                  <div className="flex items-start gap-3">
                    <Home className="mt-0.5 h-5 w-5 shrink-0 text-neutral-500" />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="font-semibold">
                          {address.label ?? "Address"}
                        </p>
                        {currentAddressId === address.id ? (
                          <Badge variant="brand">Selected</Badge>
                        ) : null}
                      </div>
                      <p className="mt-1 text-sm leading-6 text-neutral-600">
                        {address.addressLine}, {address.city}, {address.zipCode}
                        , {address.country}
                      </p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </aside>
    </div>
  )
}

function AddressForm({
  form,
  isSaving,
  onSave,
  updateForm,
}: {
  form: AddressInput
  isSaving: boolean
  onSave: () => void
  updateForm: <TKey extends keyof AddressInput>(
    key: TKey,
    value: AddressInput[TKey]
  ) => void
}) {
  return (
    <div className="mt-5 grid gap-3">
      <Input
        className="h-12"
        onChange={(event) => updateForm("label", event.target.value)}
        placeholder="Address label"
        value={form.label}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          className="h-12"
          onChange={(event) => updateForm("phone", event.target.value)}
          placeholder="Phone"
          value={form.phone}
        />
        <Input
          className="h-12"
          onChange={(event) => updateForm("identityNumber", event.target.value)}
          placeholder="Identity number"
          value={form.identityNumber}
        />
      </div>
      <textarea
        className="min-h-24 rounded-lg border border-neutral-300 px-3 py-3 text-sm outline-none focus:border-neutral-950"
        onChange={(event) => updateForm("addressLine", event.target.value)}
        placeholder="Address line"
        value={form.addressLine}
      />
      <div className="grid gap-3 sm:grid-cols-3">
        <Input
          className="h-12"
          onChange={(event) => updateForm("city", event.target.value)}
          placeholder="City"
          value={form.city}
        />
        <Input
          className="h-12"
          onChange={(event) => updateForm("country", event.target.value)}
          placeholder="Country"
          value={form.country}
        />
        <Input
          className="h-12"
          onChange={(event) => updateForm("zipCode", event.target.value)}
          placeholder="Zip code"
          value={form.zipCode}
        />
      </div>
      <Button
        className="mt-2 w-full"
        disabled={isSaving}
        onClick={onSave}
        size="xl"
        type="button"
      >
        {isSaving ? "Saving..." : "Save and Use Address"}
      </Button>
    </div>
  )
}

function CheckboxRow({
  checked,
  className,
  label,
  onChange,
}: {
  checked: boolean
  label: string
  className?: string
  onChange: (value: boolean) => void
}) {
  return (
    <label className={cn("flex items-center gap-3 text-sm", className)}>
      <span
        className={cn(
          "flex h-5 w-5 shrink-0 items-center justify-center rounded border",
          checked
            ? "border-neutral-950 bg-neutral-950 text-white"
            : "border-neutral-300"
        )}
      >
        {checked ? <Check className="h-3.5 w-3.5" /> : null}
      </span>
      <input
        checked={checked}
        className="sr-only"
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      <span>{label}</span>
    </label>
  )
}

function SummaryLine({
  label,
  large,
  value,
}: {
  label: string
  value: string
  large?: boolean
}) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-4",
        large ? "text-base" : ""
      )}
    >
      <span className={cn(large ? "font-semibold" : "text-neutral-700")}>
        {label}
      </span>
      <span className={cn("font-semibold", large ? "text-lg" : "")}>
        {value}
      </span>
    </div>
  )
}

function buildSummary(total: number) {
  const shipping = 49.9
  const shippingDiscount = 49.9

  return {
    shipping,
    shippingDiscount,
    subtotal: total,
    total,
  }
}
