package com.nfcalarmclock.system.permission.fullscreenintent

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.nfcalarmclock.shared.NacSharedPreferences

/**
 * Helper functions for the USE_FULL_SCREEN_INTENT permission.
 *
 * Starting with Android 14 the user can turn this permission off, and some phones
 * turn it off by default. Without it, a ringing alarm is not shown on the lock
 * screen.
 */
object NacFullScreenIntentPermission
{

	/**
	 * Check if the correct Android version is being used.
	 */
	val isCorrectAndroidVersion: Boolean
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

	/**
	 * The name of the USE_FULL_SCREEN_INTENT permission.
	 */
	val permissionName: String
		get() = Manifest.permission.USE_FULL_SCREEN_INTENT

	/**
	 * Check if the app has the USE_FULL_SCREEN_INTENT permission.
	 */
	fun hasPermission(context: Context): Boolean
	{
		// Granted automatically on older versions of Android
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
		{
			return true
		}

		val notificationManager = context.getSystemService(NotificationManager::class.java)

		return notificationManager?.canUseFullScreenIntent() ?: true
	}

	/**
	 * Open the system setting where the user can allow full screen notifications.
	 */
	fun requestPermission(activity: Activity?)
	{
		if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) || (activity == null))
		{
			return
		}

		val uri = ("package:"+activity.packageName).toUri()
		val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, uri)

		try
		{
			activity.startActivity(intent)
		}
		catch (_: ActivityNotFoundException)
		{
			// Some phones do not have this screen. Open the app's notification
			// settings instead
			val fallback = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
				.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)

			activity.startActivity(fallback)
		}
	}

	/**
	 * Check whether the app should request the USE_FULL_SCREEN_INTENT permission.
	 */
	fun shouldRequestPermission(
		context: Context,
		shared: NacSharedPreferences
	): Boolean
	{
		return isCorrectAndroidVersion
			&& !hasPermission(context)
			&& !shared.wasFullScreenIntentPermissionRequested
	}

}
