package net.matsudamper.gptclient

import androidx.compose.runtime.Composable

/**
 * システムバーのアイコン色をアプリのテーマに追従させる。
 *
 * アプリ内のテーマ設定はシステムのダークモード設定と一致しないことがあり、
 * プラットフォーム任せにするとアイコンが背景と同系色になって見えなくなる。
 */
@Composable
expect fun SyncSystemBarsAppearance(isDark: Boolean)
