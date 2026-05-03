import { useState } from "react"
import {
  Link,
  createFileRoute,
  notFound,
  redirect,
  useNavigate,
} from "@tanstack/react-router"
import {
  Check,
  CheckCircle2,
  Loader2,
  Package,
  ReceiptText,
  RotateCcw,
  Undo2,
} from "lucide-react"

import { StatusBadge, formatOrderDate, formatOrderStatus } from "./orders"
import {
  cancelOrder,
  fetchOrder,
  finishOrder,
  isForbiddenError as isOrderForbiddenError,
  isNotFoundError as isOrderNotFoundError,
  isUnauthorizedError as isOrderUnauthorizedError,
  payOrder,
  retryRefund,
  returnOrder,
} from "@/lib/orders"
import { formatPrice } from "@/lib/products"
import {
  AlertMessage,
  Breadcrumbs,
  DataTable,
  PageShell,
  Panel,
  SummaryRow,
} from "@/components/app/page"
import { Button } from "@/components/ui/button"

export const Route = createFileRoute("/_authenticated/orders_/$orderId")({
  loader: async ({ location, params }) => {
    const orderId = Number(params.orderId)

    if (!Number.isInteger(orderId) || orderId <= 0) {
      throw redirect({ to: "/orders" })
    }

    try {
      const order = await fetchOrder(orderId)

      return { order }
    } catch (error) {
      if (isOrderForbiddenError(error)) {
        throw redirect({ to: "/forbidden" })
      }

      if (isOrderNotFoundError(error)) {
        throw notFound()
      }

      if (isOrderUnauthorizedError(error)) {
        throw redirect({
          to: "/login",
          search: { mode: "signin", redirect: location.href },
        })
      }

      throw error
    }
  },
  component: OrderDetailPage,
})

function OrderDetailPage() {
  const navigate = useNavigate()
  const { order } = Route.useLoaderData()
  const [busyAction, setBusyAction] = useState<
    "finish" | "return" | "retry" | "repay" | "cancel" | null
  >(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const statusHistory =
    order.statusHistory.length > 0
      ? order.statusHistory
      : [{ status: order.status, changedAt: order.createdAt, note: null }]

  async function handlePayNow() {
    setBusyAction("repay")
    setErrorMessage(null)
    try {
      const result = await payOrder(order.id, {
        forceThreeDS: true,
        paymentWithNewCardEnabled: true,
      })
      window.location.href = result.paymentPageUrl
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Payment could not be started."
      )
      setBusyAction(null)
    }
  }

  async function runOrderAction(
    action: "finish" | "return" | "retry" | "cancel",
    callback: () => Promise<unknown>
  ) {
    setBusyAction(action)
    setErrorMessage(null)

    try {
      await callback()
      await navigate({
        params: { orderId: String(order.id) },
        to: "/orders/$orderId",
      })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "Order could not be updated."
      )
    } finally {
      setBusyAction(null)
    }
  }

  return (
    <main className="min-h-svh bg-neutral-100 text-neutral-950">
      <PageShell>
        <Breadcrumbs
          className="mb-4"
          items={[
            { label: "Home", href: "/" },
            { label: "My Account", href: "/settings" },
            { label: "My Orders", href: "/orders" },
            { label: `Order #${order.id}` },
          ]}
        />

        <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
          <Panel className="p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="text-sm text-neutral-500">Order Detail</p>
                <h1 className="mt-1 text-3xl font-semibold">
                  Order #{order.id}
                </h1>
                <p className="mt-2 text-sm text-neutral-500">
                  {formatOrderDate(order.createdAt)}
                </p>
              </div>
              <StatusBadge status={order.status} />
            </div>

            <DataTable className="mt-6 rounded-xl" minWidth="620px">
              <thead className="bg-neutral-50 text-xs font-semibold text-neutral-500 uppercase">
                <tr>
                  <th className="px-4 py-3">Product</th>
                  <th className="px-4 py-3">Quantity</th>
                  <th className="px-4 py-3">Unit Price</th>
                  <th className="px-4 py-3 text-right">Line Total</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr
                    className="border-t border-neutral-100"
                    key={item.productId}
                  >
                    <td className="px-4 py-4">
                      <Link
                        className="flex items-center gap-3 hover:text-brand"
                        params={{ productId: String(item.productId) }}
                        to="/products/$productId"
                      >
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-neutral-100">
                          <Package className="h-4 w-4 text-neutral-500" />
                        </div>
                        <span className="font-medium underline-offset-4 hover:underline">
                          Product #{item.productId}
                        </span>
                      </Link>
                    </td>
                    <td className="px-4 py-4">{item.quantity}</td>
                    <td className="px-4 py-4">{formatPrice(item.unitPrice)}</td>
                    <td className="px-4 py-4 text-right font-semibold">
                      {formatPrice(item.totalPrice)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </DataTable>

            <section className="mt-6 rounded-xl border border-neutral-200 p-5">
              <h2 className="text-lg font-semibold">Order Status History</h2>
              <div className="mt-5 space-y-5">
                {statusHistory.map((entry, index) => {
                  const isLast = index === statusHistory.length - 1

                  return (
                    <div
                      className="relative flex gap-4"
                      key={`${entry.status}-${entry.changedAt}-${index}`}
                    >
                      {!isLast ? (
                        <span className="absolute top-9 left-4 h-[calc(100%+0.25rem)] w-px bg-neutral-200" />
                      ) : null}
                      <span className="relative flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-neutral-950 text-white">
                        <CheckCircle2 className="h-4 w-4" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                          <StatusBadge status={entry.status} />
                          <time className="text-sm text-neutral-500">
                            {formatOrderDate(entry.changedAt)}
                          </time>
                        </div>
                        {entry.note ? (
                          <p className="mt-2 text-sm leading-6 text-neutral-600">
                            {entry.note}
                          </p>
                        ) : null}
                      </div>
                    </div>
                  )
                })}
              </div>
            </section>
          </Panel>

          <Panel className="p-6 lg:self-start">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-neutral-100">
                <ReceiptText className="h-5 w-5 text-neutral-600" />
              </div>
              <h2 className="text-lg font-semibold">Order Summary</h2>
            </div>

            <div className="mt-5 space-y-3 text-sm">
              <SummaryRow label="Items" value={String(order.items.length)} />
              <SummaryRow
                label="Status"
                value={formatOrderStatus(order.status)}
              />
              <SummaryRow
                label="Created"
                value={formatOrderDate(order.createdAt)}
              />
            </div>

            <div className="mt-5 border-t border-neutral-200 pt-4">
              <SummaryRow
                label="Total"
                large
                value={formatPrice(order.totalPrice)}
              />
            </div>

            <section className="mt-5 border-t border-neutral-200 pt-5">
              <h3 className="font-semibold">Order Resolution</h3>
              <p className="mt-2 text-sm leading-6 text-neutral-500">
                {getResolutionMessage(order.status)}
              </p>

              {errorMessage ? (
                <AlertMessage className="mt-4">{errorMessage}</AlertMessage>
              ) : null}

              {order.status === "PENDING" ||
              order.status === "PAYMENT_FAILED" ? (
                <div className="mt-4 grid gap-2">
                  <Button
                    disabled={busyAction !== null}
                    onClick={() => void handlePayNow()}
                    type="button"
                  >
                    {busyAction === "repay" ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <RotateCcw className="h-4 w-4" />
                    )}
                    <span>
                      {busyAction === "repay"
                        ? "Redirecting…"
                        : order.status === "PAYMENT_FAILED"
                          ? "Retry Payment"
                          : "Pay Now"}
                    </span>
                  </Button>
                  <Button
                    disabled={busyAction !== null}
                    onClick={() =>
                      void runOrderAction("cancel", () => cancelOrder(order.id))
                    }
                    type="button"
                    variant="destructive"
                  >
                    {busyAction === "cancel" ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Undo2 className="h-4 w-4" />
                    )}
                    <span>
                      {busyAction === "cancel" ? "Cancelling…" : "Cancel Order"}
                    </span>
                  </Button>
                </div>
              ) : null}

              {order.status === "DELIVERED" ? (
                <div className="mt-4 grid gap-2">
                  <Button
                    disabled={busyAction !== null}
                    onClick={() =>
                      void runOrderAction("finish", () => finishOrder(order.id))
                    }
                    type="button"
                  >
                    {busyAction === "finish" ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Check className="h-4 w-4" />
                    )}
                    <span>
                      {busyAction === "finish" ? "Finishing" : "Finish Order"}
                    </span>
                  </Button>
                  <Button
                    disabled={busyAction !== null}
                    onClick={() =>
                      void runOrderAction("return", () => returnOrder(order.id))
                    }
                    type="button"
                    variant="outline"
                  >
                    {busyAction === "return" ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Undo2 className="h-4 w-4" />
                    )}
                    <span>
                      {busyAction === "return" ? "Starting" : "Return Order"}
                    </span>
                  </Button>
                </div>
              ) : null}

              {order.status === "RETURN_FAILED" ? (
                <Button
                  className="mt-4 w-full"
                  disabled={busyAction !== null}
                  onClick={() =>
                    void runOrderAction("retry", () => retryRefund(order.id))
                  }
                  type="button"
                >
                  {busyAction === "retry" ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <RotateCcw className="h-4 w-4" />
                  )}
                  <span>
                    {busyAction === "retry" ? "Retrying" : "Retry Refund"}
                  </span>
                </Button>
              ) : null}
            </section>
          </Panel>
        </div>
      </PageShell>
    </main>
  )
}

function getResolutionMessage(status: string) {
  switch (status) {
    case "PENDING":
      return "Your order is waiting for payment. Pay now or cancel the order."
    case "PAYMENT_FAILED":
      return "Payment was declined. You can retry with the same card or cancel the order."
    case "DELIVERED":
      return "Your order was delivered. You can keep it to complete the order or start a return."
    case "RETURNING":
      return "Your return has started and is waiting for pickup."
    case "RETURN_SHIPPED":
      return "Your return package is on its way back to the warehouse."
    case "REFUNDING":
      return "Your return was received. The refund is being processed."
    case "RETURNED":
      return "Your order was returned and refunded."
    case "RETURN_FAILED":
      return "The refund failed after the return was received. You can retry the refund."
    case "COMPLETED":
      return "This order is completed."
    case "CANCELLED":
      return "This order was cancelled."
    default:
      return "No actions available for this order."
  }
}
