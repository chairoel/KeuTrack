package com.mascill.keutrack.core.datastore.serialization

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mascill.keutrack.core.datastore.SignedInUser
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class SignedInUserSerializer @Inject constructor() : Serializer<SignedInUser> {

    override val defaultValue: SignedInUser = SignedInUser.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SignedInUser =
        try {
            SignedInUser.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read SignedInUser proto.", e)
        }

    override suspend fun writeTo(t: SignedInUser, output: OutputStream) {
        t.writeTo(output)
    }
}
