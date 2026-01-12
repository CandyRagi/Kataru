# Kataru 🎧

A slick audiobook player for Android. Built because I got tired of clunky apps that look like they're from 2015.

## What it does

**The basics:**
- Plays your local audiobooks (MP3, M4B, M4A, FLAC, and more)
- Remembers exactly where you stopped, every time
- Looks good doing it

**Library:**
- Point it to a folder and it finds your books
- Scans subfolders too — if you have a folder with an M4B and a cover image, it picks it up automatically
- Grid or list view, your choice
- Search that actually works

**Player:**
- Full-screen player with album art, progress bar, the whole deal
- Mini player that sticks to the bottom so you don't lose your spot
- Skip forward/back 10 seconds (because who needs next/previous track for audiobooks?)
- Playback speed control — go from 0.5x to 2x
- Volume control without leaving the app

**Widget:**
- Home screen widget that looks clean
- Play/pause, skip ±10s, all from your home screen
- Shows the current book and cover art
- Works even when the app is closed

**Customization:**
- Pick your accent color
- Dark mode (obviously)
- Glassmorphic UI throughout — transparent backgrounds, subtle blurs, rounded everything

## Tech stuff

- Kotlin + Jetpack Compose
- Media3 ExoPlayer for audio
- Room for history/bookmarks
- Coil for image loading
- MVVM architecture

---

Made by someone who just wanted a nice audiobook player.
