# AALyrics Privacy Policy

**Effective date:** September 22, 2026

AALyrics is an Android application for displaying synchronized lyrics for music playing on your device. This policy describes the data AALyrics accesses, stores, and sends when you use the app.

## Summary

AALyrics does not include advertising, analytics, or telemetry SDKs, and it does not require an AALyrics account.

AALyrics does access information about the media currently playing on your device so it can identify the track and find lyrics. Lyrics lookup requires network requests to third-party lyrics services.

## Media and playback information

AALyrics requires Android Notification Access so Android permits it to observe active media sessions.

From the selected media session, AALyrics may process information such as:

- track title;
- artist;
- album;
- track duration and playback position;
- playback state;
- artwork;
- the package/application that is providing playback;
- track identifiers exposed by the media session, such as a Spotify track identifier when available.

This information is used to identify the currently playing track, display playback information, control playback when supported by the selected media session, and find matching lyrics.

AALyrics does not use Notification Access to build a notification history, and the application does not intentionally upload notification message contents.

## Lyrics providers

To find lyrics, AALyrics may contact one or more third-party lyrics providers, currently:

- LRCLIB;
- PetitLyrics;
- Musixmatch;
- SyncLRC.

Depending on the provider and the metadata available for a track, a request may include the track title, artist, album, duration, and a supported service identifier such as a Spotify track identifier.

These services also receive ordinary network information needed to serve the request, such as your IP address and request headers. Their handling of data is governed by their own services and policies.

AALyrics receives lyrics and associated match metadata from those providers and uses them to select and display the best available result.

## Translation

Translation and language identification are implemented with Google ML Kit and run on the device after the required models are available.

When you request an additional translation model, ML Kit may contact Google services to download and manage that model. AALyrics does not intentionally send the lyrics text to Google for server-side translation.

Downloaded translation models are managed on the device and can be removed from **Settings > Advanced > Clear translation models**.

## Data stored on the device

AALyrics stores a small amount of app-owned configuration locally, including settings such as:

- whether Translation is enabled;
- the selected Translation target language;
- whether Verbose details is enabled;
- the Android Auto compatibility acknowledgement.

AALyrics does not currently maintain a persistent history of the songs you play or a persistent lyrics-history database.

Current playback, lyrics, and related presentation state are primarily held in memory while the app is running.

Android may include eligible app-local data in its standard device backup/restore mechanisms according to your Android and Google backup settings.

## Permissions

AALyrics currently requests Internet access for lyrics-provider communication and related network operations.

Notification Access is granted separately through Android system settings and is required for AALyrics to observe active media sessions. You can revoke it at any time in Android Settings.

## External links and support

Settings can open external pages such as the AALyrics GitHub repository.

If you choose **Support AALyrics**, the app opens the configured Buy Me a Coffee page in a browser surface. AALyrics does not embed the checkout, receive your payment credentials, process the transaction, or unlock app features based on a contribution.

Once you open an external site, that site's own terms and privacy practices apply.

## Data sharing and sale

AALyrics does not sell personal data.

AALyrics does not send data to advertising or analytics services. Track metadata is shared with lyrics providers only as needed to perform lyrics lookup, as described above.

## Retention and deletion

App settings remain on the device until they are changed, reset, cleared by Android, or the app is uninstalled, subject to Android backup/restore behavior.

Downloaded Translation models remain managed by ML Kit until they are removed through AALyrics, removed by the system/provider, or the app/device data is cleared.

AALyrics does not operate its own server-side user account or analytics database from which user data must be separately deleted.

## Changes to this policy

The Privacy Policy shipped inside AALyrics is bundled from this repository's `PRIVACY.md` at build time. A future release may update this policy when application behavior, external services, or legal requirements change.

## Contact

AALyrics is maintained by Yuta Miura (`whoxamxl`).

For privacy-related questions, use the AALyrics GitHub repository. Avoid posting sensitive personal information in a public issue or discussion.

<https://github.com/whoxamxl/AALyrics>
