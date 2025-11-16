package com.dutch.thryve.data.repository

import com.dutch.thryve.data.dao.PRDao
import com.dutch.thryve.data.dao.TrackerDao
import com.dutch.thryve.domain.repository.PRRepository
import com.dutch.thryve.domain.repository.TrackerRepository
import com.dutch.thryve.ui.viewmodel.PRViewModel
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class PRRepositoryImpl @Inject constructor(private val prDao: PRDao) :
    PRRepository {
    override fun savePersonalRecord(pr: PRViewModel.PersonalRecord) {
        TODO("Not yet implemented")
    }
}