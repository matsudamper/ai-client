package net.matsudamper.gptclient.util

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
expect object Log {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
}
