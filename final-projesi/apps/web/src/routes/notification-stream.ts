import { createFileRoute } from "@tanstack/react-router"
import { getCookie } from "@tanstack/react-start/server"

import { ACCESS_TOKEN_COOKIE } from "@/lib/auth"

function getInternalApiBaseUrl() {
  if (process.env.INTERNAL_API_BASE_URL) {
    return process.env.INTERNAL_API_BASE_URL
  }

  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL
  }

  return "http://localhost:8080"
}

export const Route = createFileRoute("/notification-stream")({
  server: {
    handlers: {
      GET: async ({ request }) => {
        const token = getCookie(ACCESS_TOKEN_COOKIE)

        if (!token) {
          return new Response("Unauthorized", { status: 401 })
        }

        const response = await fetch(
          `${getInternalApiBaseUrl()}/api/notifications/stream`,
          {
            headers: {
              Accept: "text/event-stream",
              Authorization: `Bearer ${token}`,
            },
            signal: request.signal,
          },
        )

        if (!response.ok || !response.body) {
          const upstreamMessage = await response.text().catch(() => "")

          return new Response(
            `Notification stream unavailable (${response.status})${
              upstreamMessage ? `: ${upstreamMessage}` : ""
            }`,
            {
            status: response.status,
            },
          )
        }

        return new Response(response.body, {
          headers: {
            "Cache-Control": "no-cache",
            "Content-Type": "text/event-stream",
            Connection: "keep-alive",
          },
        })
      },
    },
  },
})
