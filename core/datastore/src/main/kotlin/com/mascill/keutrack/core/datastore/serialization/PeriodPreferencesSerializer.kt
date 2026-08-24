package com.mascill.keutrack.core.datastore.serialization

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mascill.keutrack.core.datastore.PeriodPreferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class PeriodPreferencesSerializer @Inject constructor() : Serializer<PeriodPreferences> {

    override val defaultValue: PeriodPreferences = PeriodPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PeriodPreferences =
        try {
            PeriodPreferences.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read PeriodPreferences proto.", e)
        }

    override suspend fun writeTo(t: PeriodPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}
