import {
  AudioLines,
  BookOpen,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  Clock3,
  Disc3,
  Heart,
  Home,
  Image as ImageLucide,
  Info,
  LayoutGrid,
  Library,
  Link2,
  List,
  ListMusic,
  Loader2,
  Mic,
  Minus,
  Monitor,
  Moon,
  MoreHorizontal,
  Music2,
  Pause,
  Play,
  Plus,
  Radio,
  Repeat,
  Repeat1,
  Search,
  Server,
  Settings2,
  Shuffle,
  SkipBack,
  SkipForward,
  Sparkles,
  Sun,
  Trash2,
  UserRound,
  Volume2,
  VolumeX,
  X,
  type LucideProps,
} from 'lucide-react'

/** Iconos Lucide (misma librería que usan los reproductores profesionales). */
export type IconProps = LucideProps

export const HomeIcon = Home
export const SearchIcon = Search
export const LibraryIcon = Library
export const DiscIcon = Disc3
export const PlayIcon = Play
export const PauseIcon = Pause
export const NextIcon = SkipForward
export const PrevIcon = SkipBack
export const ShuffleIcon = Shuffle
export const RepeatIcon = Repeat
export const RepeatOneIcon = Repeat1
export const HeartIcon = Heart
export const HeartFilledIcon = (props: IconProps) => <Heart {...props} fill="currentColor" />
export const PlusIcon = Plus
export const MinusIcon = Minus
export const MoreIcon = MoreHorizontal
export const DotsIcon = MoreHorizontal
export const ChevronLeftIcon = ChevronLeft
export const ChevronDownIcon = ChevronDown
export const ChevronRightIcon = ChevronRight
export const VolumeIcon = Volume2
export const VolumeMuteIcon = VolumeX
export const ListMusicIcon = ListMusic
export const CloseIcon = X
export const LinkIcon = Link2
export const MusicIcon = Music2
export const TrashIcon = Trash2
export const CheckIcon = Check
export const SettingsIcon = Settings2
export const RadioIcon = Radio
export const MicIcon = Mic
export const DeviceIcon = Monitor
export const WaveformIcon = AudioLines
export const ClockIcon = Clock3
export const GridIcon = LayoutGrid
export const ListIcon = List
export const ArtistIcon = UserRound
export const MoonIcon = Moon
export const SunIcon = Sun
export const SparklesIcon = Sparkles
export const SpinnerIcon = (props: IconProps) => (
  <Loader2 {...props} className={`animate-spin ${props.className ?? ''}`} />
)
export const ImageIcon = ImageLucide
export const InfoIcon = Info
export const HelpIcon = CircleHelp
export const BookIcon = BookOpen
export const ServerIcon = Server
export const UserIcon = UserRound

/** Marcas: Lucide no incluye logos de empresas, se mantienen como SVG propio. */
export const YouTubeIcon = ({ size = 22, ...props }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
    <path d="M21.6 7.2a2.5 2.5 0 0 0-1.76-1.77C18.28 5 12 5 12 5s-6.28 0-7.84.43A2.5 2.5 0 0 0 2.4 7.2 26 26 0 0 0 2 12a26 26 0 0 0 .4 4.8 2.5 2.5 0 0 0 1.76 1.77C5.72 19 12 19 12 19s6.28 0 7.84-.43a2.5 2.5 0 0 0 1.76-1.77A26 26 0 0 0 22 12a26 26 0 0 0-.4-4.8ZM10 15V9l5.2 3z" />
  </svg>
)

export const SpotifyIcon = ({ size = 22, ...props }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
    <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm4.6 14.42a.75.75 0 0 1-1.03.25c-2.82-1.72-6.37-2.11-10.55-1.16a.75.75 0 1 1-.33-1.46c4.57-1.04 8.5-.6 11.66 1.34a.75.75 0 0 1 .25 1.03Zm1.23-2.73a.94.94 0 0 1-1.29.31c-3.23-1.98-8.15-2.56-11.97-1.4a.94.94 0 1 1-.54-1.79c4.36-1.32 9.78-.67 13.49 1.6a.94.94 0 0 1 .31 1.28Zm.1-2.85C14.06 8.54 7.9 8.33 4.2 9.45a1.12 1.12 0 1 1-.65-2.15c4.25-1.29 11.05-1.04 15.4 1.54a1.12 1.12 0 0 1-1.14 1.92Z" />
  </svg>
)

export const GoogleIcon = ({ size = 20, ...props }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true" {...props}>
    <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.1 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.3-.4-3.5z" />
    <path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 18.9 12 24 12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.1 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
    <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z" />
    <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C41.1 34.8 44 29.9 44 24c0-1.3-.1-2.3-.4-3.5z" />
  </svg>
)
