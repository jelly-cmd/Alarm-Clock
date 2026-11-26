package com.nfcalarmclock.system.permission.fullscreenintent

import com.nfcalarmclock.R
import com.nfcalarmclock.system.permission.NacPermissionRequestDialog

/**
 * Dialog to request the USE_FULL_SCREEN_INTENT permission.
 */
class NacFullScreenIntentPermissionRequestDialog
	: NacPermissionRequestDialog()
{

	/**
	 * The name of the permission.
	 */
	override val permission: String = NacFullScreenIntentPermission.permissionName

	/**
	 * The ID of the icon.
	 */
	override val iconId: Int = R.drawable.desktop_windows_32

	/**
	 * The ID of the title string.
	 */
	override val titleId: Int = R.string.title_permission_full_screen_intent

	/**
	 * The ID of the text string.
	 */
	override val descriptionId: Int = R.string.message_permission_full_screen_intent_request

	/**
	 * The actions to execute when the permission request is accepted.
	 */
	override fun doPermissionRequestAccepted()
	{
		sharedPreferences.wasFullScreenIntentPermissionRequested = true
		super.doPermissionRequestAccepted()
	}

	/**
	 * The actions to execute when the permission request is canceled.
	 */
	override fun doPermissionRequestCanceled()
	{
		sharedPreferences.wasFullScreenIntentPermissionRequested = true
		super.doPermissionRequestCanceled()
	}

	companion object
	{

		/**
		 * Tag for the class.
		 */
		const val TAG = "NacFullScreenIntentPermissionDialog"

	}

}
