package net.matsudamper.gptclient.util

private const val MAX_CAUSE_DEPTH = 5

/**
 * 画面に出たメッセージだけで原因を追えるように、例外の型名と cause の連鎖まで含めた文字列にする。
 * message が null の例外だと「エラーが発生しました」しか残らず、何が起きたか調べられないため。
 */
fun Throwable.toDetailMessage(): String {
    return generateSequence(this) { throwable -> throwable.cause?.takeIf { it !== throwable } }
        .take(MAX_CAUSE_DEPTH)
        .mapIndexed { index, throwable ->
            val prefix = if (index == 0) "" else "Caused by: "
            val name = throwable::class.simpleName ?: "Throwable"
            when (val message = throwable.message?.takeIf { it.isNotBlank() }) {
                null -> "$prefix$name"
                else -> "$prefix$name: $message"
            }
        }
        .joinToString("\n")
}
