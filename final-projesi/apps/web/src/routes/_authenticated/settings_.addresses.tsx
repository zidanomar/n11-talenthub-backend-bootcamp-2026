import { useMemo, useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import {
  Check,
  Edit3,
  Home,
  Loader2,
  MapPin,
  Plus,
  Star,
  Trash2,
} from "lucide-react"

import type { Address, AddressInput, UserProfile } from "@/lib/users"
import {
  createAddress,
  deleteAddress,
  fetchUserProfile,
  isUnauthorizedError as isUserUnauthorizedError,
  setCurrentAddress,
  updateAddress,
} from "@/lib/users"
import {
  AlertMessage,
  EmptyState,
  PageHeader,
  PageShell,
  Panel,
} from "@/components/app/page"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

type AddressesPageData = Awaited<ReturnType<typeof loadAddressesPageData>>

const emptyAddressForm: AddressInput = {
  label: "",
  phone: "",
  identityNumber: "",
  addressLine: "",
  city: "",
  country: "",
  zipCode: "",
}

async function loadAddressesPageData() {
  return { profile: await fetchUserProfile() }
}

export const Route = createFileRoute("/_authenticated/settings_/addresses")({
  loader: async ({ location }) => {
    try {
      return await loadAddressesPageData()
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: {
            mode: "signin",
            redirect: location.href,
          },
        })
      }

      throw error
    }
  },
  component: AddressesPage,
})

function AddressesPage() {
  const navigate = useNavigate()
  const initialData = Route.useLoaderData()
  const [pageData, setPageData] = useState<AddressesPageData>(initialData)
  const [formValues, setFormValues] = useState<AddressInput>(emptyAddressForm)
  const [editingAddressId, setEditingAddressId] = useState<number | null>(null)
  const [busyAction, setBusyAction] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const sortedAddresses = useMemo(
    () =>
      [...pageData.profile.addresses].sort((a, b) => {
        if (a.id === pageData.profile.currentAddressId) return -1
        if (b.id === pageData.profile.currentAddressId) return 1
        return b.id - a.id
      }),
    [pageData.profile.addresses, pageData.profile.currentAddressId]
  )

  async function handleUnauthorized() {
    await navigate({
      to: "/login",
      search: {
        mode: "signin",
        redirect: "/settings/addresses",
      },
    })
  }

  function startCreate() {
    setEditingAddressId(null)
    setFormValues(emptyAddressForm)
    setErrorMessage(null)
  }

  function startEdit(address: Address) {
    setEditingAddressId(address.id)
    setFormValues({
      label: address.label ?? "",
      phone: address.phone,
      identityNumber: address.identityNumber,
      addressLine: address.addressLine,
      city: address.city,
      country: address.country,
      zipCode: address.zipCode,
    })
    setErrorMessage(null)
  }

  function mergeProfile(updates: Partial<UserProfile>) {
    setPageData((current) => ({
      ...current,
      profile: {
        ...current.profile,
        ...updates,
      },
    }))
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusyAction("save")
    setErrorMessage(null)

    try {
      if (editingAddressId) {
        const updatedAddress = await updateAddress(editingAddressId, formValues)
        mergeProfile({
          addresses: pageData.profile.addresses.map((address) =>
            address.id === updatedAddress.id ? updatedAddress : address
          ),
        })
      } else {
        const createdAddress = await createAddress(formValues)
        const addresses = [...pageData.profile.addresses, createdAddress]
        mergeProfile({ addresses })

        if (!pageData.profile.currentAddressId) {
          const updatedProfile = await setCurrentAddress(createdAddress.id)
          setPageData((current) => ({ ...current, profile: updatedProfile }))
        }
      }

      startCreate()
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
        await handleUnauthorized()
        return
      }

      setErrorMessage(
        error instanceof Error ? error.message : "Address could not be saved."
      )
    } finally {
      setBusyAction(null)
    }
  }

  async function handleSetCurrent(addressId: number) {
    setBusyAction(`current-${addressId}`)
    setErrorMessage(null)

    try {
      const profile = await setCurrentAddress(addressId)
      setPageData((current) => ({ ...current, profile }))
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
        await handleUnauthorized()
        return
      }

      setErrorMessage(
        error instanceof Error
          ? error.message
          : "Default address could not be changed."
      )
    } finally {
      setBusyAction(null)
    }
  }

  async function handleDelete(address: Address) {
    if (!window.confirm(`Delete ${address.label || "this address"}?`)) {
      return
    }

    setBusyAction(`delete-${address.id}`)
    setErrorMessage(null)

    try {
      await deleteAddress(address.id)
      mergeProfile({
        addresses: pageData.profile.addresses.filter(
          (item) => item.id !== address.id
        ),
        currentAddressId:
          pageData.profile.currentAddressId === address.id
            ? null
            : pageData.profile.currentAddressId,
      })

      if (editingAddressId === address.id) {
        startCreate()
      }
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
        await handleUnauthorized()
        return
      }

      setErrorMessage(
        error instanceof Error ? error.message : "Address could not be deleted."
      )
    } finally {
      setBusyAction(null)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-50 text-neutral-950">
      <PageShell>
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
          <section className="space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <PageHeader
                breadcrumbs={[
                  { label: "Home", href: "/" },
                  { label: "My Account", href: "/settings" },
                  { label: "My Addresses" },
                ]}
                className="mb-0"
                eyebrow="Delivery information"
                title="My Addresses"
              />
              <Button onClick={startCreate} type="button">
                <Plus className="h-4 w-4" />
                <span>New Address</span>
              </Button>
            </div>

            {errorMessage ? <AlertMessage>{errorMessage}</AlertMessage> : null}

            {sortedAddresses.length === 0 ? (
              <EmptyState
                description="Add your first address to use it on future orders."
                icon={MapPin}
                title="No saved addresses"
              />
            ) : (
              <div className="grid gap-4 xl:grid-cols-2">
                {sortedAddresses.map((address) => (
                  <AddressCard
                    address={address}
                    busyAction={busyAction}
                    isCurrent={address.id === pageData.profile.currentAddressId}
                    isEditing={address.id === editingAddressId}
                    key={address.id}
                    onDelete={() => void handleDelete(address)}
                    onEdit={() => startEdit(address)}
                    onSetCurrent={() => void handleSetCurrent(address.id)}
                  />
                ))}
              </div>
            )}
          </section>

          <aside className="lg:sticky lg:top-6 lg:self-start">
            <form
              className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm"
              onSubmit={(event) => void handleSubmit(event)}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm text-neutral-500">
                    {editingAddressId ? "Edit address" : "New address"}
                  </p>
                  <h2 className="mt-1 text-xl font-semibold">
                    {editingAddressId ? "Edit Address" : "Add Address"}
                  </h2>
                </div>
                {editingAddressId ? (
                  <Button
                    onClick={startCreate}
                    size="sm"
                    type="button"
                    variant="outline"
                  >
                    Cancel
                  </Button>
                ) : null}
              </div>

              <div className="mt-5 grid gap-4">
                <TextField
                  label="Address Title"
                  name="label"
                  onChange={setFormValues}
                  placeholder="Home, Work"
                  value={formValues.label}
                />
                <TextField
                  label="Phone"
                  name="phone"
                  onChange={setFormValues}
                  placeholder="+905551234567"
                  required
                  value={formValues.phone}
                />
                <TextField
                  label="Identity Number"
                  name="identityNumber"
                  onChange={setFormValues}
                  placeholder="12345678901"
                  required
                  value={formValues.identityNumber}
                />
                <div className="grid gap-2">
                  <Label>Address</Label>
                  <textarea
                    className="min-h-28 rounded-lg border border-neutral-200 bg-white px-3 py-3 text-sm font-normal transition outline-none focus:border-neutral-950 focus:ring-2 focus:ring-neutral-900/10"
                    onChange={(event) =>
                      setFormValues((current) => ({
                        ...current,
                        addressLine: event.target.value,
                      }))
                    }
                    placeholder="Neighborhood, street, building, and apartment details"
                    required
                    value={formValues.addressLine}
                  />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField
                    label="City"
                    name="city"
                    onChange={setFormValues}
                    required
                    value={formValues.city}
                  />
                  <TextField
                    label="Zip Code"
                    name="zipCode"
                    onChange={setFormValues}
                    required
                    value={formValues.zipCode}
                  />
                </div>
                <TextField
                  label="Country"
                  name="country"
                  onChange={setFormValues}
                  required
                  value={formValues.country}
                />
              </div>

              <Button
                className="mt-5 w-full"
                disabled={busyAction === "save"}
                type="submit"
              >
                {busyAction === "save" ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Check className="h-4 w-4" />
                )}
                <span>{editingAddressId ? "Update" : "Save"}</span>
              </Button>
            </form>
          </aside>
        </div>
      </PageShell>
    </main>
  )
}

function AddressCard({
  address,
  busyAction,
  isCurrent,
  isEditing,
  onDelete,
  onEdit,
  onSetCurrent,
}: {
  address: Address
  busyAction: string | null
  isCurrent: boolean
  isEditing: boolean
  onDelete: () => void
  onEdit: () => void
  onSetCurrent: () => void
}) {
  return (
    <Panel
      className={cn(
        "p-5 shadow-sm transition",
        isCurrent ? "border-neutral-950" : "border-neutral-300",
        isEditing ? "ring-2 ring-neutral-950/10" : ""
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-neutral-100">
            <Home className="h-5 w-5 text-neutral-700" />
          </div>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-base font-semibold">
                {address.label || "Address"}
              </h2>
              {isCurrent ? <Badge variant="success">Default</Badge> : null}
            </div>
            <p className="mt-1 text-sm text-neutral-500">{address.phone}</p>
          </div>
        </div>
      </div>

      <p className="mt-4 text-sm leading-6 text-neutral-700">
        {address.addressLine}
      </p>
      <p className="mt-2 text-sm font-medium text-neutral-900">
        {address.city}, {address.zipCode}, {address.country}
      </p>

      <div className="mt-5 flex flex-wrap gap-2">
        <Button onClick={onEdit} size="sm" type="button" variant="outline">
          <Edit3 className="h-4 w-4" />
          <span>Edit</span>
        </Button>
        <Button
          disabled={busyAction === `delete-${address.id}`}
          onClick={onDelete}
          size="sm"
          type="button"
          variant="destructive"
        >
          {busyAction === `delete-${address.id}` ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Trash2 className="h-4 w-4" />
          )}
          <span>Delete</span>
        </Button>
        {!isCurrent ? (
          <Button
            disabled={busyAction === `current-${address.id}`}
            onClick={onSetCurrent}
            size="sm"
            type="button"
            variant="outline"
          >
            {busyAction === `current-${address.id}` ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Star className="h-4 w-4" />
            )}
            <span>Set as Default</span>
          </Button>
        ) : null}
      </div>
    </Panel>
  )
}

function TextField({
  label,
  name,
  onChange,
  placeholder,
  required,
  value,
}: {
  label: string
  name: keyof AddressInput
  onChange: React.Dispatch<React.SetStateAction<AddressInput>>
  placeholder?: string
  required?: boolean
  value: string
}) {
  return (
    <div className="grid gap-2">
      <Label>{label}</Label>
      <Input
        name={name}
        onChange={(event) =>
          onChange((current) => ({ ...current, [name]: event.target.value }))
        }
        placeholder={placeholder}
        required={required}
        value={value}
      />
    </div>
  )
}
