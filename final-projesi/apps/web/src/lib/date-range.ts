export function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")

  return `${year}-${month}-${day}`
}

export function getLastSevenDaysRange() {
  const today = new Date()
  const from = new Date(today)
  from.setDate(today.getDate() - 6)

  return {
    from: formatDateInput(from),
    to: formatDateInput(today),
  }
}

export function normalizeDateInput(value: unknown) {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value)
    ? value
    : undefined
}
