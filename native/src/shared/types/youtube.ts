export interface YouTubeSearchItem {
  id?: { videoId?: string }
  snippet?: {
    title?: string
    channelTitle?: string
    thumbnails?: Record<string, { url?: string }>
  }
}

export interface YouTubeVideoItem {
  id?: string
  contentDetails?: { duration?: string }
  snippet?: {
    title?: string
    channelTitle?: string
    thumbnails?: Record<string, { url?: string }>
  }
}

export interface YouTubePlaylistItem {
  snippet?: {
    title?: string
    videoOwnerChannelTitle?: string
    channelTitle?: string
    resourceId?: { videoId?: string }
    thumbnails?: Record<string, { url?: string }>
  }
}
