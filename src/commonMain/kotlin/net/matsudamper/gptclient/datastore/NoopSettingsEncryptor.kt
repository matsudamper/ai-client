package net.matsudamper.gptclient.datastore

class NoopSettingsEncryptor : SettingsEncryptor {
    override fun encrypt(data: ByteArray): ByteArray = data
    override fun decrypt(data: ByteArray): ByteArray = data
}
