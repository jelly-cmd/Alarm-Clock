package com.nfcalarmclock.alarm.group

import androidx.lifecycle.LiveData
import com.nfcalarmclock.alarm.group.db.NacAlarmGroup
import com.nfcalarmclock.alarm.group.db.NacAlarmGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Alarm group repository.
 */
class NacAlarmGroupRepository @Inject constructor(
	private val alarmGroupDao: NacAlarmGroupDao
)
{

	val allAlarmGroups: LiveData<List<NacAlarmGroup>>
		get() = alarmGroupDao.allAlarmGroupsLive

	suspend fun delete(group: NacAlarmGroup): Int = alarmGroupDao.delete(group)

	suspend fun getAllAlarmGroups(): List<NacAlarmGroup> = alarmGroupDao.getAllAlarmGroups()

	suspend fun insert(group: NacAlarmGroup): Long = alarmGroupDao.insert(group)

	suspend fun update(group: NacAlarmGroup): Int = alarmGroupDao.update(group)

}

/**
 * Hilt module to provide an instance of the alarm group repository.
 */
@InstallIn(SingletonComponent::class)
@Module
class NacAlarmGroupRepositoryModule
{

	@Provides
	fun provideAlarmGroupRepository(alarmGroupDao: NacAlarmGroupDao) : NacAlarmGroupRepository
	{
		return NacAlarmGroupRepository(alarmGroupDao)
	}

}
