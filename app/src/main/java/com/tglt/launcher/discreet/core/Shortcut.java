package com.tglt.launcher.discreet.core ;

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
import android.content.ActivityNotFoundException ;
import android.content.Context ;
import android.content.Intent ;
import android.content.pm.LauncherApps ;
import android.graphics.drawable.Drawable ;
import android.os.Build ;
import android.os.UserHandle ;
import android.view.View ;
import com.tglt.launcher.discreet.Constants ;
import com.tglt.launcher.discreet.R ;
import com.tglt.launcher.discreet.ShowDialog ;
import java.net.URISyntaxException ;

/**
 * Represent a shortcut with its name and icon.
 */
public class Shortcut extends Application
{
	/**
	 * Constructor to represent a shortcut
	 * @param display_name Displayed to the user
	 * @param name Application name used internally
	 * @param apk Package name used internally
	 * @param icon Displayed to the user
	 */
	public Shortcut(String display_name, String name, String apk, Drawable icon)
	{
		super(display_name, name, apk, icon) ;
	}


	/**
	 * Start the shortcut as a new task.
	 * @param view Element from which the event originates
	 * @return <code>true</code> if the shortcut was launched, <code>false</code> otherwise
	 */
	public boolean start(View view)
	{
		// Check the type of shortcut
		Context context = view.getContext() ;
		Intent intent ;

		// If this is a shortcut before Oreo, launch it directly
		if(apk.equals(Constants.APK_SHORTCUT_LEGACY))
			{
				try { intent = Intent.parseUri(name, 0) ; }
				catch(URISyntaxException e) { return false ; }
				context.startActivity(intent) ;
				return true ;
			}

		// If this is a shortcut with Oreo or higher, extract the shortcut details
		String[] shortcut = name.split(Constants.SHORTCUT_SEPARATOR) ;
		if(shortcut.length != 3)
			{
				ShowDialog.toastLong(context, context.getString(R.string.error_shortcut_missing_info)) ;
				return false ;
			}

		// Try to retrieve the user ID, use 0 if not found (0 is "System", the most commonly used)
		int user_id ;
		try { user_id = Integer.parseInt(shortcut[2]) ; }
		catch(NumberFormatException e) { user_id = 0 ; }

		// Check if the system can manage these shortcuts
		LauncherApps launcher = (LauncherApps)context.getSystemService(Context.LAUNCHER_APPS_SERVICE) ;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
			{
				// Check if the launcher can start shortcuts
				if(!launcher.hasShortcutHostPermission())
					{
						ShowDialog.toastLong(context, context.getString(R.string.error_shortcut_not_default_launcher)) ;
						return false ;
					}

				try
				{
					// Try to launch the shortcut
					launcher.startShortcut(shortcut[0], shortcut[1], null, null, UserHandle.getUserHandleForUid(user_id)) ;
				}
				catch(ActivityNotFoundException | IllegalStateException e)
				{
					ShowDialog.toastLong(context, context.getString(R.string.error_shortcut_start)) ;
					return false ;
				}
				return true ;
			}
			else
			{
				ShowDialog.toastLong(context, context.getString(R.string.error_shortcut_start)) ;
				return false ;
			}
	}
}
