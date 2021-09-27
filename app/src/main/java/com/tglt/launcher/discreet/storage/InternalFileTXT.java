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
import com.tglt.launcher.discreet.Constants ;
import java.io.BufferedReader ;
import java.io.FileReader ;
import java.io.FileWriter ;
import java.util.ArrayList ;

/**
 * Manage the storage of an internal TXT file.
 */
public class InternalFileTXT extends InternalFile
{
	/**
	 * Constructor to create or open an internal TXT file.
	 * @param filename Name of the file including the extension
	 */
	public InternalFileTXT(String filename)
	{
		super(filename) ;
	}


	/**
	 * Read the internal file line by line and return the result in an array of lines.
	 * @return Content of the file or <code>null</code> if an error happened
	 */
	public ArrayList<String> readAllLines()
	{
		// Check if the file exists
		if(!exists()) return null ;

		// Prepare the table used to store the lines
		ArrayList<String> content = new ArrayList<>() ;
		String buffer ;

		try
		{
			// Read the content from the file line by line
			BufferedReader reader = new BufferedReader(new FileReader(file)) ;
			while((buffer = reader.readLine()) != null) content.add(buffer) ;
			reader.close() ;
		}
		catch (Exception e)
		{
			// An error happened while reading the file
			return null ;
		}

		// Return the content of the file
		return content ;
	}


	/**
	 * Check if a specific line exists in the file.
	 * @param search Text of the line
	 * @return <code>true</code> if it exists, <code>false</code> otherwis
	 */
	public boolean isLineExisting(String search)
	{
		return readAllLines().contains(search) ;
	}


	/**
	 * Write a new line at the end of the file (create it if not existing yet).
	 * After the writing, a new line character is added.
	 * @param added_line To write in the file
	 * @return <code>true</code> if successful, <code>false</code> otherwise
	 */
	public boolean writeLine(String added_line)
	{
		try
		{
			// Write the line at the end of the file followed by a new line character
			FileWriter writer = new FileWriter(file, true) ;
			writer.write(added_line) ;
			writer.write(System.lineSeparator()) ;
			writer.close() ;
			return true ;
		}
		catch (Exception e)
		{
			// An error happened while writing the line
			return false ;
		}
	}


	/**
	 * Search a line in a file and remove it if it exists.
	 * @param to_remove Line to search and remove
	 * @return <code>true</code> if found and removed, <code>false</code> otherwise
	 */
	public boolean removeLine(String to_remove)
	{
		// Check if the line exists in the file
		if(!isLineExisting(to_remove)) return false ;

		// Keep a copy of the file content and remove it
		ArrayList<String> content = readAllLines() ;
		if(!remove()) return false ;

		// Write back the content of the file except the line to remove
		boolean result = false ;
		for(String line : content)
		{
			if(line.equals(to_remove)) result = true ;
				else writeLine(line) ;
		}
		return result ;
	}


	/**
	 * Prepare the internal file for inclusion in an export file.
	 * @return An array of lines where each line starts with the filename
	 */
	public ArrayList<String> prepareForExport()
	{
		// Return the content of the file or indicate that it does not exist
		ArrayList<String> content = new ArrayList<>() ;
		if(!exists()) content.add(file.getName() + ": " + Constants.NONE) ;
			else for(String line : readAllLines()) content.add(file.getName() + ": " + line) ;
		return content ;
	}
}
