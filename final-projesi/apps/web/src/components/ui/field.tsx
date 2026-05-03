import { cn } from "@/lib/utils"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

type FieldProps = {
  label: string
  type?: string
  value: string
  error?: string
  placeholder?: string
  trailingAction?: React.ReactNode
  id?: string
  autoComplete?: string
  readOnly?: boolean
  onChange?: (value: string) => void
  onBlur?: () => void
}

export function Field({
  autoComplete,
  error,
  id,
  label,
  onBlur,
  onChange,
  placeholder,
  readOnly,
  trailingAction,
  type = "text",
  value,
}: FieldProps) {
  return (
    <div className="grid gap-2">
      <Label className="text-sm text-neutral-700" htmlFor={id}>
        {label}
      </Label>
      <div className="flex items-center gap-2">
        <Input
          autoComplete={autoComplete}
          aria-invalid={error ? true : undefined}
          className={cn(readOnly ? "bg-neutral-50" : "", trailingAction ? "pr-2" : "")}
          id={id}
          onBlur={onBlur}
          onChange={onChange ? (e) => onChange(e.target.value) : undefined}
          placeholder={placeholder}
          readOnly={readOnly}
          type={type}
          value={value}
        />
        {trailingAction}
      </div>
      {error ? <p className="mt-2 text-xs text-red-600">{error}</p> : null}
    </div>
  )
}
