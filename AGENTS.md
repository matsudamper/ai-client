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

- **MVVM パターン:** ViewModel + StateFlow による状態管理
- **DI:** Koin によるモジュール構成
- **ネットワーク:** Ktor Client (CIO エンジン)
- **データベース:** Room (KSP でコード生成)
- **永続化:** DataStore (設定)
- **シリアライズ:** kotlinx.serialization (JSON)
- **マルチプラットフォーム:** `commonMain` に共通ロジック、`androidMain`/`jvmMain` にプラットフォーム固有実装

## Pull Request

Pull Requestを作成する際、タイトルと説明文は日本語で記述してください。

## AI 応答ルール

**質問への対応:**
- ユーザーから質問を受けた場合、すぐに作業（コード変更、ファイル作成、リファクタリング等）を開始しない
- まず質問に対する回答や提案を提示し、ユーザーの明示的な承認または作業指示を待つこと
- 例: 「〜についてどう思う?」「〜を追加すべき?」等の質問には、意見や提案を述べるに留め、実装は指示があってから行う

**作業の実施:**
- 「〜を追加して」「〜を修正して」「〜を実装して」等の明確な指示がある場合のみ、作業を開始する
