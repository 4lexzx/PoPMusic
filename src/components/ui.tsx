import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'
import { CloseIcon, SpinnerIcon } from './Icons'
import { clamp } from '@/shared/utils/time'

/** Renderiza en document.body para escapar de contenedores con transform. */
export function Portal({ children }: { children: ReactNode }) {
  if (typeof document === 'undefined') return null
  return createPortal(children, document.body)
}

/* ==========================================================================
   Button
   ========================================================================== */
type ButtonVariant = 'primary' | 'ghost' | 'subtle' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  fullWidth?: boolean
  loading?: boolean
  icon?: ReactNode
}

const buttonVariants: Record<ButtonVariant, string> = {
  primary: 'bg-ink text-bg shadow-lg shadow-black/30 active:opacity-85',
  ghost: 'bg-transparent text-ink tap',
  subtle: 'surface-2 text-ink tap',
  danger: 'bg-accent/15 text-accent active:bg-accent/25',
}

const buttonSizes: Record<ButtonSize, string> = {
  sm: 'h-9 px-3.5 text-sm rounded-xl gap-1.5',
  md: 'h-11 px-4 text-sm rounded-2xl gap-2',
  lg: 'h-13 px-5 text-base rounded-2xl gap-2',
}

export function Button({
  variant = 'primary',
  size = 'md',
  fullWidth,
  loading,
  icon,
  className = '',
  children,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center font-medium transition-all duration-150 select-none active:scale-[0.98] disabled:opacity-40 ${
        buttonVariants[variant]
      } ${buttonSizes[size]} ${fullWidth ? 'w-full' : ''} ${className}`}
    >
      {loading ? <SpinnerIcon className="animate-spin" size={18} /> : icon}
      {children}
    </button>
  )
}

/* ==========================================================================
   IconButton
   ========================================================================== */
interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  label: string
  active?: boolean
  size?: 'sm' | 'md' | 'lg'
}

export function IconButton({
  label,
  active,
  size = 'md',
  className = '',
  children,
  ...props
}: IconButtonProps) {
  const dimension = size === 'sm' ? 'h-9 w-9' : size === 'lg' ? 'h-14 w-14' : 'h-11 w-11'
  return (
    <button
      {...props}
      aria-label={label}
      title={label}
      className={`inline-flex items-center justify-center rounded-full transition-colors duration-150 tap ${
        dimension
      } ${active ? 'bg-accent/15 text-accent' : 'text-muted'} ${className}`}
    >
      {children}
    </button>
  )
}

/* ==========================================================================
   Reveal (aparición al hacer scroll)
   ========================================================================== */
export function Reveal({
  children,
  className = '',
  delay = 0,
}: {
  children: ReactNode
  className?: string
  delay?: number
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const node = ref.current
    if (!node) return
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setVisible(true)
            observer.disconnect()
          }
        })
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.05 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className={`reveal ${visible ? 'is-visible' : ''} ${className}`}
      style={delay ? { transitionDelay: `${delay}ms` } : undefined}
    >
      {children}
    </div>
  )
}

/* ==========================================================================
   Indicador de reproducción (barras animadas)
   ========================================================================== */
export function PlayingBars({ className = '' }: { className?: string }) {
  return (
    <span className={`eq inline-flex items-end gap-[2px] ${className}`} aria-hidden="true">
      <span className="eq-bar" style={{ animationDelay: '0ms' }} />
      <span className="eq-bar" style={{ animationDelay: '180ms' }} />
      <span className="eq-bar" style={{ animationDelay: '360ms' }} />
      <span className="eq-bar" style={{ animationDelay: '120ms' }} />
    </span>
  )
}

/* ==========================================================================
   Toggle (interruptor accesible)
   ========================================================================== */
interface ToggleProps {
  checked: boolean
  onChange: (value: boolean) => void
  label?: string
}

export function Toggle({ checked, onChange, label }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`relative h-7 w-12 shrink-0 overflow-hidden rounded-full transition-colors duration-200 ${
        checked ? 'bg-accent' : 'bg-[color:var(--overlay-3)]'
      }`}
    >
      <span
        className={`absolute left-1 top-1 h-5 w-5 rounded-full shadow-sm transition-transform duration-200 ${
          checked ? 'translate-x-5 bg-[color:var(--color-accent-ink)]' : 'translate-x-0 bg-white'
        }`}
      />
    </button>
  )
}

/* ==========================================================================
   Slider
   ========================================================================== */
interface SliderProps {
  value: number
  onChange: (value: number) => void
  onCommit?: (value: number) => void
  ariaLabel?: string
  className?: string
  showThumb?: boolean
  trackClassName?: string
  fillClassName?: string
  thumbClassName?: string
  disabled?: boolean
}

export function Slider({
  value,
  onChange,
  onCommit,
  ariaLabel = 'Slider',
  className = '',
  showThumb = true,
  trackClassName = 'h-1.5 bg-[color:var(--overlay-3)]',
  fillClassName = 'bg-ink',
  thumbClassName = 'bg-white',
  disabled,
}: SliderProps) {
  const railRef = useRef<HTMLDivElement>(null)
  const draggingRef = useRef(false)
  const [dragging, setDragging] = useState(false)

  const ratioFromClientX = useCallback((clientX: number): number => {
    const element = railRef.current
    if (!element) return 0
    const rect = element.getBoundingClientRect()
    if (rect.width === 0) return 0
    return clamp((clientX - rect.left) / rect.width, 0, 1)
  }, [])

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (disabled) return
    draggingRef.current = true
    setDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
    onChange(ratioFromClientX(event.clientX))
  }

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!draggingRef.current) return
    onChange(ratioFromClientX(event.clientX))
  }

  const handlePointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!draggingRef.current) return
    draggingRef.current = false
    setDragging(false)
    const ratio = ratioFromClientX(event.clientX)
    onChange(ratio)
    onCommit?.(ratio)
  }

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (disabled) return
    let delta = 0
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') delta = 0.05
    if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') delta = -0.05
    if (delta === 0) return
    event.preventDefault()
    const next = clamp(value + delta, 0, 1)
    onChange(next)
    onCommit?.(next)
  }

  const percent = clamp(value, 0, 1) * 100

  return (
    <div
      ref={railRef}
      role="slider"
      tabIndex={0}
      aria-label={ariaLabel}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(percent)}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onKeyDown={handleKeyDown}
      className={`group relative flex touch-none items-center py-2.5 ${disabled ? 'opacity-50' : ''} ${className}`}
    >
      <div className={`relative w-full overflow-hidden rounded-full ${trackClassName}`}>
        <div className={`h-full rounded-full ${fillClassName}`} style={{ width: `${percent}%` }} />
      </div>
      {showThumb ? (
        <div
          className={`absolute top-1/2 h-3.5 w-3.5 -translate-y-1/2 rounded-full shadow-md transition-transform ${thumbClassName} ${
            dragging ? 'scale-125' : 'scale-100'
          }`}
          style={{ left: `calc(${percent}% - 7px)` }}
        />
      ) : null}
    </div>
  )
}

/* ==========================================================================
   Skeleton / Spinner
   ========================================================================== */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`skeleton rounded-lg ${className}`} />
}

export function Spinner({ size = 22, className = '' }: { size?: number; className?: string }) {
  return <SpinnerIcon className={`animate-spin text-muted ${className}`} size={size} />
}

/* ==========================================================================
   Sheet (bottom sheet con arrastre para cerrar)
   ========================================================================== */
interface SheetProps {
  open: boolean
  onClose: () => void
  title?: string
  children: ReactNode
}

export function Sheet({ open, onClose, title, children }: SheetProps) {
  const [dragY, setDragY] = useState(0)
  const [dragging, setDragging] = useState(false)
  const startYRef = useRef(0)

  useEffect(() => {
    if (!open) {
      setDragY(0)
      setDragging(false)
    }
  }, [open])

  if (!open) return null

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    // No iniciar el arrastre si se pulsa un botón (evita que la X deje de cerrar).
    if ((event.target as HTMLElement).closest('button, a, input')) return
    startYRef.current = event.clientY
    setDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!dragging) return
    setDragY(Math.max(0, event.clientY - startYRef.current))
  }

  const handlePointerUp = () => {
    if (!dragging) return
    setDragging(false)
    if (dragY > 90) {
      onClose()
    } else {
      setDragY(0)
    }
  }

  return createPortal(
    <div className="fixed inset-0 z-50 flex flex-col justify-end">
      <div
        className="animate-fade-in absolute inset-0 bg-black/70 backdrop-blur-md"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className="animate-sheet-up relative mx-auto flex w-full max-w-md flex-col overflow-hidden rounded-t-[1.75rem] border-t border-[color:var(--hair-strong)] bg-bg-elev/95 backdrop-blur-2xl"
        style={{
          transform: dragY ? `translateY(${dragY}px)` : undefined,
          transition: dragging ? 'none' : 'transform 0.25s ease',
          maxHeight: '85dvh',
        }}
      >
        <div
          className="shrink-0 touch-none px-4 pb-2 pt-3"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        >
          <div className="mx-auto h-1.5 w-10 rounded-full bg-[color:var(--overlay-3)]" />
          <div className="mt-3 flex items-center justify-between">
            {title ? <h2 className="text-base font-semibold tracking-tight">{title}</h2> : <span />}
            <IconButton label="Cerrar" size="sm" onClick={onClose}>
              <CloseIcon size={18} />
            </IconButton>
          </div>
        </div>
        <div className="no-scrollbar overflow-y-auto overscroll-contain px-4 pb-[calc(1.5rem+env(safe-area-inset-bottom))]">
          {children}
        </div>
      </div>
    </div>,
    document.body,
  )
}

/* ==========================================================================
   Pestillas tipo píldora
   ========================================================================== */
interface PillTabsProps<T extends string> {
  tabs: Array<{ id: T; label: string; count?: number }>
  value: T
  onChange: (id: T) => void
  className?: string
}

export function PillTabs<T extends string>({ tabs, value, onChange, className = '' }: PillTabsProps<T>) {
  return (
    <div className={`no-scrollbar flex gap-2 overflow-x-auto ${className}`}>
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <button
            key={tab.id}
            type="button"
            onClick={() => onChange(tab.id)}
            className={`shrink-0 rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors ${
              active
                ? 'bg-accent text-accent-ink shadow-[0_6px_18px_-8px_var(--color-accent)]'
                : 'surface-1 text-muted tap'
            }`}
          >
            {tab.label}
            {tab.count !== undefined && tab.count > 0 ? (
              <span className={`ml-1.5 ${active ? 'opacity-60' : 'opacity-60'}`}>{tab.count}</span>
            ) : null}
          </button>
        )
      })}
    </div>
  )
}

/* ==========================================================================
   Control segmentado
   ========================================================================== */
interface SegmentedProps<T extends string> {
  tabs: Array<{ id: T; label: string; count?: number }>
  value: T
  onChange: (id: T) => void
  className?: string
}

export function Segmented<T extends string>({ tabs, value, onChange, className = '' }: SegmentedProps<T>) {
  return (
    <div className={`flex items-center gap-1 rounded-2xl hairline surface-1 p-1 ${className}`}>
      {tabs.map((tab) => {
        const active = tab.id === value
        return (
          <button
            key={tab.id}
            type="button"
            onClick={() => onChange(tab.id)}
            className={`flex flex-1 items-center justify-center gap-1.5 whitespace-nowrap rounded-xl px-2.5 py-2 text-[0.82rem] font-medium transition-all ${
              active
                ? 'bg-accent text-accent-ink shadow-[0_6px_18px_-8px_var(--color-accent)]'
                : 'text-faint tap'
            }`}
          >
            {tab.label}
            {tab.count !== undefined && tab.count > 0 ? (
              <span className={`text-[0.7rem] ${active ? 'opacity-70' : 'text-faint'}`}>
                {tab.count}
              </span>
            ) : null}
          </button>
        )
      })}
    </div>
  )
}

/* ==========================================================================
   Diálogo centrado (con fondo desenfocado)
   ========================================================================== */
interface DialogProps {
  open: boolean
  onClose: () => void
  children: ReactNode
  className?: string
}

export function Dialog({ open, onClose, children, className = '' }: DialogProps) {
  useEffect(() => {
    if (!open) return
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-[80] flex items-center justify-center p-4">
      <div
        className="animate-fade-in absolute inset-0 bg-black/70 backdrop-blur-md"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className={`animate-pop glass-panel relative w-full max-w-sm rounded-2xl p-5 ${className}`}
      >
        {children}
      </div>
    </div>,
    document.body,
  )
}

/* ==========================================================================
   Encabezado de pantalla
   ========================================================================== */
interface ScreenHeaderProps {
  title: string
  subtitle?: string
  action?: ReactNode
}

export function ScreenHeader({ title, subtitle, action }: ScreenHeaderProps) {
  return (
    <header className="flex items-end justify-between gap-3 px-5 pb-4 pt-5">
      <div className="min-w-0">
        <h1 className="truncate text-[1.7rem] font-semibold tracking-tight">{title}</h1>
        {subtitle ? <p className="mt-1 truncate text-sm text-faint">{subtitle}</p> : null}
      </div>
      {action}
    </header>
  )
}
