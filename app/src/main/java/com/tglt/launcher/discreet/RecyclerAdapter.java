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
import android.content.Context ;
import android.content.DialogInterface ;
import android.content.Intent ;
import androidx.annotation.NonNull ;
import androidx.appcompat.app.AlertDialog ;
import androidx.preference.PreferenceManager ;
import androidx.recyclerview.widget.RecyclerView ;
import android.content.SharedPreferences ;
import android.graphics.PorterDuff ;
import android.graphics.Typeface ;
import android.net.Uri ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.TextView ;
import com.tglt.launcher.discreet.core.Application ;
import com.tglt.launcher.discreet.core.Folder ;
import com.tglt.launcher.discreet.core.Shortcut ;
import com.tglt.launcher.discreet.events.ShortcutListener ;
import java.util.ArrayList ;

/**
 * Fill a RecyclerView with a list of applications.
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ApplicationView>
{
	// Attributes
	private final ArrayList<Application> applicationsList ;
	private final SharedPreferences settings ;
	boolean hide_app_name ;


	/**
	 * Constructor to fill a RecyclerView with the applications list.
	 * @param context To get the settings
	 * @param applicationsList Applications to display in the recycler
	 */
	public RecyclerAdapter(Context context, ArrayList<Application> applicationsList)
	{
		this.applicationsList = applicationsList ;
		settings = PreferenceManager.getDefaultSharedPreferences(context) ;
		this.hide_app_name = settings.getBoolean(Constants.HIDE_APP_NAMES, false) ;
	}
	public RecyclerAdapter(Context context, ArrayList<Application> applicationsList, boolean hide_app_name)
	{
		this.applicationsList = applicationsList ;
		settings = PreferenceManager.getDefaultSharedPreferences(context) ;
		this.hide_app_name = hide_app_name ;
	}


	/**
	 * Create an ApplicationView to add in the RecyclerView based on an XML layout.
	 * @param parent To get the context
	 * @param viewType Not used (herited)
	 * @return Created ApplicationView
	 */
	@NonNull
	@Override
	public ApplicationView onCreateViewHolder(ViewGroup parent, int viewType)
	{
		LayoutInflater inflater = LayoutInflater.from(parent.getContext()) ;
		View view = inflater.inflate(R.layout.view_application, parent, false) ;
		return new ApplicationView(view) ;
	}


	/**
	 * Write the metadata (name, icon) of each application in the RecyclerView
	 * @param appView Current application
	 * @param i For incrementation
	 */
	@Override
	public void onBindViewHolder(@NonNull ApplicationView appView, int i)
	{
		appView.name.setText(applicationsList.get(i).getDisplayName()) ;
		appView.name.setCompoundDrawables(null, applicationsList.get(i).getIcon(), null, null) ;

		// If the option is selected, hide applications names (but not folders names)
		if(!(applicationsList.get(i) instanceof Folder) && hide_app_name  )
				appView.name.setTextSize(0) ;
			else appView.name.setTextSize(14) ;
	}


	/**
	 * Return the number of items in the RecyclerView.
	 * @return Number of items
	 */
	@Override
	public int getItemCount()
	{
		return applicationsList.size() ;
	}


	/**
	 * Represent a clickable application item in the RecyclerView.
	 */
	public class ApplicationView extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
	{
		// Attributes
		private final TextView name ;


		/**
		 * Constructor.
		 * @param view To get the context
		 */
		ApplicationView(View view)
		{
			// Let the parent actions be performed
			super(view) ;

			// Listen for a click on the application
			name = view.findViewById(R.id.application_item) ;
			view.setOnClickListener(this) ;
			view.setOnLongClickListener(this) ;
		}


		/**
		 * Start the application when it is clicked.
		 * @param view Target element
		 */
		@Override
		public void onClick(View view)
		{
			// Start the application
			Application application = applicationsList.get(getBindingAdapterPosition()) ;
			if(application.start(view)) return ;

			// Display an error message if the application was not found
			Context context = view.getContext() ;
			ShowDialog.toastLong(context, context.getString(R.string.error_application_not_found, application.getDisplayName())) ;
		}


		/**
		 * When the application is long clicked, propose to open its system settings.
		 * @param view Target element
		 * @return <code>true</code> if the event is consumed, <code>false</code> otherwise
		 */
		@Override
		public boolean onLongClick(final View view)
		{
			// Get the clicked position and retrieve the selected application
			if(view == null) return false ;
			final Application application = applicationsList.get(getBindingAdapterPosition()) ;
			final Context context = view.getContext() ;

			// Show visual feedback (will be hidden after click or dismiss)
			setVisualFeedback(context, true) ;

			// Prepare and display the selection dialog
			AlertDialog.Builder dialog = new AlertDialog.Builder(context) ;
			if(application instanceof Shortcut)
				{
					dialog.setMessage(context.getString(R.string.dialog_open_or_remove, application.getDisplayName())) ;
					dialog.setPositiveButton(R.string.button_remove,
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								// Remove the shortcut from the file and update the applications list
								setVisualFeedback(context, false) ;
								ShortcutListener.removeShortcut(context, application.getDisplayName(), application.getApk()) ;
								ActivityMain.updateList(context) ;
								notifyDataSetChanged() ;
							}
						}) ;
				}
				else
				{
					dialog.setMessage(context.getString(R.string.dialog_open_or_settings, application.getDisplayName())) ;
					dialog.setPositiveButton(R.string.button_settings,
						new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								setVisualFeedback(context, false) ;
								if(application instanceof Folder) context.startActivity(new Intent().setClass(context, ActivityFolders.class)) ;
									else
									{
										// Open the application system settings
										Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS) ;
										intent.setData(Uri.parse("package:" + application.getApk())) ;
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ;
										context.startActivity(intent) ;
									}
							}
						}) ;
				}
			dialog.setNeutralButton(R.string.button_open,
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						// Start the application and display an error message if it was not found
						setVisualFeedback(context, false) ;
						if(!application.start(view))
							ShowDialog.toastLong(context, context.getString(R.string.error_application_not_found, application.getDisplayName())) ;
					}
				}) ;
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						setVisualFeedback(context, false) ;
					}
				}) ;
			dialog.show() ;
			return true ;
		}


		/**
		 * Show or hide visual feedback.
		 * @param context To get the colors
		 * @param display <code>true</code> to show, <code>false</code> to hide
		 */
		private void setVisualFeedback(Context context, boolean display)
		{
			if(display)
				{
					name.setTypeface(Typeface.DEFAULT_BOLD) ;
					name.setShadowLayer(15, 0, 0, context.getResources().getColor(R.color.white)) ;
					name.getCompoundDrawables()[1].setColorFilter(context.getResources().getColor(R.color.translucent_white), PorterDuff.Mode.SRC_ATOP) ;
				}
				else
				{
					name.setTypeface(Typeface.DEFAULT) ;
					name.setShadowLayer(0, 0, 0, 0) ;
					name.getCompoundDrawables()[1].clearColorFilter() ;
				}
		}
	}
}
