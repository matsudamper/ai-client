# プロジェクト概要
Kotlin Multiplatform(KMP)で構築されたAIクライアントアプリ。
Android,Desktop(JVM)に対応

バージョン等は`build-logic/libs.versions.toml`参照

## ビルドコマンド

```shell
# Android Debug APK ビルド（CI で実行される主要タスク）
./gradlew assembleDebug

# Desktop JVM JAR ビルド
./gradlew :app-desktop:jvmJar

# コードフォーマットチェック
./gradlew ktlintCheck

# コードフォーマット修正
./gradlew ktlintFormat
```


## プロジェクト構成

```
ai-client/
├── app-android/          # Android アプリケーションモジュール
├── app-desktop/          # Desktop (JVM) アプリケーションモジュール
├── ui/                   # 共有 Compose UI モジュール
├── room/                 # Room データベースモジュール
```

## Agent固有
### Claude Code Web
Claude Code WebではGradleの依存がダウンロードでエラーになります。
https://github.com/anthropics/claude-code/issues/13372

## アーキテクチャ
### UI + ViewModel
ComposeとViewModelのやり取りはUiStateを介して行う。
UiStateはViewModelStateFlowからのみの情報で構成されるべき。

## Pull Request

Pull Requestを作成する際、タイトルと説明文は日本語で記述してください。

## AI 応答ルール

**質問への対応:**
- ユーザーから質問と取れる応答があった場合、すぐに作業（コード変更、ファイル作成、リファクタリング等）を開始しない
  - まず質問に対する回答や提案を提示し、ユーザーの明示的な承認または作業指示を待つこと
