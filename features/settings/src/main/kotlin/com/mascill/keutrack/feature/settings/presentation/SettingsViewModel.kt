package com.mascill.keutrack.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.LeaveFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.settings.presentation.model.SettingsUIState
import com.mascill.keutrack.feature.settings.presentation.model.SettingsUiMapper
import com.mascill.keutrack.feature.settings.presentation.model.SignOutState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    familyRepository: FamilyRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    private val createFamilyGroup: CreateFamilyGroupUseCase,
    private val joinFamilyGroup: JoinFamilyGroupUseCase,
    private val leaveFamilyGroup: LeaveFamilyGroupUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val _signOutState = MutableStateFlow<SignOutState>(SignOutState.Idle)
    private val _membershipLoading = MutableStateFlow(false)
    private val _membershipMessage = MutableStateFlow<String?>(null)
    private val _infoMessage = MutableStateFlow<String?>(null)

    private val contentFlow =
        combine(
            userRepository.getCurrentUser(),
            familyRepository.observeCurrentFamily(),
            getWalletSummary(),
        ) { user: User?, family: FamilyGroup?, walletSummary: WalletSummary ->
            SettingsUiMapper.from(user, family, walletSummary)
        }.catch { e ->
            emit(
                SettingsUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }

    val uiState: StateFlow<SettingsUIState> =
        combine(
            contentFlow,
            _signOutState,
            _membershipLoading,
            _membershipMessage,
            _infoMessage,
        ) { content, signOutState, membershipLoading, membershipMessage, infoMessage ->
            content.copy(
                signOutState = signOutState,
                membershipLoading = membershipLoading,
                membershipMessage = membershipMessage,
                infoMessage = infoMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUIState(),
        )

    init {
        viewModelScope.launch(dispatcher.io) {
            runCatching { userRepository.syncUserProfile() }
        }
    }

    fun onSheetsComingSoon() {
        _infoMessage.value = MSG_SHEETS_COMING_SOON
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

    fun leaveFamily() {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                leaveFamilyGroup()
                    .onSuccess {
                        _membershipMessage.value = "Anda telah keluar dari keluarga"
                    }
                    .onFailure { e ->
                        _membershipMessage.value =
                            e.message ?: "Gagal keluar dari keluarga"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _membershipMessage.value = e.message ?: "Gagal keluar dari keluarga"
            } finally {
                _membershipLoading.value = false
            }
        }
    }

    fun dismissSnackbar() {
        _membershipMessage.update { null }
        _infoMessage.update { null }
    }

    fun signOut() {
        viewModelScope.launch(dispatcher.io) {
            _signOutState.value = SignOutState.Loading
            try {
                userRepository.signOut()
                _signOutState.value = SignOutState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _signOutState.value = SignOutState.Error(e.message ?: "Sign out failed")
            }
        }
    }

    private companion object {
        const val ERR_LOAD_FAILED = "Gagal memuat settings"
        const val MSG_SHEETS_COMING_SOON = "Segera hadir"
    }
}
