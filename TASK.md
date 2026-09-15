# Gradle Wrapper Task

This branch fixes local/IDE Gradle reproducibility without changing the intentional Android/Kotlin toolchain versions.

## Scope

- [x] Create a dedicated `build/add-gradle-wrapper` branch from `main`.
- [x] Generate a fresh Gradle Wrapper for Gradle 9.6.0 using the official Gradle wrapper task.
- [x] Keep AGP 9.4.0, Kotlin 2.4.20, and compile/target SDK 36 unchanged.
- [x] Update CI to use `./gradlew` rather than a separately provisioned `gradle` executable.
- [ ] Remove the temporary wrapper-bootstrap workflow after generation.
- [ ] Verify the wrapper files and executable mode are committed correctly.
- [ ] Open a focused PR to `main`.
- [ ] Run CI and Codex review.
- [ ] Stop before merge for explicit approval.

## Notes

The wrapper is generated for AALyrics itself; it is not copied from the previous Auto Lyrics fork. The previous fork currently targets Gradle 8.4 and is not the source for these files.
