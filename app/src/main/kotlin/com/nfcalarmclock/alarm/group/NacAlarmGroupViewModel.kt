package com.nfcalarmclock.alarm.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcalarmclock.alarm.group.db.NacAlarmGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alarm group view model.
 */
@HiltViewModel
class NacAlarmGroupViewModel @Inject constructor(
	private val alarmGroupRepository: NacAlarmGroupRepository
) : ViewModel()
{

	/**
	 * Live data list of all alarm groups.
	 */
	val allAlarmGroups: LiveData<List<NacAlarmGroup>> = alarmGroupRepository.allAlarmGroups

	/**
	 * Delete an alarm group.
	 */
	fun delete(group: NacAlarmGroup)
	{
		viewModelScope.launch {
			alarmGroupRepository.delete(group)
		}
	}

	/**
	 * Get all alarm groups.
	 */
	suspend fun getAllAlarmGroups(): List<NacAlarmGroup> = alarmGroupRepository.getAllAlarmGroups()

	/**
	 * Insert an alarm group.
	 */
	fun insert(group: NacAlarmGroup, onInserted: (Long) -> Unit = {})
	{
		viewModelScope.launch {
			val rowId = alarmGroupRepository.insert(group)

			if (rowId > 0)
			{
				group.id = rowId
			}

			onInserted(rowId)
		}
	}

	/**
	 * Update an alarm group.
	 */
	fun update(group: NacAlarmGroup)
	{
		viewModelScope.launch {
			alarmGroupRepository.update(group)
		}
	}

}
