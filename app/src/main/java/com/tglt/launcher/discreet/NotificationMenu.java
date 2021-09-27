package com.tglt.launcher.discreet ;

// License
/*

	This file is part of Discreet Launcher.

	Copyright (C) 2019-2021 Vincent Falzon

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

// Imports
import android.app.NotificationChannel ;
import android.app.NotificationManager ;
import android.app.PendingIntent ;
import android.content.Context ;
import android.content.Intent ;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build ;
import androidx.core.app.NotificationCompat ;
import androidx.core.app.NotificationManagerCompat ;
import androidx.core.app.TaskStackBuilder;

import com.tglt.launcher.discreet.events.U;

/**
 * Display an Android notification with the favorites applications.
 */
class NotificationMenu
{
	// Attributes
	private final NotificationManagerCompat manager ;

	private boolean isHidden = true;


	/**
	 * Constructor to build the notification.
	 * @param context Provided by an activity
	 */
	NotificationMenu(Context context)
	{
		// Initializations
		manager = NotificationManagerCompat.from(context) ;

		// If the Android version is Oreo or higher, create the notification channel
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				NotificationChannel channel = new NotificationChannel("discreetlauncher", context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH) ;
				manager.createNotificationChannel(channel) ;
			}
	}


	/**
	 * Display the notification.
	 * @param context Provided by an activity
	 */
	void display(Context context) {
		// Define the notification settings
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "discreetlauncher") ;
		builder.setSmallIcon(R.drawable.notification_icon) ;
		builder.setLargeIcon( BitmapFactory.decodeResource(context.getResources(), R.drawable.logo) ) ;
		builder.setContentTitle(context.getString(R.string.info_favorites_access)) ;
		builder.setContentText(context.getString(R.string.info_order_favorites));
		builder.setShowWhen(false) ;      // Hide the notification timer
		builder.setOngoing(true) ;        // Sticky notification
		builder.setNotificationSilent() ; // No sound or vibration
		builder.setPriority(NotificationCompat.PRIORITY_DEFAULT) ;    // Default priority
		builder.setVisibility(NotificationCompat.VISIBILITY_SECRET) ; // Hidden on lock screen
//		builder.setAutoCancel(true);

		// Prepare the intent to display the favorites popup
		Intent intent = new Intent(Intent.ACTION_MAIN) ;
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP) ;
		intent.setClassName(context.getPackageName(), context.getPackageName() + ".events.NotificationListener") ;

		// Define the notification action
		PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, intent, 0) ;
		builder.setContentIntent(pendingIntent) ;

//		String showHideLabel;
//		SharedPreferences pref = U.getSharedPreferences(context);
//		Intent receiverIntent = new Intent(Constants.ACTION_SHOW_HIDE_LAUNCHER);
//		receiverIntent.setPackage(context.getPackageName());

//		if(U.canEnableFreeform() && !U.isChromeOs(context) ) {
//			String freeformLabel = context.getString( !U.isFreeformModeEnabled(context) ? R.string.tb_freeform_off : R.string.tb_freeform_on);
//
//			Intent freeformIntent = new Intent(Constants.ACTION_TOGGLE_FREEFORM_MODE);
//			freeformIntent.setPackage(context.getPackageName());
//
//			PendingIntent freeformPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, freeformIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//			builder.addAction(0, freeformLabel, freeformPendingIntent);
//		}

		Intent intentHome = new Intent( context, ActivityMain.class );
		// Create the TaskStackBuilder and add the intent, which inflates the back stack
		TaskStackBuilder stackBuilderHome = TaskStackBuilder.create(context);
		stackBuilderHome.addNextIntentWithParentStack(intentHome);
		// Get the PendingIntent containing the entire back stack
		PendingIntent resultPendingIntentHome = stackBuilderHome.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(0, context.getString(R.string.button_home), resultPendingIntentHome);

		Intent receiverIntent2 = new Intent( context, ActivityFavorites.class );
		// Create the TaskStackBuilder and add the intent, which inflates the back stack
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addNextIntentWithParentStack(receiverIntent2);
		// Get the PendingIntent containing the entire back stack
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(0, context.getString(R.string.button_manage_favorites), resultPendingIntent);

		Intent intentSetting = new Intent( context, ActivitySettings.class );
		// Create the TaskStackBuilder and add the intent, which inflates the back stack
		TaskStackBuilder stackBuilderSetting = TaskStackBuilder.create(context);
		stackBuilderSetting.addNextIntentWithParentStack(intentSetting);
		// Get the PendingIntent containing the entire back stack
		PendingIntent resultPendingIntentSetting = stackBuilderSetting.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(0, context.getString(R.string.button_settings), resultPendingIntentSetting);

		// Display the notification
		manager.notify(1, builder.build()) ;
	}


	/**
	 * Hide the notification.
	 */
	void hide() {
		manager.cancel(1) ;
	}
}
