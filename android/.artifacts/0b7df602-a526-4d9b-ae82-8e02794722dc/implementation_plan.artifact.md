# Fix missing AndroidManifest.xml

The build is failing because `app/src/main/AndroidManifest.xml` is missing. This file is required for every Android application module.

## User Review Required

> [!IMPORTANT]
> The project currently lacks a `res` directory and any `Activity`. While I can create a minimal `AndroidManifest.xml` to fix the build error, the app will not be launchable from the home screen without a `MainActivity`.
> I will create a basic `AndroidManifest.xml` and register the existing `SMSReceiver` and `TransactionNotificationListenerService`.

## Proposed Changes

### app module

#### [NEW] [AndroidManifest.xml](file:///Users/varun/.gemini/antigravity/scratch/upgrade-finance/android/app/src/main/AndroidManifest.xml)
Create a new manifest file with:
- Necessary permissions (`RECEIVE_SMS`, `READ_SMS`, `INTERNET`, `ACCESS_NETWORK_STATE`).
- Registration for `SMSReceiver`.
- Registration for `TransactionNotificationListenerService`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the build completes successfully.

### Manual Verification
- Verify that the `AndroidManifest.xml` file exists at the expected location.
