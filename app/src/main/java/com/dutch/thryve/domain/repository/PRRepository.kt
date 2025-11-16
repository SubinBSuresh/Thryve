package com.dutch.thryve.domain.repository

import com.dutch.thryve.ui.viewmodel.PRViewModel

interface PRRepository {

    fun savePersonalRecord(pr: PRViewModel.PersonalRecord)
}