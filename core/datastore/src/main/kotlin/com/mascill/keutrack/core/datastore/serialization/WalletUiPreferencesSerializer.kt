package com.mascill.keutrack.core.datastore.serialization

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mascill.keutrack.core.datastore.WalletUiPreferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class WalletUiPreferencesSerializer @Inject constructor() : Serializer<WalletUiPreferences> {

    override val defaultValue: WalletUiPreferences = WalletUiPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): WalletUiPreferences =
        try {
            WalletUiPreferences.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read WalletUiPreferences proto.", e)
        }

    override suspend fun writeTo(t: WalletUiPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}
