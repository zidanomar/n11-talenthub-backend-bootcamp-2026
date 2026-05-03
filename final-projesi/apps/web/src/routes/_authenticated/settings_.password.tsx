import { useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { KeyRound, Loader2 } from "lucide-react"
import { toast } from "sonner"

import {
  changePassword,
  fetchUserProfile,
  isUnauthorizedError as isUserUnauthorizedError,
} from "@/lib/users"
import {
  AlertMessage,
  PageHeader,
  PageShell,
  Panel,
} from "@/components/app/page"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export const Route = createFileRoute("/_authenticated/settings_/password")({
  loader: async ({ location }) => {
    try {
      const profile = await fetchUserProfile()
      return { profile }
    } catch (error) {
      if (isUserUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: location.href },
        })
      }
      throw error
    }
  },
  component: ChangePasswordPage,
})

function ChangePasswordPage() {
  const navigate = useNavigate()
  const [oldPassword, setOldPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setErrorMessage(null)

    if (!oldPassword || !newPassword || !confirmPassword) {
      setErrorMessage("All fields are required.")
      return
    }

    if (newPassword.length < 6) {
      setErrorMessage("New password must be at least 6 characters.")
      return
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage("New password and confirmation do not match.")
      return
    }

    if (oldPassword === newPassword) {
      setErrorMessage("New password must differ from old password.")
      return
    }

    setIsSubmitting(true)
    try {
      await changePassword({ oldPassword, newPassword })
      toast.success("Password changed. Please sign in again.")
      await navigate({
        to: "/login",
        search: { mode: "signin", redirect: "/settings" },
      })
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Password could not be changed."
      setErrorMessage(message)
      toast.error(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-50 text-neutral-950">
      <PageShell maxWidth="5xl">
        <PageHeader
          breadcrumbs={[
            { label: "Home", href: "/" },
            { label: "My Account", href: "/settings" },
            { label: "Change Password" },
          ]}
          description="Update the password used to sign into your account."
          eyebrow="Security"
          title="Change Password"
        />

        <Panel className="mt-6 max-w-xl p-6">
          <form className="grid gap-4" onSubmit={handleSubmit}>
            <div className="grid gap-2">
              <Label htmlFor="old-password">Current Password</Label>
              <Input
                autoComplete="current-password"
                id="old-password"
                onChange={(event) => setOldPassword(event.target.value)}
                type="password"
                value={oldPassword}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="new-password">New Password</Label>
              <Input
                autoComplete="new-password"
                id="new-password"
                minLength={6}
                onChange={(event) => setNewPassword(event.target.value)}
                type="password"
                value={newPassword}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="confirm-password">Confirm New Password</Label>
              <Input
                autoComplete="new-password"
                id="confirm-password"
                minLength={6}
                onChange={(event) => setConfirmPassword(event.target.value)}
                type="password"
                value={confirmPassword}
              />
            </div>

            {errorMessage ? <AlertMessage>{errorMessage}</AlertMessage> : null}

            <div className="flex justify-end">
              <Button disabled={isSubmitting} type="submit">
                {isSubmitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <KeyRound className="h-4 w-4" />
                )}
                <span>{isSubmitting ? "Updating" : "Change Password"}</span>
              </Button>
            </div>
          </form>
        </Panel>
      </PageShell>
    </main>
  )
}
