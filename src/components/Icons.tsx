import type { ReactNode, SVGProps } from 'react'

export type IconProps = SVGProps<SVGSVGElement> & { size?: number }

function Svg({ size = 22, children, ...props }: IconProps & { children: ReactNode }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {children}
    </svg>
  )
}

export const HomeIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V20a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9.5" />
    <path d="M9.5 21v-6h5v6" />
  </Svg>
)

export const SearchIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="11" cy="11" r="7" />
    <path d="m20.5 20.5-4-4" />
  </Svg>
)

export const LibraryIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="4" y="4" width="5" height="16" rx="1.4" />
    <rect x="11" y="4" width="5" height="16" rx="1.4" />
    <path d="m17.5 5.4 2.6 14" />
  </Svg>
)

export const DiscIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="9" />
    <circle cx="12" cy="12" r="2.6" />
  </Svg>
)

export const PlayIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M8 5.4v13.2L19 12z" fill="currentColor" stroke="none" />
  </Svg>
)

export const PauseIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="7" y="5" width="3.4" height="14" rx="1.2" fill="currentColor" stroke="none" />
    <rect x="13.6" y="5" width="3.4" height="14" rx="1.2" fill="currentColor" stroke="none" />
  </Svg>
)

export const NextIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M6 5.4v13.2L15 12z" fill="currentColor" stroke="none" />
    <rect x="16.6" y="5" width="2.4" height="14" rx="1.2" fill="currentColor" stroke="none" />
  </Svg>
)

export const PrevIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M18 5.4v13.2L9 12z" fill="currentColor" stroke="none" />
    <rect x="5" y="5" width="2.4" height="14" rx="1.2" fill="currentColor" stroke="none" />
  </Svg>
)

export const ShuffleIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M16 4h4v4" />
    <path d="M4 20 20 4" />
    <path d="M16 20h4v-4" />
    <path d="m4 4 5 5" />
    <path d="m15 15 5 5" />
  </Svg>
)

export const RepeatIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M17 3.5 20 6l-3 2.5" />
    <path d="M20 6H7a4 4 0 0 0-4 4v1" />
    <path d="M7 20.5 4 18l3-2.5" />
    <path d="M4 18h13a4 4 0 0 0 4-4v-1" />
  </Svg>
)

export const RepeatOneIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M17 3.5 20 6l-3 2.5" />
    <path d="M20 6H7a4 4 0 0 0-4 4v1" />
    <path d="M7 20.5 4 18l3-2.5" />
    <path d="M4 18h13a4 4 0 0 0 4-4v-1" />
    <path d="M12 10v4" />
    <path d="M11 11l1-1" />
  </Svg>
)

export const HeartIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M12 20s-7-4.4-7-9.5A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7 3.5C19 15.6 12 20 12 20Z" />
  </Svg>
)

export const HeartFilledIcon = (props: IconProps) => (
  <Svg {...props}>
    <path
      d="M12 20s-7-4.4-7-9.5A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7 3.5C19 15.6 12 20 12 20Z"
      fill="currentColor"
      stroke="none"
    />
  </Svg>
)

export const PlusIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M12 5v14M5 12h14" />
  </Svg>
)

export const MinusIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M5 12h14" />
  </Svg>
)

export const MoreIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="5.5" r="1.4" fill="currentColor" stroke="none" />
    <circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" />
    <circle cx="12" cy="18.5" r="1.4" fill="currentColor" stroke="none" />
  </Svg>
)

export const ChevronLeftIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="m15 5-7 7 7 7" />
  </Svg>
)

export const ChevronDownIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="m5 9 7 7 7-7" />
  </Svg>
)

export const VolumeIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 9.5h3.5L12 5.5v13L7.5 14.5H4z" />
    <path d="M16 9a4 4 0 0 1 0 6" />
    <path d="M18.5 6.5a7.5 7.5 0 0 1 0 11" />
  </Svg>
)

export const VolumeMuteIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 9.5h3.5L12 5.5v13L7.5 14.5H4z" />
    <path d="m16.5 9.5 4 5M20.5 9.5l-4 5" />
  </Svg>
)

export const ListMusicIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 6h9M4 11h9M4 16h6" />
    <circle cx="18" cy="16.5" r="2.5" />
    <path d="M20.5 16.5V7l-2 .8" />
  </Svg>
)

export const CloseIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Svg>
)

export const LinkIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M10.5 13.5a3.5 3.5 0 0 0 5 0l3-3a3.5 3.5 0 0 0-5-5l-1.2 1.2" />
    <path d="M13.5 10.5a3.5 3.5 0 0 0-5 0l-3 3a3.5 3.5 0 0 0 5 5l1.2-1.2" />
  </Svg>
)

export const MusicIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M9 18V6l10-2v12" />
    <circle cx="6.5" cy="18" r="2.5" />
    <circle cx="16.5" cy="16" r="2.5" />
  </Svg>
)

export const TrashIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13" />
  </Svg>
)

export const CheckIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="m5 12.5 4.5 4.5L19 7" />
  </Svg>
)

export const DotsIcon = MoreIcon

export const ChevronRightIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="m9 5 7 7-7 7" />
  </Svg>
)

export const SettingsIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.03 1.56V21a2 2 0 0 1-4 0v-.1A1.7 1.7 0 0 0 8.9 19.3a1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.7 15a1.7 1.7 0 0 0-1.56-1.03H3a2 2 0 0 1 0-4h.1A1.7 1.7 0 0 0 4.7 8.9a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1.03-1.56V3a2 2 0 0 1 4 0v.1A1.7 1.7 0 0 0 15 4.7a1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 9v.1a1.7 1.7 0 0 0 1.56 1.03H21a2 2 0 0 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1.03Z" />
  </Svg>
)

export const RadioIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="2.4" />
    <path d="M7.8 7.8a6 6 0 0 0 0 8.4M16.2 16.2a6 6 0 0 0 0-8.4" />
    <path d="M5.2 5.2a9.6 9.6 0 0 0 0 13.6M18.8 18.8a9.6 9.6 0 0 0 0-13.6" />
  </Svg>
)

export const MicIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="9" y="3" width="6" height="11" rx="3" />
    <path d="M5.5 11.5a6.5 6.5 0 0 0 13 0" />
    <path d="M12 18v3" />
  </Svg>
)

export const DeviceIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="3" y="5" width="18" height="12" rx="2" />
    <path d="M8 21h8M12 17v4" />
  </Svg>
)

export const WaveformIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 10v4M8 6.5v11M12 4v16M16 6.5v11M20 10v4" />
  </Svg>
)

export const ClockIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="8.5" />
    <path d="M12 7.5V12l3 1.8" />
  </Svg>
)

export const GridIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="4" y="4" width="7" height="7" rx="1.6" />
    <rect x="13" y="4" width="7" height="7" rx="1.6" />
    <rect x="4" y="13" width="7" height="7" rx="1.6" />
    <rect x="13" y="13" width="7" height="7" rx="1.6" />
  </Svg>
)

export const ListIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 6.5h16M4 12h16M4 17.5h16" />
  </Svg>
)

export const ArtistIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="8.5" r="3.6" />
    <path d="M5.5 19.5a6.5 6.5 0 0 1 13 0" />
  </Svg>
)

export const MoonIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5Z" />
  </Svg>
)

export const SunIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" />
  </Svg>
)

export const SparklesIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M12 4.5 13.6 9 18 10.6 13.6 12.2 12 16.7 10.4 12.2 6 10.6 10.4 9z" />
    <path d="M18.5 4v2.6M17.2 5.3h2.6M5.5 17v2M4.5 18h2" />
  </Svg>
)

export const YouTubeIcon = ({ size = 22, ...props }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="currentColor"
    aria-hidden="true"
    {...props}
  >
    <path d="M21.6 7.2a2.5 2.5 0 0 0-1.76-1.77C18.28 5 12 5 12 5s-6.28 0-7.84.43A2.5 2.5 0 0 0 2.4 7.2 26 26 0 0 0 2 12a26 26 0 0 0 .4 4.8 2.5 2.5 0 0 0 1.76 1.77C5.72 19 12 19 12 19s6.28 0 7.84-.43a2.5 2.5 0 0 0 1.76-1.77A26 26 0 0 0 22 12a26 26 0 0 0-.4-4.8ZM10 15V9l5.2 3z" />
  </svg>
)

export const SpotifyIcon = ({ size = 22, ...props }: IconProps) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="currentColor"
    aria-hidden="true"
    {...props}
  >
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm4.6 14.42a.75.75 0 0 1-1.03.25c-2.82-1.72-6.37-2.11-10.55-1.16a.75.75 0 1 1-.33-1.46c4.57-1.04 8.5-.6 11.66 1.34a.75.75 0 0 1 .25 1.03Zm1.23-2.73a.94.94 0 0 1-1.29.31c-3.23-1.98-8.15-2.56-11.97-1.4a.94.94 0 1 1-.54-1.79c4.36-1.32 9.78-.67 13.49 1.6a.94.94 0 0 1 .31 1.28Zm.1-2.85C14.06 8.54 7.9 8.33 4.2 9.45a1.12 1.12 0 1 1-.65-2.15c4.25-1.29 11.05-1.04 15.4 1.54a1.12 1.12 0 0 1-1.14 1.92Z" />
  </svg>
)

export const SpinnerIcon = ({ size = 22, className, ...props }: IconProps) => (
  <Svg size={size} className={className} {...props}>
    <path d="M12 3a9 9 0 1 0 9 9" />
  </Svg>
)

export const ImageIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="3.5" y="4.5" width="17" height="15" rx="2.4" />
    <circle cx="8.5" cy="9.5" r="1.6" />
    <path d="m4 17 4.5-4.5a2 2 0 0 1 2.8 0L16 17" />
    <path d="m14 15 1.8-1.8a2 2 0 0 1 2.8 0L20 14.4" />
  </Svg>
)

export const InfoIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 11v5" />
    <circle cx="12" cy="7.8" r="0.9" fill="currentColor" stroke="none" />
  </Svg>
)

export const HelpIcon = (props: IconProps) => (
  <Svg {...props}>
    <circle cx="12" cy="12" r="9" />
    <path d="M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-.9.8-.9 1.5" />
    <circle cx="12" cy="16.6" r="0.9" fill="currentColor" stroke="none" />
  </Svg>
)

export const BookIcon = (props: IconProps) => (
  <Svg {...props}>
    <path d="M4 5.5A2 2 0 0 1 6 4h13v15H6a2 2 0 0 0-2 2z" />
    <path d="M4 6.5v13" />
    <path d="M8 8h7M8 11.5h7" />
  </Svg>
)

export const ServerIcon = (props: IconProps) => (
  <Svg {...props}>
    <rect x="4" y="5" width="16" height="5" rx="1.6" />
    <rect x="4" y="14" width="16" height="5" rx="1.6" />
    <path d="M8 7.5h.01M8 16.5h.01" />
  </Svg>
)
