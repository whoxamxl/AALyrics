# Notification Access Onboarding

## Branch and baseline

- Branch: `feature/notification-access-onboarding`.
- Base: current `main` after the Phone shell work.
- Classification: **APPLICATION ENTRY / REQUIRED SYSTEM ACCESS**.
- Authoritative references: `AGENTS.md`, `docs/MEDIA_SESSION_RUNTIME.md`, and the existing NotificationListenerService runtime.
- The user explicitly authorized implementation.

## Goal

Require Notification Listener access before entering the normal phone application flow, because AALyrics depends on that access to observe other apps' active MediaSessions.

The application entry flow is:

```text
App launch / resume
        |
        v
Notification access granted?
   |                     |
   no                    yes
   |                     |
   v                     v
Required setup UI      normal app content
   |
   v
Android notification-listener settings
   |
   v
return to app -> re-check actual system state
```

## Acceptance criteria

- Treat Notification Listener access as required application setup.
- Do not request `POST_NOTIFICATIONS`; AALyrics does not currently need permission to post its own notifications.
- Check the real system grant state on launch and every Activity resume.
- On Android 11+ open the listener-specific detail settings page when available.
- Fall back to the general Notification Listener settings page, then general Settings if an OEM does not expose the more specific Activity.
- Android 8.0 (API 26) remains supported even though `NotificationManager.isNotificationListenerAccessGranted()` starts at API 27.
- The setup screen has no skip path and clearly explains why the access is required.
- Preserve the existing granted-state placeholder until the separate Phone application-composition slice wires the production shell.
- Keep MediaSession observation/provider/selection logic unchanged.
- Add deterministic JVM coverage for the entry gate state decision.
- Run CI/review, open a PR, and stop before merge for explicit approval.

## Planned work

- [x] Prepare branch and task scope.
- [x] Add framework access checker and settings navigation.
- [x] Add required setup screen and Activity resume gate.
- [x] Add deterministic gate tests.
- [x] Update durable runtime documentation.
- [x] Review diff / CI and open PR.

## Scope guard

This slice gates application entry on the access already required by the live MediaSession runtime. It does not implement finished Lyrics presentation, Android Auto UI, media controls, provider changes, or a general-purpose permission framework.
