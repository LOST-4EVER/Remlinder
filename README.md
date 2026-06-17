# Remlinder

A minimal, animated reminder app for Android built with Jetpack Compose.

## Features

- **Countdown Timer** — Circular timer wheel with play/pause/reset controls
- **Reminders** — Create reminders with a title, description, and media type
- **Media Types** — Text, Audio, Image, and Task reminders with appropriate icons
- **Full-screen Alarm** — Lock screen alarm activity with snooze/dismiss
- **Animations** — Custom "wiggly worm" canvas animation for idle and alarm states
- **Persistent Storage** — Room database for reminder storage
- **Background Scheduling** — AlarmManager for exact alarms, WorkManager for daily cache maintenance
- **Boot Recovery** — Reschedules pending alarms after device reboot
- **Notifications** — High-priority notification channel with full-screen intent

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Navigation Compose
- **Database:** Room (with KSP)
- **Background Work:** WorkManager
- **Alarms:** AlarmManager (exact + while idle)
- **Image Loading:** Coil
- **Build:** Gradle 8.10, AGP 8.7.3
- **Min SDK:** 26 | **Target SDK:** 35 | **Compile SDK:** 35

## Screenshots

| Timer & Reminders | Create Reminder | Alarm |
|---|---|---|
| Circular timer with animated wheel, reminder list, FAB to add | Bottom sheet with title, description, media type picker, duration selector | Full-screen lock screen alarm with snooze/dismiss |

## Building

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/remlinder/app/
├── RemlinderApp.kt              # Application class, notification channel
├── MainActivity.kt               # Single activity entry point
├── alarm/
│   └── AlarmReceiver.kt          # BroadcastReceiver for alarm intents
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database singleton
│   │   ├── ReminderDao.kt        # Room DAO
│   │   └── ReminderEntity.kt     # Reminder entity + MediaType enum
│   └── repository/
│       └── ReminderRepository.kt # Repository layer
├── ui/
│   ├── components/
│   │   ├── ReminderCard.kt       # Reminder list item card
│   │   ├── TimerWheel.kt         # Circular timer wheel canvas
│   │   └── WigglyWormAnimation.kt# Animated worm canvas effect
│   ├── screens/
│   │   ├── MainTimerScreen.kt    # Main timer + reminder list screen
│   │   └── AlarmFullScreenActivity.kt # Lock screen alarm activity
│   └── theme/
│       ├── Color.kt              # Theme colors
│       ├── Theme.kt              # Material 3 theme
│       └── Type.kt               # Typography
├── viewmodel/
│   └── MainViewModel.kt          # Timer + reminder logic
└── worker/
    ├── BootReceiver.kt           # Re-schedules on device boot
    ├── DailyCacheWorker.kt       # Daily reminder cache worker
    └── ReminderWorker.kt         # Checks for due reminders
```

## CI/CD

GitHub Actions workflow builds both Debug and Release APKs on every push to `main`. Auto-versioning bumps the minor tag on each push. APKs are uploaded as GitHub Release artifacts.

## Permissions

- `POST_NOTIFICATIONS` — Alarm notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — Precise alarm timing
- `RECEIVE_BOOT_COMPLETED` — Re-schedule after reboot
- `FOREGROUND_SERVICE` — Foreground service support
- `RECORD_AUDIO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_IMAGES` — Media attachments
