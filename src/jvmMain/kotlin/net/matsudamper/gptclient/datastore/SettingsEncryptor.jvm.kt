package net.matsudamper.gptclient.datastore

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object SettingsEncryptor {
    actual fun encrypt(data: ByteArray): ByteArray = data
    actual fun decrypt(data: ByteArray): ByteArray = data
}
