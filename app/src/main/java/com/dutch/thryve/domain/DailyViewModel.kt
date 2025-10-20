package com.dutch.thryve.domain

import androidx.lifecycle.ViewModel
import com.dutch.thryve.data.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DailyViewModel @Inject constructor(private val repository: TrackerRepository) : ViewModel()
