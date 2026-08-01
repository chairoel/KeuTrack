package com.mascill.keutrack.core.data.datasource

import androidx.datastore.core.DataStore
import com.mascill.keutrack.core.data.mapper.SignedInUserProtoMapper
import com.mascill.keutrack.core.datastore.SignedInUser
import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserProfileLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<SignedInUser>,
    private val mapper: SignedInUserProtoMapper,
) : UserProfileLocalDataSource {

    override fun observeSignedInUser(): Flow<User?> =
        dataStore.data.map { mapper.toDomainOrNull(it) }

    override suspend fun getSignedInUser(): User? =
        mapper.toDomainOrNull(dataStore.data.first())

    override suspend fun persist(user: User) {
        dataStore.updateData { mapper.toProto(user) }
    }

    override suspend fun clear() {
        dataStore.updateData { SignedInUser.getDefaultInstance() }
    }
}
