package net.matsudamper.gptclient.util

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Log {
    actual fun d(tag: String, message: String) {
        println("DEBUG: $tag: $message")
    }

    actual fun e(tag: String, message: String) {
        System.err.println("ERROR: $tag: $message")
    }
}
