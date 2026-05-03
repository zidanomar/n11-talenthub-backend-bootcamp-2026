import type { VariantProps } from "class-variance-authority"
import type { badgeVariants } from "@/components/ui/badge"
import type { OrderStatus } from "@/lib/orders"

import { Badge } from "@/components/ui/badge"

type BadgeVariant = NonNullable<VariantProps<typeof badgeVariants>["variant"]>

export const ORDER_STATUS_META: Record<
  OrderStatus,
  {
    label: string
    variant: BadgeVariant
  }
> = {
  CANCELLED: { label: "Cancelled", variant: "danger" },
  CHECKOUT: { label: "Checkout", variant: "neutral" },
  COMPLETED: { label: "Completed", variant: "violet" },
  DELIVERED: { label: "Delivered", variant: "teal" },
  PAID: { label: "Paid", variant: "success" },
  PAYMENT_FAILED: { label: "Payment Failed", variant: "danger" },
  PENDING: { label: "Pending", variant: "warning" },
  REFUNDING: { label: "Refunding", variant: "indigo" },
  RETURNED: { label: "Returned", variant: "neutral" },
  RETURNING: { label: "Returning", variant: "orange" },
  RETURN_FAILED: { label: "Return Failed", variant: "danger" },
  RETURN_SHIPPED: { label: "Return Shipped", variant: "cyan" },
  SHIPPED: { label: "Shipped", variant: "info" },
}

export function formatOrderStatus(status: OrderStatus) {
  return ORDER_STATUS_META[status].label
}

export function OrderStatusBadge({
  status,
  size = "md",
}: {
  status: OrderStatus
  size?: "sm" | "md" | "lg"
}) {
  const meta = ORDER_STATUS_META[status]

  return (
    <Badge size={size} variant={meta.variant}>
      {meta.label}
    </Badge>
  )
}
