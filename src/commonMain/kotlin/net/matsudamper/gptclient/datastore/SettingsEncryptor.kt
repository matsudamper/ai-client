package net.matsudamper.gptclient.datastore

interface SettingsEncryptor {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}
