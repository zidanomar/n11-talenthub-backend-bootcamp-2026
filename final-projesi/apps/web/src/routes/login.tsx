import { useState } from "react"
import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router"
import { useForm } from "@tanstack/react-form"
import { Eye, EyeOff } from "lucide-react"

import type { SigninPayload, SignupPayload } from "@/lib/auth"
import {
  getAuthSession,
  persistAuthSession,
  signinRequest,
  signupRequest,
} from "@/lib/auth"
import { Field } from "@/components/ui/field"
import { AlertMessage, Breadcrumbs } from "@/components/app/page"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

type AuthMode = "signin" | "signup"

function sanitizeRedirect(url: unknown) {
  if (typeof url !== "string" || !url.startsWith("/") || url.startsWith("//")) {
    return "/"
  }

  return url
}

function validateMode(mode: unknown): AuthMode {
  return mode === "signup" ? "signup" : "signin"
}

export const Route = createFileRoute("/login")({
  validateSearch: (search: Record<string, unknown>) => ({
    mode: validateMode(search.mode),
    redirect: sanitizeRedirect(search.redirect),
  }),
  beforeLoad: async ({ search }) => {
    const auth = await getAuthSession()

    if (auth.isAuthenticated) {
      throw redirect({
        to: auth.role === "MERCHANT" ? "/admin" : search.redirect,
      })
    }
  },
  component: LoginPage,
})

function LoginPage() {
  const navigate = useNavigate()
  const search = Route.useSearch()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [signupNotice, setSignupNotice] = useState<string | null>(null)
  const [showSigninPassword, setShowSigninPassword] = useState(false)
  const [showSignupPassword, setShowSignupPassword] = useState(false)

  const signinForm = useForm({
    defaultValues: {
      username: "",
      password: "",
    } satisfies SigninPayload,
    onSubmit: async ({ value }) => {
      setSubmitError(null)
      setSignupNotice(null)

      try {
        const response = await signinRequest(value)

        if (!response.access_token) {
          throw new Error("Access token was not returned by the API")
        }

        await persistAuthSession({
          data: {
            accessToken: response.access_token,
            refreshToken:
              typeof response.refresh_token === "string"
                ? response.refresh_token
                : undefined,
            username: value.username,
          },
        })

        const auth = await getAuthSession()

        await navigate({
          to: auth.role === "MERCHANT" ? "/admin" : search.redirect,
        })
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : "Sign in failed"
        )
      }
    },
  })

  const signupForm = useForm({
    defaultValues: {
      firstName: "",
      lastName: "",
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
    onSubmit: async ({ value }) => {
      setSubmitError(null)

      if (value.password !== value.confirmPassword) {
        setSubmitError("Password and confirmation do not match.")
        return
      }

      const { confirmPassword: _confirm, ...payload } = value
      void _confirm

      try {
        const response = await signupRequest(payload satisfies SignupPayload)

        setSignupNotice(
          `Account created for ${response.username}. You can sign in now.`
        )
        await navigate({
          to: "/login",
          search: {
            mode: "signin",
            redirect: search.redirect,
          },
        })
        signinForm.setFieldValue("username", value.username)
        signinForm.setFieldValue("password", value.password)
      } catch (error) {
        setSubmitError(
          error instanceof Error
            ? error.message
            : "Sign up could not be completed"
        )
      }
    },
  })

  return (
    <main className="flex min-h-svh items-center justify-center bg-neutral-100 px-4 py-10">
      <div className="w-full max-w-md rounded-[28px] bg-white p-6 shadow-sm sm:p-8">
        <Breadcrumbs
          className="mb-6"
          items={[
            { label: "Home", href: "/" },
            { label: search.mode === "signin" ? "Sign In" : "Sign Up" },
          ]}
        />
        <div className="mb-8 text-center">
          <div className="text-5xl font-black tracking-tight text-neutral-900">
            n11
          </div>
          <h1 className="mt-5 text-3xl font-semibold text-neutral-900">
            {search.mode === "signin" ? "Sign In" : "Sign Up"}
          </h1>
          <p className="mt-3 text-sm text-neutral-600">
            Form fields currently match the existing backend API contract.
          </p>
        </div>

        <div className="mb-6 grid grid-cols-2 rounded-2xl bg-neutral-100 p-1">
          <button
            className={cn(
              "rounded-xl px-4 py-3 text-sm font-semibold transition-colors",
              search.mode === "signin"
                ? "bg-white text-neutral-900 shadow-sm"
                : "text-neutral-500"
            )}
            onClick={() =>
              navigate({
                to: "/login",
                search: { mode: "signin", redirect: search.redirect },
              })
            }
            type="button"
          >
            Sign In
          </button>
          <button
            className={cn(
              "rounded-xl px-4 py-3 text-sm font-semibold transition-colors",
              search.mode === "signup"
                ? "bg-white text-neutral-900 shadow-sm"
                : "text-neutral-500"
            )}
            onClick={() =>
              navigate({
                to: "/login",
                search: { mode: "signup", redirect: search.redirect },
              })
            }
            type="button"
          >
            Sign Up
          </button>
        </div>

        {submitError ? (
          <AlertMessage className="mb-4">{submitError}</AlertMessage>
        ) : null}

        {signupNotice ? (
          <AlertMessage className="mb-4" tone="success">
            {signupNotice}
          </AlertMessage>
        ) : null}

        {search.mode === "signin" ? (
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault()
              event.stopPropagation()
              void signinForm.handleSubmit()
            }}
          >
            <signinForm.Field
              name="username"
              validators={{
                onChange: ({ value }) =>
                  !value.trim() ? "Username is required." : undefined,
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Username"
                  type="text"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                />
              )}
            </signinForm.Field>

            <signinForm.Field
              name="password"
              validators={{
                onChange: ({ value }) =>
                  !value.trim() ? "Password is required." : undefined,
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Password"
                  type={showSigninPassword ? "text" : "password"}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                  trailingAction={
                    <button
                      className="text-neutral-500"
                      onClick={() => setShowSigninPassword((value) => !value)}
                      type="button"
                    >
                      {showSigninPassword ? (
                        <EyeOff className="h-5 w-5" />
                      ) : (
                        <Eye className="h-5 w-5" />
                      )}
                    </button>
                  }
                />
              )}
            </signinForm.Field>

            <signinForm.Subscribe
              selector={(state) => [state.isSubmitting] as const}
            >
              {([isSubmitting]) => (
                <Button
                  className="w-full"
                  disabled={isSubmitting}
                  size="lg"
                  type="submit"
                >
                  {isSubmitting ? "Signing In..." : "Sign In"}
                </Button>
              )}
            </signinForm.Subscribe>
          </form>
        ) : (
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault()
              event.stopPropagation()
              void signupForm.handleSubmit()
            }}
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <signupForm.Field
                name="firstName"
                validators={{
                  onChange: ({ value }) =>
                    !value.trim() ? "First name is required." : undefined,
                }}
              >
                {(field) => (
                  <Field
                    error={field.state.meta.errors[0]}
                    label="First Name"
                    type="text"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={field.handleChange}
                  />
                )}
              </signupForm.Field>

              <signupForm.Field
                name="lastName"
                validators={{
                  onChange: ({ value }) =>
                    !value.trim() ? "Last name is required." : undefined,
                }}
              >
                {(field) => (
                  <Field
                    error={field.state.meta.errors[0]}
                    label="Last Name"
                    type="text"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={field.handleChange}
                  />
                )}
              </signupForm.Field>
            </div>

            <signupForm.Field
              name="username"
              validators={{
                onChange: ({ value }) =>
                  !value.trim() ? "Username is required." : undefined,
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Username"
                  type="text"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                />
              )}
            </signupForm.Field>

            <signupForm.Field
              name="email"
              validators={{
                onChange: ({ value }) => {
                  if (!value.trim()) {
                    return "Email is required."
                  }

                  return /\S+@\S+\.\S+/.test(value)
                    ? undefined
                    : "Enter a valid email address."
                },
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Email"
                  type="email"
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                />
              )}
            </signupForm.Field>

            <signupForm.Field
              name="password"
              validators={{
                onChange: ({ value }) => {
                  if (!value.trim()) {
                    return "Password is required."
                  }

                  return value.length >= 6
                    ? undefined
                    : "The current backend requires at least 6 characters."
                },
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Password"
                  type={showSignupPassword ? "text" : "password"}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                  trailingAction={
                    <button
                      className="text-neutral-500"
                      onClick={() => setShowSignupPassword((value) => !value)}
                      type="button"
                    >
                      {showSignupPassword ? (
                        <EyeOff className="h-5 w-5" />
                      ) : (
                        <Eye className="h-5 w-5" />
                      )}
                    </button>
                  }
                />
              )}
            </signupForm.Field>

            <signupForm.Field
              name="confirmPassword"
              validators={{
                onChangeListenTo: ["password"],
                onChange: ({ value, fieldApi }) => {
                  if (!value) return "Please confirm your password."
                  return value === fieldApi.form.getFieldValue("password")
                    ? undefined
                    : "Passwords do not match."
                },
              }}
            >
              {(field) => (
                <Field
                  error={field.state.meta.errors[0]}
                  label="Confirm Password"
                  type={showSignupPassword ? "text" : "password"}
                  value={field.state.value}
                  onBlur={field.handleBlur}
                  onChange={field.handleChange}
                />
              )}
            </signupForm.Field>

            <signupForm.Subscribe
              selector={(state) => [state.isSubmitting] as const}
            >
              {([isSubmitting]) => (
                <Button
                  className="w-full"
                  disabled={isSubmitting}
                  size="lg"
                  type="submit"
                >
                  {isSubmitting ? "Creating Account..." : "Sign Up"}
                </Button>
              )}
            </signupForm.Subscribe>
          </form>
        )}
      </div>
    </main>
  )
}
