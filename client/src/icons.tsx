/**
 * Iconos de la app (Phosphor). Se exportan con los nombres que ya usaba la
 * interfaz. Por defecto van en trazo grueso ("bold"); `fill="currentColor"`
 * (como en los botones de reproducir) los pasa a la variante rellena.
 */
import type { ComponentType, CSSProperties } from 'react'
import * as P from '@phosphor-icons/react'

export interface IconProps {
  size?: number
  fill?: string
  strokeWidth?: number
  className?: string
  style?: CSSProperties
  weight?: P.IconWeight
}

function make(Icon: P.Icon): ComponentType<IconProps> {
  const Wrapped = ({ fill, strokeWidth: _s, weight, size = 20, ...rest }: IconProps) => (
    <Icon size={size} weight={weight || (fill && fill !== 'none' ? 'fill' : 'bold')} {...rest} />
  )
  return Wrapped
}

export const ArrowRight = make(P.ArrowRight)
export const Check = make(P.Check)
export const ChevronDown = make(P.CaretDown)
export const ChevronLeft = make(P.CaretLeft)
export const ChevronRight = make(P.CaretRight)
export const Clock3 = make(P.ClockCounterClockwise)
export const Copy = make(P.CopySimple)
export const Disc3 = make(P.VinylRecord)
export const HardDrive = make(P.HardDrives)
export const Headphones = make(P.Headphones)
export const Heart = make(P.Heart)
export const Home = make(P.House)
export const Info = make(P.Info)
export const Keyboard = make(P.Keyboard)
export const LayoutGrid = make(P.SquaresFour)
export const Library = make(P.Books)
export const List = make(P.ListBullets)
export const ListEnd = make(P.ListPlus)
export const ListMusic = make(P.Playlist)
export const ListPlus = make(P.ArrowBendDownRight)
export const LoaderCircle = make(P.CircleNotch)
export const Maximize2 = make(P.ArrowsOutSimple)
export const MicVocal = make(P.MicrophoneStage)
export const Minus = make(P.Minus)
export const MoreVertical = make(P.DotsThreeVertical)
export const Music2 = make(P.MusicNotes)
export const Paintbrush = make(P.PaintBrush)
export const Palette = make(P.Palette)
export const Pause = make(P.Pause)
export const Pencil = make(P.PencilSimple)
export const Pipette = make(P.Eyedropper)
export const Play = make(P.Play)
export const Plus = make(P.Plus)
export const Radio = make(P.Radio)
export const Repeat = make(P.Repeat)
export const Repeat1 = make(P.RepeatOnce)
export const Search = make(P.MagnifyingGlass)
export const Settings = make(P.GearSix)
export const Shapes = make(P.Shapes)
export const Shuffle = make(P.Shuffle)
export const SkipBack = make(P.SkipBack)
export const SkipForward = make(P.SkipForward)
export const Sparkles = make(P.Sparkle)
export const Square = make(P.Square)
export const Timer = make(P.Timer)
export const Trash2 = make(P.Trash)
export const TrendingUp = make(P.TrendUp)
export const UserRound = make(P.User)
export const Volume1 = make(P.SpeakerLow)
export const Volume2 = make(P.SpeakerHigh)
export const VolumeX = make(P.SpeakerSlash)
export const X = make(P.X)
export const PictureInPicture = make(P.PictureInPicture)
export const Fire = make(P.Fire)
export const Lightning = make(P.Lightning)
export const Waveform = make(P.Waveform)
