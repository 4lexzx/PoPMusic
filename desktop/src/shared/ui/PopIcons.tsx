// Iconos propios (Lucide) con la API de nombres que espera la app.
import type React from 'react'
import * as L from 'lucide-react'

type P = React.ComponentProps<'svg'> & { size?: number | string; weight?: string }
const map: Record<string, React.ComponentType<any>> = {}
map["AddFill"] = L.Plus as React.ComponentType<any>
map["AddLine"] = L.Plus as React.ComponentType<any>
map["AlbumFill"] = L.ListMusic as React.ComponentType<any>
map["ArrowLeftLine"] = L.ChevronLeft as React.ComponentType<any>
map["Back2Line"] = L.ChevronLeft as React.ComponentType<any>
map["Book2"] = L.BookOpen as React.ComponentType<any>
map["BookmarkFill"] = L.BookOpen as React.ComponentType<any>
map["BookmarksFill"] = L.BookOpen as React.ComponentType<any>
map["CheckCircle"] = L.Check as React.ComponentType<any>
map["CheckFill"] = L.Check as React.ComponentType<any>
map["CheckLine"] = L.CheckLine as React.ComponentType<any>
map["CloseCircleFill"] = L.X as React.ComponentType<any>
map["CloseLine"] = L.X as React.ComponentType<any>
map["CopyLine"] = L.Copy as React.ComponentType<any>
map["Database"] = L.Database as React.ComponentType<any>
map["DiscordFill"] = L.Disc3 as React.ComponentType<any>
map["DownLine"] = L.ChevronDown as React.ComponentType<any>
map["Download2Line"] = L.Download as React.ComponentType<any>
map["ExitFill"] = L.LogOut as React.ComponentType<any>
map["ExitLine"] = L.LogOut as React.ComponentType<any>
map["ExternalLinkLine"] = L.Link as React.ComponentType<any>
map["EyeCloseLine"] = L.X as React.ComponentType<any>
map["EyeLine"] = L.Eye as React.ComponentType<any>
map["FastForwardLine"] = L.SkipForward as React.ComponentType<any>
map["FastRewindLine"] = L.SkipBack as React.ComponentType<any>
map["FileMusicLine"] = L.Circle as React.ComponentType<any>
map["FireFill"] = L.Flame as React.ComponentType<any>
map["FolderCheckFill"] = L.Check as React.ComponentType<any>
map["FolderOpenLine"] = L.Folder as React.ComponentType<any>
map["GithubFill"] = L.Circle as React.ComponentType<any>
map["GridFill"] = L.Grid3x3 as React.ComponentType<any>
map["GridLine"] = L.Grid3x3 as React.ComponentType<any>
map["HeartFill"] = L.Heart as React.ComponentType<any>
map["HeartLine"] = L.Heart as React.ComponentType<any>
map["Home7Fill"] = L.House as React.ComponentType<any>
map["InfoCircle"] = L.Info as React.ComponentType<any>
map["InformationLine"] = L.Info as React.ComponentType<any>
map["KeyboardFill"] = L.Keyboard as React.ComponentType<any>
map["LeftLine"] = L.ChevronLeft as React.ComponentType<any>
map["LinkLine"] = L.Link as React.ComponentType<any>
map["ListCheckFill"] = L.Check as React.ComponentType<any>
map["ListCheckLine"] = L.Check as React.ComponentType<any>
map["Magic2Fill"] = L.Sparkles as React.ComponentType<any>
map["MaximizeSquare"] = L.Maximize as React.ComponentType<any>
map["MaximizeSquare3"] = L.Maximize as React.ComponentType<any>
map["MinimizeLine"] = L.Minimize as React.ComponentType<any>
map["More1Fill"] = L.Ellipsis as React.ComponentType<any>
map["More2Fill"] = L.Ellipsis as React.ComponentType<any>
map["Music2Fill"] = L.Circle as React.ComponentType<any>
map["MusicFill"] = L.Circle as React.ComponentType<any>
map["MusicNote"] = L.Circle as React.ComponentType<any>
map["NewFolderLine"] = L.Folder as React.ComponentType<any>
map["Palette"] = L.Palette as React.ComponentType<any>
map["PauseFill"] = L.Pause as React.ComponentType<any>
map["Play"] = L.Play as React.ComponentType<any>
map["PlayCircleFill"] = L.Circle as React.ComponentType<any>
map["PlayFill"] = L.Play as React.ComponentType<any>
map["PlaybackSpeed"] = L.ChevronLeft as React.ComponentType<any>
map["Playlist2Fill"] = L.ListMusic as React.ComponentType<any>
map["PlaylistFill"] = L.ListMusic as React.ComponentType<any>
map["QuestionCircle"] = L.Circle as React.ComponentType<any>
map["Refresh1Line"] = L.RefreshCw as React.ComponentType<any>
map["Refresh2Line"] = L.RefreshCw as React.ComponentType<any>
map["Repeat"] = L.Repeat as React.ComponentType<any>
map["RepeatFill"] = L.Repeat as React.ComponentType<any>
map["RepeatOne"] = L.Repeat as React.ComponentType<any>
map["RightLine"] = L.ChevronRight as React.ComponentType<any>
map["SafeShieldLine"] = L.Shield as React.ComponentType<any>
map["Search2Line"] = L.Search as React.ComponentType<any>
map["Search3Fill"] = L.Search as React.ComponentType<any>
map["Search3Line"] = L.Search as React.ComponentType<any>
map["ServerLine"] = L.Server as React.ComponentType<any>
map["Settings4Fill"] = L.Settings as React.ComponentType<any>
map["ShareForwardLine"] = L.Share2 as React.ComponentType<any>
map["Shuffle"] = L.Shuffle as React.ComponentType<any>
map["Sidebar"] = L.Circle as React.ComponentType<any>
map["SkipNext"] = L.SkipForward as React.ComponentType<any>
map["SkipPrevious"] = L.SkipBack as React.ComponentType<any>
map["SparklesFill"] = L.Sparkles as React.ComponentType<any>
map["TelegramFill"] = L.Send as React.ComponentType<any>
map["TimeFill"] = L.Clock as React.ComponentType<any>
map["TimeLine"] = L.Clock as React.ComponentType<any>
map["TrashBin2"] = L.Trash as React.ComponentType<any>
map["TrophyFill"] = L.Trophy as React.ComponentType<any>
map["Tuning2"] = L.Settings as React.ComponentType<any>
map["User1Line"] = L.User as React.ComponentType<any>
map["User2Fill"] = L.User as React.ComponentType<any>
map["UserQuestionFill"] = L.Circle as React.ComponentType<any>
map["VolumeCross"] = L.Volume2 as React.ComponentType<any>
map["VolumeLoud"] = L.Volume2 as React.ComponentType<any>
map["VolumeSmall"] = L.Volume2 as React.ComponentType<any>
map["WarningFill"] = L.Circle as React.ComponentType<any>
map["Widget5"] = L.Circle as React.ComponentType<any>
map["WifiOffLine"] = L.WifiOff as React.ComponentType<any>
map["World2Line"] = L.Globe as React.ComponentType<any>
map["YoutubeFill"] = L.Circle as React.ComponentType<any>
map["YoutubeLine"] = L.Circle as React.ComponentType<any>

export const __iconMap = map
export const AddFill = (props: P) => { const C = map["AddFill"] || L.Circle; return <C {...props} /> }
export const AddLine = (props: P) => { const C = map["AddLine"] || L.Circle; return <C {...props} /> }
export const AlbumFill = (props: P) => { const C = map["AlbumFill"] || L.Circle; return <C {...props} /> }
export const ArrowLeftLine = (props: P) => { const C = map["ArrowLeftLine"] || L.Circle; return <C {...props} /> }
export const Back2Line = (props: P) => { const C = map["Back2Line"] || L.Circle; return <C {...props} /> }
export const Book2 = (props: P) => { const C = map["Book2"] || L.Circle; return <C {...props} /> }
export const BookmarkFill = (props: P) => { const C = map["BookmarkFill"] || L.Circle; return <C {...props} /> }
export const BookmarksFill = (props: P) => { const C = map["BookmarksFill"] || L.Circle; return <C {...props} /> }
export const CheckCircle = (props: P) => { const C = map["CheckCircle"] || L.Circle; return <C {...props} /> }
export const CheckFill = (props: P) => { const C = map["CheckFill"] || L.Circle; return <C {...props} /> }
export const CheckLine = (props: P) => { const C = map["CheckLine"] || L.Circle; return <C {...props} /> }
export const CloseCircleFill = (props: P) => { const C = map["CloseCircleFill"] || L.Circle; return <C {...props} /> }
export const CloseLine = (props: P) => { const C = map["CloseLine"] || L.Circle; return <C {...props} /> }
export const CopyLine = (props: P) => { const C = map["CopyLine"] || L.Circle; return <C {...props} /> }
export const Database = (props: P) => { const C = map["Database"] || L.Circle; return <C {...props} /> }
export const DiscordFill = (props: P) => { const C = map["DiscordFill"] || L.Circle; return <C {...props} /> }
export const DownLine = (props: P) => { const C = map["DownLine"] || L.Circle; return <C {...props} /> }
export const Download2Line = (props: P) => { const C = map["Download2Line"] || L.Circle; return <C {...props} /> }
export const ExitFill = (props: P) => { const C = map["ExitFill"] || L.Circle; return <C {...props} /> }
export const ExitLine = (props: P) => { const C = map["ExitLine"] || L.Circle; return <C {...props} /> }
export const ExternalLinkLine = (props: P) => { const C = map["ExternalLinkLine"] || L.Circle; return <C {...props} /> }
export const EyeCloseLine = (props: P) => { const C = map["EyeCloseLine"] || L.Circle; return <C {...props} /> }
export const EyeLine = (props: P) => { const C = map["EyeLine"] || L.Circle; return <C {...props} /> }
export const FastForwardLine = (props: P) => { const C = map["FastForwardLine"] || L.Circle; return <C {...props} /> }
export const FastRewindLine = (props: P) => { const C = map["FastRewindLine"] || L.Circle; return <C {...props} /> }
export const FileMusicLine = (props: P) => { const C = map["FileMusicLine"] || L.Circle; return <C {...props} /> }
export const FireFill = (props: P) => { const C = map["FireFill"] || L.Circle; return <C {...props} /> }
export const FolderCheckFill = (props: P) => { const C = map["FolderCheckFill"] || L.Circle; return <C {...props} /> }
export const FolderOpenLine = (props: P) => { const C = map["FolderOpenLine"] || L.Circle; return <C {...props} /> }
export const GithubFill = (props: P) => { const C = map["GithubFill"] || L.Circle; return <C {...props} /> }
export const GridFill = (props: P) => { const C = map["GridFill"] || L.Circle; return <C {...props} /> }
export const GridLine = (props: P) => { const C = map["GridLine"] || L.Circle; return <C {...props} /> }
export const HeartFill = (props: P) => { const C = map["HeartFill"] || L.Circle; return <C {...props} /> }
export const HeartLine = (props: P) => { const C = map["HeartLine"] || L.Circle; return <C {...props} /> }
export const Home7Fill = (props: P) => { const C = map["Home7Fill"] || L.Circle; return <C {...props} /> }
export const InfoCircle = (props: P) => { const C = map["InfoCircle"] || L.Circle; return <C {...props} /> }
export const InformationLine = (props: P) => { const C = map["InformationLine"] || L.Circle; return <C {...props} /> }
export const KeyboardFill = (props: P) => { const C = map["KeyboardFill"] || L.Circle; return <C {...props} /> }
export const LeftLine = (props: P) => { const C = map["LeftLine"] || L.Circle; return <C {...props} /> }
export const LinkLine = (props: P) => { const C = map["LinkLine"] || L.Circle; return <C {...props} /> }
export const ListCheckFill = (props: P) => { const C = map["ListCheckFill"] || L.Circle; return <C {...props} /> }
export const ListCheckLine = (props: P) => { const C = map["ListCheckLine"] || L.Circle; return <C {...props} /> }
export const Magic2Fill = (props: P) => { const C = map["Magic2Fill"] || L.Circle; return <C {...props} /> }
export const MaximizeSquare = (props: P) => { const C = map["MaximizeSquare"] || L.Circle; return <C {...props} /> }
export const MaximizeSquare3 = (props: P) => { const C = map["MaximizeSquare3"] || L.Circle; return <C {...props} /> }
export const MinimizeLine = (props: P) => { const C = map["MinimizeLine"] || L.Circle; return <C {...props} /> }
export const More1Fill = (props: P) => { const C = map["More1Fill"] || L.Circle; return <C {...props} /> }
export const More2Fill = (props: P) => { const C = map["More2Fill"] || L.Circle; return <C {...props} /> }
export const Music2Fill = (props: P) => { const C = map["Music2Fill"] || L.Circle; return <C {...props} /> }
export const MusicFill = (props: P) => { const C = map["MusicFill"] || L.Circle; return <C {...props} /> }
export const MusicNote = (props: P) => { const C = map["MusicNote"] || L.Circle; return <C {...props} /> }
export const NewFolderLine = (props: P) => { const C = map["NewFolderLine"] || L.Circle; return <C {...props} /> }
export const Palette = (props: P) => { const C = map["Palette"] || L.Circle; return <C {...props} /> }
export const PauseFill = (props: P) => { const C = map["PauseFill"] || L.Circle; return <C {...props} /> }
export const Play = (props: P) => { const C = map["Play"] || L.Circle; return <C {...props} /> }
export const PlayCircleFill = (props: P) => { const C = map["PlayCircleFill"] || L.Circle; return <C {...props} /> }
export const PlayFill = (props: P) => { const C = map["PlayFill"] || L.Circle; return <C {...props} /> }
export const PlaybackSpeed = (props: P) => { const C = map["PlaybackSpeed"] || L.Circle; return <C {...props} /> }
export const Playlist2Fill = (props: P) => { const C = map["Playlist2Fill"] || L.Circle; return <C {...props} /> }
export const PlaylistFill = (props: P) => { const C = map["PlaylistFill"] || L.Circle; return <C {...props} /> }
export const QuestionCircle = (props: P) => { const C = map["QuestionCircle"] || L.Circle; return <C {...props} /> }
export const Refresh1Line = (props: P) => { const C = map["Refresh1Line"] || L.Circle; return <C {...props} /> }
export const Refresh2Line = (props: P) => { const C = map["Refresh2Line"] || L.Circle; return <C {...props} /> }
export const Repeat = (props: P) => { const C = map["Repeat"] || L.Circle; return <C {...props} /> }
export const RepeatFill = (props: P) => { const C = map["RepeatFill"] || L.Circle; return <C {...props} /> }
export const RepeatOne = (props: P) => { const C = map["RepeatOne"] || L.Circle; return <C {...props} /> }
export const RightLine = (props: P) => { const C = map["RightLine"] || L.Circle; return <C {...props} /> }
export const SafeShieldLine = (props: P) => { const C = map["SafeShieldLine"] || L.Circle; return <C {...props} /> }
export const Search2Line = (props: P) => { const C = map["Search2Line"] || L.Circle; return <C {...props} /> }
export const Search3Fill = (props: P) => { const C = map["Search3Fill"] || L.Circle; return <C {...props} /> }
export const Search3Line = (props: P) => { const C = map["Search3Line"] || L.Circle; return <C {...props} /> }
export const ServerLine = (props: P) => { const C = map["ServerLine"] || L.Circle; return <C {...props} /> }
export const Settings4Fill = (props: P) => { const C = map["Settings4Fill"] || L.Circle; return <C {...props} /> }
export const ShareForwardLine = (props: P) => { const C = map["ShareForwardLine"] || L.Circle; return <C {...props} /> }
export const Shuffle = (props: P) => { const C = map["Shuffle"] || L.Circle; return <C {...props} /> }
export const Sidebar = (props: P) => { const C = map["Sidebar"] || L.Circle; return <C {...props} /> }
export const SkipNext = (props: P) => { const C = map["SkipNext"] || L.Circle; return <C {...props} /> }
export const SkipPrevious = (props: P) => { const C = map["SkipPrevious"] || L.Circle; return <C {...props} /> }
export const SparklesFill = (props: P) => { const C = map["SparklesFill"] || L.Circle; return <C {...props} /> }
export const TelegramFill = (props: P) => { const C = map["TelegramFill"] || L.Circle; return <C {...props} /> }
export const TimeFill = (props: P) => { const C = map["TimeFill"] || L.Circle; return <C {...props} /> }
export const TimeLine = (props: P) => { const C = map["TimeLine"] || L.Circle; return <C {...props} /> }
export const TrashBin2 = (props: P) => { const C = map["TrashBin2"] || L.Circle; return <C {...props} /> }
export const TrophyFill = (props: P) => { const C = map["TrophyFill"] || L.Circle; return <C {...props} /> }
export const Tuning2 = (props: P) => { const C = map["Tuning2"] || L.Circle; return <C {...props} /> }
export const User1Line = (props: P) => { const C = map["User1Line"] || L.Circle; return <C {...props} /> }
export const User2Fill = (props: P) => { const C = map["User2Fill"] || L.Circle; return <C {...props} /> }
export const UserQuestionFill = (props: P) => { const C = map["UserQuestionFill"] || L.Circle; return <C {...props} /> }
export const VolumeCross = (props: P) => { const C = map["VolumeCross"] || L.Circle; return <C {...props} /> }
export const VolumeLoud = (props: P) => { const C = map["VolumeLoud"] || L.Circle; return <C {...props} /> }
export const VolumeSmall = (props: P) => { const C = map["VolumeSmall"] || L.Circle; return <C {...props} /> }
export const WarningFill = (props: P) => { const C = map["WarningFill"] || L.Circle; return <C {...props} /> }
export const Widget5 = (props: P) => { const C = map["Widget5"] || L.Circle; return <C {...props} /> }
export const WifiOffLine = (props: P) => { const C = map["WifiOffLine"] || L.Circle; return <C {...props} /> }
export const World2Line = (props: P) => { const C = map["World2Line"] || L.Circle; return <C {...props} /> }
export const YoutubeFill = (props: P) => { const C = map["YoutubeFill"] || L.Circle; return <C {...props} /> }
export const YoutubeLine = (props: P) => { const C = map["YoutubeLine"] || L.Circle; return <C {...props} /> }
