# Cresto

Cresto is a modern Android to-do app built with Kotlin and Jetpack Compose. It combines a clean task manager with AI-assisted capture, calendar sync, reminders, lightweight productivity insights, and a custom visual system called Glasense.

> Cresto is currently in active alpha development. Some details may change quickly as features are refined.

## Highlights

- Clean Compose UI with Cresto's Glasense design system and optional Liquid Glass styling.
- Full to-do workflow.
- Rich task details.
- AI task extraction.
- Share todos.
- Calendar view.
- Manual or automatic sync of todos into the Android system calendar.
- Insights page.
- Appearance customization.

## AI Capture

Cresto can turn unstructured content into todos with due dates, times, reminders, and subtasks.

Supported capture paths:

- In-app text extraction from the add flow.
- Image extraction from selected or shared images.
- Android share sheet integration for `text/*`, `image/*`, and multiple images.
- One-tap current-screen extraction through a Quick Settings tile.

AI settings live in `Settings > AI`:

- API URL
- API key
- Text processing model
- Multimodal model

The default endpoint is BigModel's chat completions API, and the default model is `glm-4-flash`. Other compatible chat-completions-style endpoints can be used if they support the same text and image message format.

Current-screen extraction requires Shizuku because Cresto captures the screen through Shizuku-powered shell access. Enable the feature in `Settings > General`, grant Shizuku permission, then add the `Extract Screen` tile from Android Quick Settings.

## Permissions

Cresto requests only the permissions needed by optional features:

- `POST_NOTIFICATIONS`: todo reminders and extraction progress/results.
- `SCHEDULE_EXACT_ALARM`: accurate reminder alarms.
- `RECEIVE_BOOT_COMPLETED`: restore scheduled reminders after reboot, app update, time, or timezone changes.
- `READ_CALENDAR` and `WRITE_CALENDAR`: manual and automatic sync to the system calendar.
- `INTERNET`: AI extraction requests.
- `moe.shizuku.manager.permission.API_V23`: Shizuku integration for screen extraction.

## Tech Stack

- Kotlin 2.3.x and Java 17
- Jetpack Compose
- Room for local todo storage
- Shizuku API for current-screen extraction
- `:glasense-ui`, a local UI/design-system module used by the app

## Project Structure

```text
.
├── app/                 # Android application module
│   └── src/main/java/com/nevoit/cresto/
│       ├── data/        # Room database, repository, reminders, calendar sync, backup
│       ├── feature/     # App screens and feature flows
│       ├── theme/       # App theme, colors, shapes, transitions
│       ├── toolkit/     # Visual helpers and effects
│       └── ui/          # Shared Compose components and modifiers
├── glasense-ui/         # Local Glasense UI library module
├── gradle/              # Version catalog and Gradle wrapper
└── test_script/         # Scratch/test scripts
```

## Backup Format

The Data & Storage screen exports todos and subtasks as JSON. Imports can either skip duplicate todos or import everything, which makes it safer to merge data from another installation without accidentally losing existing tasks.

## Glasense UI

`glasense-ui` is Cresto's in-repo UI library and design system. It contains reusable visual primitives, components, theme tokens, and interaction patterns used across the app.

I don't recommend you using `glasense-ui` at the time. It is still unfinished.

## Third-Party Notices

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for third-party license attributions.
