package com.dutch.thryve.domain.model



import com.google.firebase.firestore.FirebaseFirestore


data class ConnectionResult(
    val isReady: Boolean = false,
    val userId: String? = null,
    val db: FirebaseFirestore? = null,
    val error: String? = null
)