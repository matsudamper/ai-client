# ai-client 固有ルール

## 概要
Kotlin Multiplatform の AI クライアント。Android / Desktop(JVM)。
バージョンは `build-logic/libs.versions.toml` 参照。

## 構成
- `app-android/` / `app-desktop/` / `ui/` / `room/`

## UI + ViewModel
- ComposeとViewModelはUiState経由
- UiStateはViewModelStateFlowからの情報で構成する

## ビルド
```shell
./gradlew assembleDebug
./gradlew :app-desktop:jvmJar
./gradlew ktlintCheck
./gradlew ktlintFormat
```

