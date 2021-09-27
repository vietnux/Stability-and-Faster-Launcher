package com.tglt.launcher.discreet.storage ;

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
import android.content.Context ;
import java.io.File ;
import java.io.FilenameFilter ;
import static com.tglt.launcher.discreet.ActivityMain.getInternalFolder ;

/**
 * Manage the storage of an internal file.
 */
public class InternalFile
{
	// Attributes
	final File file ;


	/**
	 * Constructor to create or open an internal file.
	 * @param filename Name of the file including the extension
	 */
	InternalFile(String filename)
	{
		file = new File(getInternalFolder(), filename) ;
	}


	/**
	 * Check if the internal file exists on the system.
	 * @return <code>true</code> if it exists, <code>false</code> otherwise
	 */
	public boolean exists()
	{
		return file.exists() ;
	}


	/**
	 * Try to remove the internal file (considered as successful if not existing).
	 * @return <code>true</code> if successful, <code>false</code> otherwise
	 */
	public boolean remove()
	{
		if(!exists()) return true ;
		return file.delete() ;
	}


	/**
	 * Return the name of the internal file without the path.
	 * @return Name of the file
	 */
	public String getName()
	{
		return file.getName() ;
	}


	/**
	 * Rename the internal file.
	 * @param new_filename New name including the extension
	 * @return <code>true</code> if successful, <code>false</code> otherwise
	 */
	public boolean rename(String new_filename)
	{
		return file.renameTo(new File(getInternalFolder(), new_filename)) ;
	}


	/**
	 * Search internal files starting with a certain prefix.
	 * @param context To list files
	 * @param prefix Search filter
	 * @return List of filenames or <code>null</code> if none was found
	 */
	public static String[] searchFilesStartingWith(Context context, final String prefix)
	{
		FilenameFilter filter = new FilenameFilter()
		{
			@Override
			public boolean accept(File directory, String name)
			{
				return name.startsWith(prefix) ;
			}
		} ;
		return context.getFilesDir().list(filter) ;
	}
}
