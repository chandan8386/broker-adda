# Trishakti CRM — Mobile (Flutter)

Android + iOS client for the same REST API used by the web app.

## Prerequisites
- Flutter 3.24+ (`flutter --version`)
- Android Studio / Xcode for device emulators

## First-time setup
This folder contains the Dart source (`lib/`), `pubspec.yaml`, tests and lint config.
Generate the native platform folders once:

```bash
cd mobile
flutter create --org com.trishakti --project-name trishakti_crm .
flutter pub get
```

`flutter create .` adds `android/`, `ios/`, `web/`, etc. **without** touching the existing
`lib/`, `pubspec.yaml` or `test/`.

## Run

```bash
# Android emulator (host loopback is 10.0.2.2)
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api

# iOS simulator / physical device on same LAN
flutter run --dart-define=API_BASE_URL=http://<your-machine-ip>:8080/api
```

## Structure

```
lib/
├── main.dart
├── core/          config, Dio client (+JWT refresh), secure token store, theme, router, providers
└── features/
    ├── auth/       repository, controller (Riverpod StateNotifier), login screen
    ├── shell/      bottom-nav scaffold
    ├── dashboard/  KPI grid from /dashboard/summary
    ├── leads/      paginated list + search, lead detail, log-call & status-change sheets
    └── followups/  "my pending follow-ups" with one-tap complete
```

Auth tokens are stored with `flutter_secure_storage`; a 401 triggers a single transparent
refresh via `/auth/refresh`, mirroring the web client.

## Test
```bash
flutter test
```
