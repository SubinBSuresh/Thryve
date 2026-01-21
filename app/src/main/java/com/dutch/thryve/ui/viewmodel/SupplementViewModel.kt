package com.dutch.thryve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutch.thryve.data.repository.FirebaseRepositoryImpl
import com.dutch.thryve.domain.model.Supplement
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplementUiState(
    val supplements: List<Supplement> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SupplementViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplementUiState())
    val uiState: StateFlow<SupplementUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        observeSupplements()
    }

    private fun observeSupplements() {
        val userId = auth.currentUser?.uid ?: return
        firebaseRepository.getSupplements(userId).onEach { supplements ->
            _uiState.update { it.copy(supplements = supplements, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun saveSupplement(supplement: Supplement) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firebaseRepository.saveSupplement(supplement, userId)
        }
    }

    fun deleteSupplement(supplementId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firebaseRepository.deleteSupplement(supplementId, userId)
        }
    }
    
    fun updateRemainingQuantity(supplement: Supplement, newQuantity: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firebaseRepository.saveSupplement(supplement.copy(remainingQuantity = newQuantity), userId)
        }
    }
}
