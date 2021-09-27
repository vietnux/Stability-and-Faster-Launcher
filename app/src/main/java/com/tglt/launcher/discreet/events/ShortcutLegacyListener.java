package com.tglt.launcher.discreet.events ;

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
import android.content.BroadcastReceiver ;
import android.content.Context ;
import android.content.Intent ;
import android.content.IntentFilter ;
import android.graphics.Bitmap ;
import android.os.Build ;
import com.tglt.launcher.discreet.Constants ;
import com.tglt.launcher.discreet.R ;
import com.tglt.launcher.discreet.ShowDialog ;
import static com.tglt.launcher.discreet.ActivityMain.updateList ;

/**
 * Listen for legacy shortcut creation requests.
 */
public class ShortcutLegacyListener extends BroadcastReceiver
{
	/**
	 * Provide the filter to use when registering this receiver.
	 * @return An IntentFilter allowing to listen for legacy shortcut creation requests
	 */
	public IntentFilter getFilter()
	{
		return new IntentFilter("com.android.launcher.action.INSTALL_SHORTCUT") ;
	}


	/**
	 * Method called when a broadcast message is received.
	 * @param context Context of the message.
	 * @param intent Type and content of the message.
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		// Execute the following code only if the Android version is before Oreo
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
			{
				// Check if the intent as a valid action
				if(intent.getAction() == null) return ;

				// Check if a request to add a shortcut has been received
				if(intent.getAction().equals("com.android.launcher.action.INSTALL_SHORTCUT"))
					{
						// Retrive the name, icon and intent of the shortcut
						String display_name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ;
						Bitmap icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON) ;
						Intent shortcutIntent = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT) ;

						// If the request is invalid, display a message and quit
						if((display_name == null) || (shortcutIntent == null))
							{
								ShowDialog.toastLong(context, context.getString(R.string.error_shortcut_invalid_request)) ;
								return ;
							}

						// Add the shortcut and update the applications list
						String shortcut = display_name + Constants.SHORTCUT_SEPARATOR + shortcutIntent.toUri(0) ;
						ShortcutListener.addShortcut(context, display_name, shortcut, icon, true) ;
						updateList(context) ;
					}
			}
	}
}
