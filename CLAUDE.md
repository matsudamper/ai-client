# CLAUDE.md

## プロジェクト概要

Kotlin Multiplatform (KMP) で構築された ChatGPT クライアントアプリケーション。Android と Desktop (JVM) の両プラットフォームに対応。

- **パッケージ名:** `net.matsudamper.gptclient`
- **Kotlin / Compose バージョン:** `gradle.properties` を参照
- **ライブラリバージョン:** `build-logic/libs.versions.toml` を参照
- **Java:** JDK 21

## プロジェクト構成

```
ai-client/
├── src/
│   ├── commonMain/       # 共通コード（ViewModel, UseCase, API クライアント等）
│   ├── androidMain/      # Android 固有コード
│   ├── jvmMain/          # Desktop (JVM) 固有コード
│   └── androidInstrumentedTest/  # Android テスト
├── ui/                   # 共有 Compose UI モジュール
├── room/                 # Room データベースモジュール
├── build-logic/          # ビルド設定・バージョンカタログ (libs.versions.toml)
├── .github/workflows/    # CI/CD
└── gradle/               # Gradle Wrapper
```

### 主要パッケージ (commonMain)

| パッケージ | 役割 |
|---|---|
| `gpt/` | ChatGPT API クライアント・リクエスト/レスポンスモデル |
| `viewmodel/` | MVVM ViewModel 層 |
| `usecase/` | ビジネスロジック（レスポンスパーサー、チャット削除等） |
| `entity/` | データモデル |
| `datastore/` | 設定の永続化 (DataStore) |
| `navigation/` | 画面遷移 |
| `serialization/` | JSON シリアライザ |

### UI モジュール (`ui/`)

共有 Compose UI コンポーネント（NewChat, ChatList, SettingsScreen, ProjectScreen 等）

### Room モジュール (`room/`)

SQLite データベース。エンティティ: `Chat`, `ChatRoom`, `Project`

## ビルドコマンド

```shell
# Android Debug APK ビルド（CI で実行される主要タスク）
./gradlew assembleDebug

# Desktop JVM JAR ビルド
./gradlew jvmJar

# Android Release APK ビルド
./gradlew assembleRelease

# コードフォーマットチェック (ktlint)
./gradlew ktlintCheck

# コードフォーマット修正
./gradlew ktlintFormat
```

**CI では `assembleDebug` と `jvmJar` が実行される。** 変更後はこの2つが通ることを確認すること。

## コードスタイル・規約

- **フォーマッター:** ktlint（全モジュールに適用）
- 詳細な設定は `.editorconfig` を参照

## アーキテクチャ

- **MVVM パターン:** ViewModel + StateFlow による状態管理
- **DI:** Koin によるモジュール構成
- **ネットワーク:** Ktor Client (CIO エンジン)
- **データベース:** Room (KSP でコード生成)
- **永続化:** DataStore (設定)
- **シリアライズ:** kotlinx.serialization (JSON)
- **マルチプラットフォーム:** `commonMain` に共通ロジック、`androidMain`/`jvmMain` にプラットフォーム固有実装

## エントリーポイント

- **Android:** `src/androidMain/.../MainActivity.kt`
- **Desktop:** `src/jvmMain/.../Main.kt`
- **共通:** `src/commonMain/.../App.kt`

## Android 設定

- **minSdk:** 34 / **targetSdk:** 36 / **compileSdk:** 36
- **必要なパーミッション:** INTERNET, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC

## CI/CD

- **GitHub Actions** (`build.yaml`): `main` ブランチへの push と PR で `assembleDebug` + `jvmJar` を実行
- **リリース** (`release.yml`): `v*` タグで Release APK をビルドし GitHub Release に公開
- **依存関係管理:** Renovate Bot（最大 20 件の同時 PR）
