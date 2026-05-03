import {
  Bar,
  BarChart,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"

import type { Order, OrderStatus } from "@/lib/orders"
import { formatPrice } from "@/lib/products"

const REVENUE_COLOR = {
  revenue: "#16a34a",
  gross: "#ec4899",
  refund: "#171717",
} as const

const REVENUE_STATUSES: ReadonlyArray<OrderStatus> = ["COMPLETED"]
const GROSS_STATUSES: ReadonlyArray<OrderStatus> = [
  "PENDING",
  "PAID",
  "SHIPPED",
  "DELIVERED",
  "RETURNING",
  "RETURN_SHIPPED",
  "REFUNDING",
]
const REFUND_STATUSES: ReadonlyArray<OrderStatus> = ["RETURNED"]

function bucketize(orders: Array<Order>) {
  const sumBy = (statuses: ReadonlyArray<OrderStatus>) =>
    orders
      .filter((order) => statuses.includes(order.status))
      .reduce((sum, order) => sum + order.totalPrice, 0)

  const countBy = (statuses: ReadonlyArray<OrderStatus>) =>
    orders.filter((order) => statuses.includes(order.status)).length

  return {
    revenue: { value: sumBy(REVENUE_STATUSES), count: countBy(REVENUE_STATUSES) },
    gross: { value: sumBy(GROSS_STATUSES), count: countBy(GROSS_STATUSES) },
    refund: { value: sumBy(REFUND_STATUSES), count: countBy(REFUND_STATUSES) },
  }
}

export function OrderStatusBarChart({ orders }: { orders: Array<Order> }) {
  const buckets = bucketize(orders)
  const data = [
    { key: "Revenue", value: buckets.revenue.value, fill: REVENUE_COLOR.revenue },
    { key: "Gross", value: buckets.gross.value, fill: REVENUE_COLOR.gross },
    { key: "Refund", value: buckets.refund.value, fill: REVENUE_COLOR.refund },
  ]

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="mb-4">
        <h2 className="font-semibold">Revenue Breakdown</h2>
        <p className="text-sm text-neutral-500">
          Net revenue · in-flight · refunds
        </p>
      </div>
      <div className="h-72 w-full">
        <ResponsiveContainer height="100%" width="100%">
          <BarChart data={data}>
            <XAxis
              dataKey="key"
              fontSize={11}
              interval={0}
              stroke="#737373"
              tickLine={false}
            />
            <YAxis
              allowDecimals={false}
              fontSize={11}
              stroke="#737373"
              tickLine={false}
            />
            <Tooltip
              contentStyle={{
                border: "1px solid #e5e5e5",
                borderRadius: 8,
                fontSize: 12,
              }}
              cursor={{ fill: "#f5f5f5" }}
              formatter={(value) => formatPrice(Number(value))}
            />
            <Bar dataKey="value" radius={[6, 6, 0, 0]}>
              {data.map((entry) => (
                <Cell fill={entry.fill} key={entry.key} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

export function OrderRevenuePieChart({ orders }: { orders: Array<Order> }) {
  const buckets = bucketize(orders)
  const data = [
    { key: "Revenue", value: buckets.revenue.value, fill: REVENUE_COLOR.revenue },
    { key: "Gross", value: buckets.gross.value, fill: REVENUE_COLOR.gross },
    { key: "Refund", value: buckets.refund.value, fill: REVENUE_COLOR.refund },
  ].filter((entry) => entry.value > 0)

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="mb-4">
        <h2 className="font-semibold">Revenue Share</h2>
        <p className="text-sm text-neutral-500">
          Distribution of order value across buckets
        </p>
      </div>
      <div className="h-72 w-full">
        <ResponsiveContainer height="100%" width="100%">
          <PieChart>
            <Tooltip
              contentStyle={{
                border: "1px solid #e5e5e5",
                borderRadius: 8,
                fontSize: 12,
              }}
              formatter={(value) => formatPrice(Number(value))}
            />
            <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} />
            <Pie
              data={data}
              dataKey="value"
              innerRadius={50}
              nameKey="key"
              outerRadius={90}
              paddingAngle={2}
              stroke="white"
            >
              {data.map((entry) => (
                <Cell fill={entry.fill} key={entry.key} />
              ))}
            </Pie>
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
