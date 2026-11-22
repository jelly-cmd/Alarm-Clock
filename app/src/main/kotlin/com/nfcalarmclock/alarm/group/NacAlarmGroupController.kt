package com.nfcalarmclock.alarm.group

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.textview.MaterialTextView
import com.nfcalarmclock.R
import com.nfcalarmclock.alarm.NacAlarmViewModel
import com.nfcalarmclock.alarm.db.NacAlarm
import com.nfcalarmclock.alarm.group.db.NacAlarmGroup
import com.nfcalarmclock.log.NacLog
import com.nfcalarmclock.shared.NacSharedPreferences
import com.nfcalarmclock.view.quickToast
import com.nfcalarmclock.view.setupSwitchColor
import kotlinx.coroutines.launch

/**
 * Show the alarm groups above the alarm list and handle everything that can be
 * done with them: create, rename, delete, choose alarms, and turn all alarms of a
 * group on or off together.
 *
 * An alarm is in at most one group at a time.
 */
class NacAlarmGroupController(

	/**
	 * Context.
	 */
	private val context: Context,

	/**
	 * Shared preferences.
	 */
	private val sharedPreferences: NacSharedPreferences,

	/**
	 * Scope to run database work in.
	 */
	private val scope: LifecycleCoroutineScope,

	/**
	 * Alarm view model.
	 */
	private val alarmViewModel: NacAlarmViewModel,

	/**
	 * Alarm group view model.
	 */
	private val alarmGroupViewModel: NacAlarmGroupViewModel,

	/**
	 * Container that holds one row per group.
	 */
	private val rowContainer: LinearLayout,

	/**
	 * Called for each alarm whose enabled state was changed by a group switch. This
	 * should save and reschedule the alarm.
	 */
	private val onAlarmChanged: (NacAlarm) -> Unit,

	/**
	 * Called once after one or more alarms were changed by a group switch.
	 */
	private val onAlarmsChanged: () -> Unit

)
{

	/**
	 * Most recent list of groups.
	 */
	private var groups: List<NacAlarmGroup> = emptyList()

	/**
	 * Most recent list of alarms.
	 */
	private var alarms: List<NacAlarm> = emptyList()

	/**
	 * IDs of the groups that are open and show their alarms.
	 */
	private val expandedGroupIds: MutableSet<Long> = mutableSetOf()

	/**
	 * Called when the list of groups changes.
	 */
	fun onGroupsChanged(newGroups: List<NacAlarmGroup>)
	{
		groups = newGroups
		refreshRows()
	}

	/**
	 * Called when the list of alarms changes.
	 */
	fun onAlarmsChanged(newAlarms: List<NacAlarm>)
	{
		alarms = newAlarms
		refreshRows()
	}

	/**
	 * Alarms that exist and are in a group.
	 */
	private fun alarmsInGroup(group: NacAlarmGroup): List<NacAlarm>
	{
		val ids = group.alarmIdSet()

		return alarms.filter { ids.contains(it.id) }
	}

	/**
	 * Get a short label for an alarm, such as "7:30 AM  Work".
	 */
	private fun alarmLabel(alarm: NacAlarm): String
	{
		val time = "${alarm.getClockTime(context)} ${alarm.getMeridian(context)}".trim()

		return if (alarm.name.isNotBlank()) "$time  ${alarm.name}" else time
	}

	/**
	 * Rebuild the group rows.
	 */
	private fun refreshRows()
	{
		val inflater = LayoutInflater.from(context)

		rowContainer.removeAllViews()

		for (group in groups)
		{
			// Inflate the row
			val row = inflater.inflate(R.layout.nac_alarm_group_row, rowContainer, false)
			val header: View = row.findViewById(R.id.alarm_group_header)
			val expandView: ImageView = row.findViewById(R.id.iv_alarm_group_expand)
			val nameView: MaterialTextView = row.findViewById(R.id.tv_alarm_group_name)
			val countView: MaterialTextView = row.findViewById(R.id.tv_alarm_group_count)
			val editView: ImageView = row.findViewById(R.id.iv_alarm_group_edit)
			val switch: SwitchCompat = row.findViewById(R.id.switch_alarm_group)
			val memberContainer: LinearLayout = row.findViewById(R.id.ll_alarm_group_members)
			val members = alarmsInGroup(group).sorted()
			val isExpanded = expandedGroupIds.contains(group.id)

			// Name and count
			nameView.text = group.name
			countView.text = context.resources.getQuantityString(
				R.plurals.alarm_group_alarm_count, members.size, members.size)

			// Switch is on when at least one alarm of the group is on
			switch.setupSwitchColor(sharedPreferences)
			switch.isChecked = members.any { it.isEnabled }
			switch.isEnabled = members.isNotEmpty()
			switch.setOnCheckedChangeListener { _, state ->
				setAlarmsEnabled(group.alarmIdSet(), state)
			}

			// Alarms of the group, like a dropdown
			expandView.rotation = if (isExpanded) 180f else 0f
			memberContainer.visibility = if (isExpanded && members.isNotEmpty()) View.VISIBLE else View.GONE

			if (isExpanded)
			{
				for (alarm in members)
				{
					memberContainer.addView(createMemberRow(inflater, memberContainer, alarm))
				}
			}

			// Tap the header to open or close the group
			header.setOnClickListener {
				if (!expandedGroupIds.remove(group.id))
				{
					expandedGroupIds.add(group.id)
				}

				refreshRows()
			}

			// Long press the header, or tap the pencil, to edit the group
			header.setOnLongClickListener {
				showGroupOptionsDialog(group)
				true
			}

			editView.setOnClickListener {
				showGroupOptionsDialog(group)
			}

			rowContainer.addView(row)
		}

		rowContainer.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
	}

	/**
	 * Create the row for one alarm inside an open group.
	 */
	private fun createMemberRow(inflater: LayoutInflater, parent: LinearLayout, alarm: NacAlarm): View
	{
		val row = inflater.inflate(R.layout.nac_alarm_group_member, parent, false)
		val labelView: MaterialTextView = row.findViewById(R.id.tv_alarm_group_member_label)
		val switch: SwitchCompat = row.findViewById(R.id.switch_alarm_group_member)

		labelView.text = alarmLabel(alarm)
		labelView.alpha = if (alarm.isEnabled) 1f else 0.5f

		switch.setupSwitchColor(sharedPreferences)
		switch.isChecked = alarm.isEnabled
		switch.setOnCheckedChangeListener { _, state ->
			setAlarmsEnabled(setOf(alarm.id), state)
		}

		return row
	}

	/**
	 * Turn alarms on or off, for example all alarms in a group.
	 */
	private fun setAlarmsEnabled(ids: Set<Long>, state: Boolean)
	{
		NacLog.i("Setting alarms $ids enabled=$state")

		scope.launch {

			// Get fresh copies of the alarms from the database so that the alarm list
			// sees them as changed and redraws the cards
			val groupAlarms = alarmViewModel.getAllAlarms().filter { ids.contains(it.id) }
			var skippedAny = false
			var changedAny = false

			for (alarm in groupAlarms)
			{
				// Same rule as the switch on the alarm card: active or snoozed alarms
				// cannot be changed
				if (alarm.isActive || (alarm.snoozeCount > 0))
				{
					skippedAny = skippedAny || (alarm.isEnabled != state)
					continue
				}

				// Nothing to change
				if (alarm.isEnabled == state)
				{
					continue
				}

				// Same changes that the switch on the alarm card makes
				alarm.shouldSkipNextAlarm = false
				alarm.snoozeCount = 0
				alarm.timeOfDismissEarlyAlarm = 0
				alarm.isEnabled = state

				onAlarmChanged(alarm)
				changedAny = true
			}

			if (changedAny)
			{
				onAlarmsChanged()
			}

			if (skippedAny)
			{
				quickToast(context, R.string.alarm_group_some_not_changed)
				refreshRows()
			}
		}
	}

	/**
	 * Show a dialog to enter a group name.
	 */
	private fun showNameDialog(title: Int, defaultName: String, onName: (String) -> Unit)
	{
		val editText = EditText(context)
		val padding = context.resources.getDimensionPixelSize(R.dimen.normal)
		val container = LinearLayout(context)

		editText.setText(defaultName)
		editText.setSelection(defaultName.length)
		editText.hint = context.getString(R.string.alarm_group_name_hint)
		editText.isSingleLine = true
		container.setPadding(padding, padding / 2, padding, 0)
		container.addView(editText, LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

		AlertDialog.Builder(context)
			.setTitle(title)
			.setView(container)
			.setPositiveButton(R.string.action_ok) { _, _ ->
				val name = editText.text.toString().trim()

				if (name.isEmpty())
				{
					quickToast(context, R.string.alarm_group_empty_name)
				}
				else
				{
					onName(name)
				}
			}
			.setNegativeButton(R.string.action_cancel, null)
			.show()
	}

	/**
	 * Show the dialog to create a new group. Optionally put an alarm in it.
	 */
	fun showNewGroupDialog(alarm: NacAlarm? = null)
	{
		showNameDialog(R.string.alarm_group_new, "") { name ->

			val group = NacAlarmGroup(name)

			if (alarm != null)
			{
				removeAlarmFromAllGroups(alarm.id)
				group.addAlarm(alarm.id)
			}

			alarmGroupViewModel.insert(group) { rowId ->

				// Let the user choose the alarms right away when the group was not
				// created from an alarm card
				if ((alarm == null) && (rowId > 0) && alarms.isNotEmpty())
				{
					showSelectAlarmsDialog(group)
				}
			}
		}
	}

	/**
	 * Show the options for a group.
	 */
	private fun showGroupOptionsDialog(group: NacAlarmGroup)
	{
		val options = arrayOf(
			context.getString(R.string.alarm_group_select_alarms),
			context.getString(R.string.alarm_group_rename),
			context.getString(R.string.alarm_group_delete))

		AlertDialog.Builder(context)
			.setTitle(group.name)
			.setItems(options) { _, which ->
				when (which)
				{
					0 -> showSelectAlarmsDialog(group)
					1 -> showRenameDialog(group)
					2 -> showDeleteDialog(group)
				}
			}
			.setNegativeButton(R.string.action_cancel, null)
			.show()
	}

	/**
	 * Show the dialog to rename a group.
	 */
	private fun showRenameDialog(group: NacAlarmGroup)
	{
		showNameDialog(R.string.alarm_group_rename, group.name) { name ->
			group.name = name
			alarmGroupViewModel.update(group)
		}
	}

	/**
	 * Show the dialog to delete a group.
	 */
	private fun showDeleteDialog(group: NacAlarmGroup)
	{
		AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.alarm_group_delete) + ": " + group.name)
			.setMessage(R.string.alarm_group_delete_message)
			.setPositiveButton(R.string.action_ok) { _, _ ->
				alarmGroupViewModel.delete(group)
			}
			.setNegativeButton(R.string.action_cancel, null)
			.show()
	}

	/**
	 * Show the dialog to choose which alarms are in a group.
	 */
	private fun showSelectAlarmsDialog(group: NacAlarmGroup)
	{
		// No alarms to choose from
		if (alarms.isEmpty())
		{
			quickToast(context, R.string.alarm_group_no_alarms)
			return
		}

		val sortedAlarms = alarms.sorted()
		val labels = sortedAlarms.map { alarmLabel(it) }.toTypedArray()
		val ids = group.alarmIdSet()
		val checked = sortedAlarms.map { ids.contains(it.id) }.toBooleanArray()

		AlertDialog.Builder(context)
			.setTitle(group.name)
			.setMultiChoiceItems(labels, checked) { _, which, isChecked ->
				checked[which] = isChecked
			}
			.setPositiveButton(R.string.action_ok) { _, _ ->

				val selectedIds = sortedAlarms.filterIndexed { i, _ -> checked[i] }
					.map { it.id }

				// An alarm can only be in one group, so take the selected alarms out of
				// every other group
				for (other in groups)
				{
					if ((other.id != group.id) && selectedIds.any { other.contains(it) })
					{
						other.replaceAlarmIds(other.alarmIdSet() - selectedIds.toSet())
						alarmGroupViewModel.update(other)
					}
				}

				// Keep IDs of alarms that no longer exist out of the group
				group.replaceAlarmIds(selectedIds)
				alarmGroupViewModel.update(group)
			}
			.setNegativeButton(R.string.action_cancel, null)
			.show()
	}

	/**
	 * Show the dialog to move an alarm to a group. Used from the alarm card menu.
	 */
	fun showMoveAlarmDialog(alarm: NacAlarm)
	{
		// None, each group, then new group. Use a snapshot of the groups in case they
		// change while the dialog is shown
		val groups = this.groups
		val labels = mutableListOf(context.getString(R.string.alarm_group_none))
		labels.addAll(groups.map { it.name })
		labels.add(context.getString(R.string.alarm_group_new) + "…")

		val currentIndex = groups.indexOfFirst { it.contains(alarm.id) } + 1

		AlertDialog.Builder(context)
			.setTitle(R.string.alarm_group_add_to_group)
			.setSingleChoiceItems(labels.toTypedArray(), currentIndex) { dialog, which ->

				dialog.dismiss()

				when (which)
				{
					// No group
					0 -> removeAlarmFromAllGroups(alarm.id)

					// New group
					labels.size - 1 -> showNewGroupDialog(alarm)

					// Existing group
					else ->
					{
						val group = groups[which - 1]

						if (!group.contains(alarm.id))
						{
							removeAlarmFromAllGroups(alarm.id)
							group.addAlarm(alarm.id)
							alarmGroupViewModel.update(group)
						}
					}
				}
			}
			.setNegativeButton(R.string.action_cancel, null)
			.show()
	}

	/**
	 * Remove an alarm from every group it is in.
	 */
	private fun removeAlarmFromAllGroups(alarmId: Long)
	{
		for (group in groups)
		{
			if (group.contains(alarmId))
			{
				group.removeAlarm(alarmId)
				alarmGroupViewModel.update(group)
			}
		}
	}

}
