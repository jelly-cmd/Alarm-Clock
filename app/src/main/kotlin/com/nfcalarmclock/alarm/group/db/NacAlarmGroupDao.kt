package com.nfcalarmclock.alarm.group.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Data access object for alarm groups.
 */
@Dao
interface NacAlarmGroupDao
{

	/**
	 * Get all alarm groups.
	 */
	@get:Query("SELECT * FROM alarm_group ORDER BY name COLLATE NOCASE ASC, id ASC")
	val allAlarmGroupsLive: LiveData<List<NacAlarmGroup>>

	/**
	 * Delete an alarm group.
	 */
	@Delete
	suspend fun delete(group: NacAlarmGroup): Int

	/**
	 * Get all alarm groups.
	 */
	@Query("SELECT * FROM alarm_group ORDER BY name COLLATE NOCASE ASC, id ASC")
	suspend fun getAllAlarmGroups(): List<NacAlarmGroup>

	/**
	 * Insert an alarm group.
	 */
	@Insert
	suspend fun insert(group: NacAlarmGroup): Long

	/**
	 * Update an alarm group.
	 */
	@Update
	suspend fun update(group: NacAlarmGroup): Int

}
