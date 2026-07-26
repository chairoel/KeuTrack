package com.mascill.keutrack.feature.settings.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.feature.settings.presentation.model.SignOutState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    familyRepository: FamilyRepository,
    private val createFamilyGroup: CreateFamilyGroupUseCase,
    private val joinFamilyGroup: JoinFamilyGroupUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val _signOutState = MutableStateFlow<SignOutState>(SignOutState.Idle)
    val signOutState: StateFlow<SignOutState> = _signOutState.asStateFlow()

    private val _membershipLoading = MutableStateFlow(false)
    val membershipLoading: StateFlow<Boolean> = _membershipLoading.asStateFlow()

    private val _membershipMessage = MutableStateFlow<String?>(null)
    val membershipMessage: StateFlow<String?> = _membershipMessage.asStateFlow()

    val currentUser: StateFlow<User?> =
        userRepository.getCurrentUser()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val currentFamily: StateFlow<FamilyGroup?> =
        familyRepository.observeCurrentFamily()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    init {
        viewModelScope.launch(dispatcher.io) {
            runCatching { userRepository.syncUserProfile() }
        }
    }

    fun createFamily(name: String) {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                createFamilyGroup(name)
                    .onSuccess {
                        _membershipMessage.value =
                            "Keluarga dibuat. Kode: ${it.inviteCode}"
                    }
                    .onFailure { e ->
                        _membershipMessage.value =
                            e.message ?: "Gagal membuat keluarga"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _membershipMessage.value = e.message ?: "Gagal membuat keluarga"
            } finally {
                _membershipLoading.value = false
            }
        }
    }

    fun joinFamily(inviteCode: String) {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                joinFamilyGroup(inviteCode)
                    .onSuccess {
                        _membershipMessage.value =
                            "Berhasil bergabung ke ${it.name}"
                    }
                    .onFailure { e ->
                        _membershipMessage.value =
                            e.message ?: "Gagal bergabung ke keluarga"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _membershipMessage.value = e.message ?: "Gagal bergabung ke keluarga"
            } finally {
                _membershipLoading.value = false
            }
        }
    }

    fun dismissMembershipMessage() {
        _membershipMessage.update { null }
    }

    fun signOut() {
        viewModelScope.launch(dispatcher.io) {
            _signOutState.value = SignOutState.Loading
            try {
                userRepository.signOut()
                _signOutState.value = SignOutState.Success
                Log.d("TAG", "signOut: success")
            } catch (e: Exception) {
                _signOutState.value = SignOutState.Error(e.message ?: "Sign out failed")
                Log.e("TAG", "signOut: failed", e)
            }
        }
    }
}
