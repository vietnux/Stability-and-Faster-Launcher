package com.tglt.launcher.discreet;

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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tglt.launcher.discreet.core.Application;
import com.tglt.launcher.discreet.core.Folder;
import com.tglt.launcher.discreet.storage.InternalFileTXT;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Allow to manage favorites.
 */
public class ActivityQuickBar extends AppCompatActivity
{
	// Attributes
	private ArrayList<Application> quickbar ;
	private final RecyclerAdapter adapter = new RecyclerAdapter() ;
	private ItemTouchHelper touchManager ;
	private Button select_favorites_button ;
	private Button select_favorites_clear ;


	/**
	 * Constructor.
	 * @param savedInstanceState To retrieve the context
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Let the parent actions be performed
		super.onCreate(savedInstanceState);

		// Initializations
		setContentView(R.layout.activity_favorites) ;
		quickbar = ActivityMain.getApplicationsList().getQuickbar() ;

		// Display the sortable list of favorites
		RecyclerView recycler = findViewById(R.id.favorites_list) ;
		recycler.setLayoutManager(new LinearLayoutManager(this)) ;
		recycler.setAdapter(adapter) ;

		// Listen for sorting actions on the list
		touchManager = new ItemTouchHelper(new TouchCallback()) ;
		touchManager.attachToRecyclerView(recycler) ;

		select_favorites_button = findViewById(R.id.select_favorites_button) ;
		select_favorites_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				displayQuickbarSelectionDialog(view);
			}
		});
		select_favorites_button.setText(R.string.button_select_quickbar);

		select_favorites_clear = findViewById(R.id.select_favorites_clear) ;
		select_favorites_clear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				quickbar.clear();
				adapter.notifyDataSetChanged();

				final InternalFileTXT file = new InternalFileTXT(Constants.FILE_QUICK_BAR) ;
				file.remove();
			}
		});
		select_favorites_clear.setText(R.string.button_select_quickbar_clear);
	}


	/**
	 * Define how many favorites could be displayed without issues on the home screen.
	 * @return Maximum number of favorites
	 */
	private int defineMaxQuickbar()
	{
		// Retrieve the total height available in portrait mode (navigation bar automatically removed)
		DisplayMetrics metrics = getResources().getDisplayMetrics() ;
		int menu_button_height = Math.round(32 * metrics.density) ;
		int total_size = Math.max(metrics.heightPixels, metrics.widthPixels)
				- Math.round(25 * metrics.density)	// Remove 25dp for the status bar
				- Math.round(20 * metrics.density)  // Remove 20dp for button margins and spare
				- menu_button_height ;

		// Define the size of an app (text estimation + icon + margins) and the maximum number of quickbar
		int app_size = menu_button_height + Math.round(48 * metrics.density) + Math.round(20 * metrics.density) ;

		// Define the maximum number of quickbar that should be allowed
		return (total_size / app_size) * Constants.COLUMNS_PORTRAIT ;
	}


	/**
	 * Display the dialog allowing to select quickbar applications.
	 * @param view Clicked button
	 */
	public void displayQuickbarSelectionDialog(View view)
	{
		// Prepare the list of applications
		final ArrayList<Application> applications = new ArrayList<>(ActivityMain.getApplicationsList().getQuickbar()) ;
		ArrayList<Application> allApplications = ActivityMain.getApplicationsList().getApplications(true) ;
		for(Application application : allApplications)
			if(!applications.contains(application)) applications.add(application) ;

		// List the names of all applications
		CharSequence[] app_names = new CharSequence[applications.size()] ;
		int i = 0 ;
		for(Application application : applications)
		{
			if(application instanceof Folder) app_names[i] = ((Folder)application).getDisplayNameWithCount() ;
			else app_names[i] = application.getDisplayName() ;
			i++ ;
		}

		// Retrieve the currently selected applications
		final InternalFileTXT file = new InternalFileTXT(Constants.FILE_QUICK_BAR) ;
		final boolean[] selected = new boolean[app_names.length] ;
		if(file.exists()) for(i = 0 ; i < app_names.length ; i++) selected[i] = file.isLineExisting(applications.get(i).getName()) ;
		else for(i = 0 ; i < app_names.length ; i++) selected[i] = false ;

		// Prepare the title
		final int max_quickbar = defineMaxQuickbar() ;
		String dialog_title = getString(R.string.button_select_favorites) + " (" + max_quickbar + ")" ;

		// Prepare and display the selection dialog
		AlertDialog.Builder dialog = new AlertDialog.Builder(this) ;
		final Context context = this ;
		dialog.setTitle(dialog_title) ;
		dialog.setMultiChoiceItems(app_names, selected,
				new DialogInterface.OnMultiChoiceClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i, boolean b) { }
				}) ;
		dialog.setPositiveButton(R.string.button_apply,
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						// Remove the current file
						if(!file.remove()) return ;

						// Write the new selected applications to the file
						int selections_number = 0 ;
						for(i = 0 ; i < selected.length ; i++)
							if(selected[i])
							{
								// Add the application only if the maximum is not reached
								selections_number++ ;
								if((max_quickbar == -1) || (selections_number <= max_quickbar)) file.writeLine(applications.get(i).getName()) ;
								else
								{
									ShowDialog.toastLong(context, context.getString(R.string.error_too_many_selections, max_quickbar)) ;
									break ;
								}
							}

						// Update the quickbar applications list
						ActivityMain.updateQuickBar(); ;
						adapter.notifyDataSetChanged() ;
					}
				}) ;
		dialog.setNegativeButton(R.string.button_cancel, null) ;
		dialog.show() ;
	}


	/**
	 * Perform actions when the user leaves the activity.
	 */
	@Override
	public void onPause()
	{
		// Let the parent actions be performed
		super.onPause() ;

		// Write the last quickbar order in the file
		InternalFileTXT file = new InternalFileTXT(Constants.FILE_QUICK_BAR) ;
		if(file.remove())
			for(Application application : quickbar) file.writeLine(application.getName()) ;

		// Update the quickbar applications list
		ActivityMain.updateQuickBar(); ;
	}


	/**
	 * Fill a RecyclerView with the quickbar.
	 */
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.FavoriteView>
	{
		/**
		 * Create a FavoriteView to add in the RecyclerView based on an XML layout.
		 * @param parent To get the context
		 * @param viewType Not used (herited)
		 * @return Created FavoriteView
		 */
		@NonNull
		@Override
		public FavoriteView onCreateViewHolder(ViewGroup parent, int viewType)
		{
			LayoutInflater inflater = LayoutInflater.from(parent.getContext()) ;
			return new FavoriteView(inflater.inflate(R.layout.view_favorite, parent, false)) ;
		}


		/**
		 * Write the name of each favorite in the RecyclerView and listen for dragging action.
		 * @param favoriteView Current favorite
		 * @param i For incrementation
		 */
		@Override
		public void onBindViewHolder(@NonNull final FavoriteView favoriteView, int i)
		{
			// Display the name of the favorite
			if(quickbar.get(i) instanceof Folder)
					favoriteView.name.setText(((Folder)quickbar.get(i)).getDisplayNameWithCount()) ;
				else favoriteView.name.setText(quickbar.get(i).getDisplayName()) ;

			favoriteView.icon.setImageDrawable(quickbar.get(i).getIcon());

			// Listen for dragging action on the view of this favorite
			favoriteView.itemView.setOnTouchListener(new View.OnTouchListener()
			{
				/**
				 * Detect a user action on the view.
				 * @param event Gesture event
				 * @return <code>true</code> if event is consumed, <code>false</code> otherwise
				 */
				@Override
				public boolean onTouch(View view, MotionEvent event)
				{
					view.performClick() ;
					if(event.getAction() == MotionEvent.ACTION_DOWN) touchManager.startDrag(favoriteView) ;
					return false ;
				}
			}) ;
		}


		/**
		 * Return the number of items in the RecyclerView.
		 * @return Number of items
		 */
		@Override
		public int getItemCount()
		{
			return quickbar.size() ;
		}


		/**
		 * Represent a favorite item in the RecyclerView.
		 */
		public class FavoriteView extends RecyclerView.ViewHolder
		{
			// Attributes
			private final TextView name ;
			private final ImageView icon ;

			/**
			 * Constructor.
			 * @param view Element
			 */
			FavoriteView(View view)
			{
				super(view) ;
				name = view.findViewById(R.id.favorite_item) ;
				icon = view.findViewById(R.id.favorite_icon) ;
			}
		}
	}


	/**
	 * Called when a touch is detected on an element.
	 */
	public class TouchCallback extends ItemTouchHelper.Callback
	{
		/**
		 * Define the recognized touch gestures.
		 * @param recycler Target recycler
		 * @param viewHolder Target ViewHolder
		 * @return Mouvements allowed on the ViewHolder
		 */
		@Override
		public int getMovementFlags(@NonNull RecyclerView recycler, @NonNull RecyclerView.ViewHolder viewHolder)
		{
			return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) ;
		}


		/**
		 * Called when a ViewHolder is moved.
		 * @param recycler Target recycler
		 * @param source ViewHolder being dragged by the user
		 * @param target ViewHolder over which the user is dragging
		 * @return <code>true</code> if the ViewHolder have been moved to the target position
		 */
		@Override
		public boolean onMove(@NonNull RecyclerView recycler, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target)
		{
			// Retrieve the starting and ending positions of the dragging action
			int start_position = source.getBindingAdapterPosition() ;
			int end_position = target.getBindingAdapterPosition() ;

			// Perform the move and inform the adapter
			Collections.swap(quickbar, start_position, end_position) ;
			adapter.notifyItemMoved(start_position, end_position) ;
			return true ;
		}


		/**
		 * Needed to extend ItemTouchHelper.Callback but not used in practice.
		 * @param viewHolder Element targeted
		 * @param direction Swipe direction
		 */
		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
	}
}
