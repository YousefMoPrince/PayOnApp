package com.jo.payonapp.storage

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class LocalImageStorage(
    private val baseDirectoryPath: String,
    private val secretKeyBytes: ByteArray? = null
) {

    private val algorithm = "AES/GCM/NoPadding"
    private val tagLengthBit = 128
    private val ivLengthByte = 12

    private val secretKey: SecretKey by lazy {
        if (secretKeyBytes != null) {
            SecretKeySpec(secretKeyBytes, "AES")
        } else {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey()
        }
    }

    fun saveEncryptedImage(fileName: String, bytes: ByteArray): String {
        val directory = File(baseDirectoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)
        if (file.exists()) file.delete()

        val iv = ByteArray(ivLengthByte).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(tagLengthBit, iv))
        val encryptedBytes = cipher.doFinal(bytes)

        file.outputStream().use { output ->
            output.write(iv)
            output.write(encryptedBytes)
        }

        return file.absolutePath
    }

    fun getDecryptedImage(fileName: String): ByteArray {
        val file = File(baseDirectoryPath, fileName)
        if (!file.exists()) throw IllegalArgumentException("الملف غير موجود: ${file.absolutePath}")

        val allBytes = file.inputStream().use { it.readBytes() }
        if (allBytes.size <= ivLengthByte) {
            throw IllegalStateException("تنسيق الملف المشفر غير صالح")
        }

        val iv = allBytes.copyOfRange(0, ivLengthByte)
        val encryptedBytes = allBytes.copyOfRange(ivLengthByte, allBytes.size)

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLengthBit, iv))

        return cipher.doFinal(encryptedBytes)
    }
}