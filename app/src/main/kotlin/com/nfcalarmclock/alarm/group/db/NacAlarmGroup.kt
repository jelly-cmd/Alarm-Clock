package com.nfcalarmclock.alarm.group.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * A named group of alarms that can be turned on and off together.
 */
@Entity(tableName = "alarm_group")
class NacAlarmGroup()
{

	/**
	 * Unique ID.
	 */
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	var id: Long = 0

	/**
	 * Name of the group.
	 */
	@ColumnInfo(name = "name", defaultValue = "")
	var name: String = ""

	/**
	 * Comma separated list of the IDs of the alarms in the group.
	 */
	@ColumnInfo(name = "alarm_ids", defaultValue = "")
	var alarmIds: String = ""

	/**
	 * Constructor.
	 */
	@Ignore
	constructor(name: String) : this()
	{
		this.name = name
	}

	/**
	 * IDs of the alarms in the group, as a set.
	 */
	fun alarmIdSet(): Set<Long>
	{
		return alarmIds.split(",")
			.mapNotNull { it.trim().toLongOrNull() }
			.toSet()
	}

	/**
	 * Check if an alarm is in the group.
	 */
	fun contains(alarmId: Long): Boolean = alarmIdSet().contains(alarmId)

	/**
	 * Set the IDs of the alarms in the group.
	 */
	fun replaceAlarmIds(ids: Collection<Long>)
	{
		alarmIds = ids.distinct().joinToString(",")
	}

	/**
	 * Add an alarm to the group.
	 */
	fun addAlarm(alarmId: Long)
	{
		replaceAlarmIds(alarmIdSet() + alarmId)
	}

	/**
	 * Remove an alarm from the group.
	 */
	fun removeAlarm(alarmId: Long)
	{
		replaceAlarmIds(alarmIdSet() - alarmId)
	}

}
