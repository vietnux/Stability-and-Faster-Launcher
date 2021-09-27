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
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context ;
import android.content.Intent ;
import android.content.SharedPreferences ;
import android.content.pm.ActivityInfo ;
import android.content.res.Configuration ;
import android.os.Build ;
import android.os.Bundle ;
import androidx.annotation.NonNull ;
import androidx.annotation.RequiresApi;
import androidx.core.view.GestureDetectorCompat ;
import androidx.appcompat.app.AppCompatActivity ;
import androidx.core.view.MotionEventCompat;
import androidx.preference.PreferenceManager ;
import androidx.recyclerview.widget.GridLayoutManager ;
import androidx.recyclerview.widget.RecyclerView ;

import android.util.Log;
import android.view.ContextMenu ;
import android.view.GestureDetector ;
import android.view.LayoutInflater;
import android.view.MenuItem ;
import android.view.MotionEvent ;
import android.view.View ;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout ;
import android.widget.RelativeLayout;
import android.widget.TextView ;
import android.widget.Toast;

import com.tglt.launcher.discreet.core.Application ;
import com.tglt.launcher.discreet.core.ApplicationsList ;
import com.tglt.launcher.discreet.core.Folder ;
import com.tglt.launcher.discreet.events.PrefManager;
import com.tglt.launcher.discreet.events.ShortcutLegacyListener ;
import com.tglt.launcher.discreet.events.MinuteListener ;
import com.tglt.launcher.discreet.events.PackagesListener ;
import com.tglt.launcher.discreet.storage.InternalFileTXT ;

import java.net.URISyntaxException;
import java.util.ArrayList ;
import java.util.Set ;

/**
 * Main class activity managing the home screen and applications drawer.
 */
public class ActivityMain extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	// Attributes
	private static ApplicationsList applicationsList ;
	private static boolean ignore_settings_changes ;
	private static boolean adapters_update_needed ;
	private static String internal_folder ;
	private PackagesListener packagesListener ;
	private ShortcutLegacyListener shortcutLegacyListener ;
	private SharedPreferences settings ;
	private GestureDetectorCompat gestureDetector ;
	private NotificationMenu notificationMenu ;

	// Attributes related to the home screen
	private LinearLayout homeScreen ;
	private LinearLayout favorites ;
	private LinearLayout quickbar ;
	private RecyclerAdapter favoritesAdapter ;
	private RecyclerAdapter quickbarAdapter ;
	private MinuteListener minuteListener ;
	private boolean reverse_interface ;

	// Attributes related to the drawer
	private RecyclerView drawer ;
	private RecyclerAdapter drawerAdapter ;
	private GridLayoutManager drawerLayout ;
	private int drawer_position ;
	private int drawer_last_position ;
	private int drawer_close_gesture ;

	View view_root;
	int swipe = 0;
	ImageView imgSwipe;
	Animation aniSwipe;
	TextView tutorial_des;
	private PrefManager prefManager;

	private static Context context;
	private static ActivityMain mainActivity;
	public static Context getContext() {
		return context;
	}

	/**
	 * Better hope there's only one instance of this object.
	 */
	public static ActivityMain getMainActivity() {
		return mainActivity;
	}

	/**
	 * Constructor.
	 * @param savedInstanceState To retrieve the context
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Let the parent actions be performed
		super.onCreate(savedInstanceState) ;
		context = this;
		mainActivity = this;

		// Initializations
		internal_folder = getApplicationContext().getFilesDir().getAbsolutePath() ;

		// Assign default values to settings not configured yet
		PreferenceManager.setDefaultValues(this, R.xml.settings, true) ;
		PreferenceManager.setDefaultValues(this, R.xml.settings_display, true) ;

		// Retrieve the current settings and start to listen for changes
		settings = PreferenceManager.getDefaultSharedPreferences(this) ;
		convertHiddenApplications() ;
		settings.registerOnSharedPreferenceChangeListener(this) ;
		ignore_settings_changes = false ;

		// Check if the interface should be reversed
		reverse_interface = settings.getBoolean(Constants.REVERSE_INTERFACE, false) ;

		// Initializations related to the interface //rootView = inflater.inflate(layout, null);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(reverse_interface) view_root = inflater.inflate(R.layout.activity_main_reverse, null) ;
			else view_root = inflater.inflate(R.layout.activity_main, null) ;
		setContentView(view_root);

		homeScreen = findViewById(R.id.home_screen) ;
		favorites = findViewById(R.id.favorites) ;
		quickbar = findViewById(R.id.quickbar) ;
		drawer = findViewById(R.id.drawer) ;
		gestureDetector = new GestureDetectorCompat(this, new GestureListener()) ;
		registerForContextMenu(findViewById(R.id.access_menu_button)) ;

		// If it does not exist yet, build the applications list
		if(applicationsList == null) {
			applicationsList = new ApplicationsList() ;
			applicationsList.update(this) ;
		}

		// Update the display according to settings
		togglePortraitMode() ;
		if(settings.getBoolean(Constants.IMMERSIVE_MODE, false)) displaySystemBars(false) ;

		// Initialize the clock listener
		minuteListener = new MinuteListener((TextView)findViewById(R.id.clock_text)) ;
		registerReceiver(minuteListener, minuteListener.getFilter()) ;

		// Prepare the notification menu
		notificationMenu = new NotificationMenu(this) ;

		// Define the favorites panel and applications list layouts based on screen orientation
		GridLayoutManager favoritesLayout ;
		GridLayoutManager quickbarLayout ;
		String column = settings.getString("list_app_column", "4-5");
		String[] colums = column.split("-");
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			favoritesLayout = new GridLayoutManager(this, Constants.COLUMNS_LANDSCAPE) ;
			quickbarLayout = new GridLayoutManager(this, Constants.COLUMNS_QUICK_PORTRAIT) ;
//			drawerLayout = new GridLayoutManager(this, Constants.COLUMNS_LANDSCAPE) ;
			drawerLayout = new GridLayoutManager(this, Integer.parseInt(colums[1])  ) ;
		} else {
			favoritesLayout = new GridLayoutManager(this, Constants.COLUMNS_PORTRAIT) ;
			quickbarLayout = new GridLayoutManager(this, Constants.COLUMNS_QUICK_PORTRAIT) ;
//			drawerLayout = new GridLayoutManager(this, Constants.COLUMNS_PORTRAIT) ;
			drawerLayout = new GridLayoutManager(this, Integer.parseInt(colums[0])  ) ;

		}
		// Initialize the content of the favorites panel
		favoritesAdapter = new RecyclerAdapter(this, applicationsList.getFavorites()) ;
		RecyclerView favoritesRecycler = findViewById(R.id.favorites_applications) ;
		favoritesRecycler.setAdapter(favoritesAdapter) ;
		favoritesRecycler.setLayoutManager(favoritesLayout) ;

		// Initialize the content of the favorites panel
		quickbarAdapter = new RecyclerAdapter(this, applicationsList.getQuickbar(), true) ;
		RecyclerView quickbarRecycler = findViewById(R.id.layout_quickrow_scroll) ;
		quickbarRecycler.setAdapter(quickbarAdapter) ;
		quickbarRecycler.setLayoutManager(quickbarLayout) ;

		// Initialize the content of the full applications list
		drawerAdapter = new RecyclerAdapter(this, applicationsList.getDrawer()) ;
		drawer.setAdapter(drawerAdapter) ;
		drawer.setLayoutManager(drawerLayout) ;
		drawer.addOnScrollListener(new DrawerScrollListener()) ;

		// Hide the favorites panel and the drawer by default
		displayFavorites(false) ;
		displayDrawer(false) ;
		displayQuickbar(true);

		// Start to listen for packages added or removed
		packagesListener = new PackagesListener() ;
		registerReceiver(packagesListener, packagesListener.getFilter()) ;

		// When Android version is before Oreo, start to listen for legacy shortcut requests
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			shortcutLegacyListener = new ShortcutLegacyListener() ;
			registerReceiver(shortcutLegacyListener, shortcutLegacyListener.getFilter()) ;
		}
	}

	/**
	 * Force or not the portrait mode according to the settings.
	 */
	private void togglePortraitMode() {
		if(settings.getBoolean(Constants.FORCE_PORTRAIT, false))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
			else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) ;
	}

	/**
	 * Display or hide the favorites panel.
	 * @param display <code>true</code> to display, <code>false</code> to hide
	 */
	private void displayFavorites(boolean display) {
		if(display) {
			// Update the recyclers (favorites panel and applications drawer) if needed
			if(adapters_update_needed) updateAdapters() ;

			// Display a message if the user does not have any favorites applications yet
			if(applicationsList.getFavorites().size() == 0) findViewById(R.id.info_no_favorites_yet).setVisibility(View.VISIBLE) ;
				else findViewById(R.id.info_no_favorites_yet).setVisibility(View.GONE) ;

			// Display the favorites panel
			favorites.setVisibility(View.VISIBLE) ;

			// Check if the interface is reversed and adjust the display accordingly
			if(reverse_interface) getWindow().setNavigationBarColor(getResources().getColor(R.color.translucent_gray)) ;
				else getWindow().setStatusBarColor(getResources().getColor(R.color.translucent_gray)) ;
		} else {
			// Hide the favorites panel
			favorites.setVisibility(View.GONE) ;

			// If the option is selected, make the status bar fully transparent
			if(settings.getBoolean(Constants.TRANSPARENT_STATUS_BAR, false))
				getWindow().setStatusBarColor(getResources().getColor(R.color.transparent)) ;

			// Make the navigation bar transparent
			getWindow().setNavigationBarColor(getResources().getColor(R.color.transparent)) ;
		}
	}

	/**
	 * Display or hide the quickbar panel.
	 * @param display <code>true</code> to display, <code>false</code> to hide
	 */
	private void displayQuickbar(boolean display) {
		if(display) {
			// Update the recyclers (quickbar panel and applications drawer) if needed
			if(adapters_update_needed) updateAdapters() ;

			// Display a message if the user does not have any quickbar applications yet
//			if(applicationsList.getQuickbar().size() == 0) findViewById(R.id.info_no_favorites_yet).setVisibility(View.VISIBLE) ;
//				else findViewById(R.id.info_no_favorites_yet).setVisibility(View.GONE) ;

			// Display the favorites panel
			quickbar.setVisibility(View.VISIBLE) ;

			// Check if the interface is reversed and adjust the display accordingly
//			if(reverse_interface) getWindow().setNavigationBarColor(getResources().getColor(R.color.translucent_gray)) ;
//				else getWindow().setStatusBarColor(getResources().getColor(R.color.translucent_gray)) ;
		} else {
			// Hide the favorites panel
			quickbar.setVisibility(View.GONE) ;

			// If the option is selected, make the status bar fully transparent
//			if(settings.getBoolean(Constants.TRANSPARENT_STATUS_BAR, false))
//				getWindow().setStatusBarColor(getResources().getColor(R.color.transparent)) ;

			// Make the navigation bar transparent
//			getWindow().setNavigationBarColor(getResources().getColor(R.color.transparent)) ;
		}
	}


	/**
	 * Display or hide the applications drawer.
	 * @param display <code>true</code> to display, <code>false</code> to hide
	 */
	private void displayDrawer(boolean display) {
		if(display) {
			// Update the recyclers (favorites panel and applications drawer) if needed
			if(adapters_update_needed) updateAdapters() ;

			// Make the status and navigation bar translucent
			getWindow().setStatusBarColor(getResources().getColor(R.color.translucent_gray)) ;
			getWindow().setNavigationBarColor(getResources().getColor(R.color.translucent_gray)) ;

			// Display the applications drawer
			drawer_position = 0 ;
			drawer_last_position = 0 ;
			drawer_close_gesture = 0 ;
			homeScreen.setVisibility(View.GONE) ;
			drawer.setVisibility(View.VISIBLE) ;
		} else {
			// Hide the applications drawer
			drawer.scrollToPosition(0) ;
			homeScreen.setVisibility(View.VISIBLE) ;
			drawer.setVisibility(View.GONE) ;

			// If the option is selected, make the status bar fully transparent
			if(settings.getBoolean(Constants.TRANSPARENT_STATUS_BAR, false))
				getWindow().setStatusBarColor(getResources().getColor(R.color.transparent)) ;

			// Make the navigation bar transparent
			getWindow().setNavigationBarColor(getResources().getColor(R.color.transparent)) ;
		}
	}


	/**
	 * Display or hide the system bars (immersive mode).
	 * @param display <code>true</code> to display, <code>false</code> to hide
	 */
	private void displaySystemBars(boolean display) {
		View decorView = getWindow().getDecorView() ;
		if(display) decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE) ;
			else decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) ;
	}


	/**
	 * Return the list of applications.
	 * @return Contains the complete list, the favorites list and the last update timestamp
	 */
	public static ApplicationsList getApplicationsList()
	{
		return applicationsList ;
	}


	/**
	 * Allow to temporary disable the SharedPreference changes listener.
	 * @param new_value <code>true</code> to disable, <code>false</code> to enable
	 */
	static void setIgnoreSettingsChanges(boolean new_value)
	{
		ignore_settings_changes = new_value ;
	}


	/**
	 * Return the internal files folder location (must be initialized by ActivityMain).
	 * @return Internal files folder location on the system or <code>null</code> if not initialized
	 */
	public static String getInternalFolder()
	{
		return internal_folder ;
	}


	/**
	 * Update the favorites applications list
	 */
	public static void updateFavorites()
	{
		System.out.println("call updateFavorites") ;
		applicationsList.updateFavorites() ;
		adapters_update_needed = true ;
	}
	public static void updateQuickBar()
	{
		System.out.println("call updateQuickBar") ;
		applicationsList.updateQuickBar() ;
		adapters_update_needed = true ;
	}


	/**
	 * Update the applications list and inform the user.
	 * @param context Needed to update the list
	 */
	public static void updateList(Context context) {
		System.out.println("call updateList") ;
		applicationsList.update(context) ;
		adapters_update_needed = true ;
		ShowDialog.toast(context, R.string.info_applications_list_refreshed) ;
	}


	/**
	 * Update the display in the favorites panel and applications drawer.
	 */
	private void updateAdapters() {
		favoritesAdapter.notifyDataSetChanged() ;
		drawerAdapter.notifyDataSetChanged() ;
		quickbarAdapter.notifyDataSetChanged() ;
		adapters_update_needed = false ;
	}
	public void updateAdaptersQuick() {
		quickbarAdapter.notifyDataSetChanged() ;
		adapters_update_needed = false ;
	}

	
	/**
	 * Create the contextual menu.
	 * @param menu Used by the parent class
	 * @param view Used by the parent class
	 * @param menuInfo Used by the parent class
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo) ;
		getMenuInflater().inflate(R.menu.contextual_menu, menu) ;
	}


	/**
	 * Detect a click on an item from the contextual menu.
	 * @param item Entry clicked
	 * @return <code>true</code> if event is consumed, <code>false</code> otherwise
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		// Identify which menu entry has been clicked
		int selection = item.getItemId() ;

		// Check if the applications list should be refreshed
		if(selection == R.id.menu_action_refresh_list)
			{
				updateList(this) ;
				displayFavorites(false) ;
			}
			// Check if another activity should be started
			else if(selection == R.id.menu_action_manage_favorites) startActivity(new Intent().setClass(this, ActivityFavorites.class)) ;
			else if(selection == R.id.menu_action_manage_quickbar) startActivity(new Intent().setClass(this, ActivityQuickBar.class)) ;
			else if(selection == R.id.menu_action_organize_folders) startActivity(new Intent().setClass(this, ActivityFolders.class)) ;
			else if(selection == R.id.menu_action_settings) startActivity(new Intent().setClass(this, ActivitySettings.class)) ;
			// Check if the dialog to hide applications should be displayed
			else if(selection == R.id.menu_action_hide_applications) showDialogHideApplications() ;
			// In other cases, ignore the click
			else return false ;

		// Indicate that the event has been consumed
		return true ;
	}


	/**
	 * Display the dialog allowing to select hidden applications.
	 */
	private void showDialogHideApplications()
	{
		// Prepare the list of applications
		ArrayList<Application> applications = new ArrayList<>() ;
		applications.addAll(applicationsList.getHidden()) ;
		applications.addAll(applicationsList.getApplications(false)) ;

		// Hide the favorites panel and display the selection dialog
		displayFavorites(false) ;
		ShowDialog.multiSelect(this, R.string.button_hide_applications, applications, Constants.FILE_HIDDEN, -1) ;
	}


	/**
	 * Detect a click on an element from the activity.
	 * @param view Element clicked
	 */
	public void onClickMainActivity(View view)
	{
		// Display the contextual menu after a short click
		if(view.getId() == R.id.access_menu_button)
			{
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) view.showContextMenu() ;
					else view.showContextMenu(0, 0) ;
			}
	}


	/**
	 * Detect a user action on the screen.
	 * @param event Gesture event
	 * @return <code>true</code> if event is consumed, <code>false</code> otherwise
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		gestureDetector.onTouchEvent(event) ;
		return super.onTouchEvent(event) ;
	}


	/**
	 * Detect and recognize a gesture on the home screen.
	 */
	class GestureListener extends GestureDetector.SimpleOnGestureListener
	{
		/**
		 * Implemented because all gestures start with an onDown() message.
		 * @param event Gesture event
		 * @return <code>true</code>
		 */
		@Override
		public boolean onDown(MotionEvent event)
		{
			return true ;
		}

		/**
		 * Detect a gesture over a distance.
		 * @param event1 Starting point
		 * @param event2 Ending point
		 * @param velocityX On horizontal axis
		 * @param velocityY On vertical axis
		 * @return <code>true</code> if event is consumed, <code>false</code> otherwise
		 */
		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
		{
			// Ignore the gesture if the applications drawer is opened
			if(drawer.getVisibility() == View.VISIBLE) return false ;

			// Calculate the traveled distances on both axes
			float x_distance = Math.abs(event1.getX() - event2.getX()) ;
			float y_distance = event1.getY() - event2.getY() ;

			// Check if this is a vertical gesture over a distance and not a single tap
			if((Math.abs(y_distance) > x_distance) && (Math.abs(y_distance) > 100))
				{
					// Adapt the gesture direction to the interface direction
					boolean swipe_drawer ;
					if(reverse_interface) swipe_drawer = y_distance < 0 ;
						else swipe_drawer = y_distance > 0 ;

					// Check if the gesture is going up (if) or down (else), based on classic interface
					if(swipe_drawer)
						{
							// Display the applications drawer only if the favorites panel is closed
							if(favorites.getVisibility() == View.VISIBLE) displayFavorites(false) ;
								else displayDrawer(true) ;
						}
						else displayFavorites(true) ;

					// Indicate that the event has been consumed
					return true ;
				}

			// Ignore other gestures
			return false ;
		}

		/**
		 * Detect a long click on the home screen.
		 * @param event Click point
		 */
		@Override
		public void onLongPress(MotionEvent event)
		{
			// Update the display according to settings
			if(settings.getBoolean(Constants.IMMERSIVE_MODE, false)) displaySystemBars(false) ;
		}
	}


	/**
	 * Listen for changes in the settings and react accordingly.
	 * @param sharedPreferences Settings where the change happened
	 * @param key The value which has changed
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(ignore_settings_changes) return ;
		switch(key)
		{
			case Constants.REVERSE_INTERFACE:
				// Change the interface direction
				reverse_interface = settings.getBoolean(Constants.REVERSE_INTERFACE, false) ;
				if(reverse_interface) setContentView(R.layout.activity_main_reverse) ;
					else setContentView(R.layout.activity_main) ;
				recreate() ;
			case Constants.HIDE_APP_NAMES :
				// Update the recycler views
				adapters_update_needed = true ;
				break ;
			case Constants.ICON_PACK :
				// Update the applications list
				updateList(this) ;
				break ;
			case Constants.NOTIFICATION:
				// Toggle the notification
				if(settings.getBoolean(Constants.NOTIFICATION, true)) notificationMenu.display(this) ;
					else notificationMenu.hide() ;
				break ;
		}
	}


	/**
	 * Detect a scrolling action on the applications drawer.
	 */
	class DrawerScrollListener extends RecyclerView.OnScrollListener
	{
		/**
		 * When the scrolling ends, check if it is stuck on top.
		 * @param recyclerView Scrolled RecyclerView
		 * @param newState 1 (Active scrolling) then 2 (Scrolling inerty) then 0 (Not scrolling)
		 */
		@Override
		public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState)
		{
			// Let the parent actions be performed
			super.onScrollStateChanged(recyclerView, newState) ;

			// Keep track of the state to avoid accidental drawer closure
			switch(newState)
			{
				case RecyclerView.SCROLL_STATE_DRAGGING :
					if(drawer_close_gesture == 0) drawer_close_gesture = 1 ;
						else drawer_close_gesture = 0 ;
					break ;
				case RecyclerView.SCROLL_STATE_SETTLING :
					if(drawer_close_gesture == 1) drawer_close_gesture = 2 ;
						else drawer_close_gesture = 0 ;
					break ;
				case RecyclerView.SCROLL_STATE_IDLE :
					if(drawer_close_gesture == 2) drawer_close_gesture = 3 ;
						else drawer_close_gesture = 0 ;
			}

			// Wait for the gesture to be finished
			if(newState == RecyclerView.SCROLL_STATE_IDLE)
				{
					// If the scrolling is stuck on top, close the drawer activity
					if((drawer_position == 0) && (drawer_last_position == 0) && (drawer_close_gesture == 3))
						displayDrawer(false) ;

					// Consider the gesture finished
					if(drawer_close_gesture == 3) drawer_close_gesture = 0 ;

					// Update the last position to detect the stuck state
					drawer_last_position = drawer_position ;
				}
		}


		/**
		 * Update the position of the first visible item as the user is scrolling.
		 * @param recyclerView Scrolled RecyclerView
		 * @param dx Horizontal distance
		 * @param dy Vertical distance
		 */
		@Override
		public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy)
		{
			// Let the parent actions be performed
			super.onScrolled(recyclerView, dx, dy) ;

			// Update the position of the first visible item
			drawer_position = drawerLayout.findFirstCompletelyVisibleItemPosition() ;
		}
	}


	/**
	 * Close the applications drawer or favorites panel if opened, otherwise do nothing.
	 */
	@Override
	public void onBackPressed()
	{
		if(drawer.getVisibility() == View.VISIBLE) displayDrawer(false) ;
			else if(favorites.getVisibility() == View.VISIBLE) displayFavorites(false) ;
	}


	/**
	 * Perform actions when the user leaves the home screen.
	 */
	@Override
	public void onPause() {
		// Let the parent actions be performed
		super.onPause() ;

		// Always show the system bars
		displaySystemBars(true) ;

		// Hide folders if some are still opened
		for(Folder folder : applicationsList.getFolders()) folder.closePopup() ;

		// If the option is selected, display the notification
		if(settings.getBoolean(Constants.NOTIFICATION, true))
			notificationMenu.display(this) ;
	}


	/**
	 * Perform actions when the user come back to the home screen.
	 */
	@Override
	public void onResume() {
		// Let the parent actions be performed
		super.onResume() ;

		// Hide the favorites panel, the drawer and the notification
		displayFavorites(false) ;

		String column = settings.getString("list_app_column", "4-5");
		String[] colums = column.split("-");
//		Log.e("Colum", colums[1]+" === "+Integer.parseInt(colums[1])  );
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			drawerLayout = new GridLayoutManager(this, Integer.parseInt(colums[1])  ) ;
		} else {
			drawerLayout = new GridLayoutManager(this, Integer.parseInt(colums[0])  ) ;
		}
		drawer.setLayoutManager(drawerLayout) ;
		displayDrawer(false) ;
		notificationMenu.hide() ;
		closeContextMenu() ;

		// Update the display according to settings
		minuteListener.updateClock() ;
		togglePortraitMode() ;
		if(settings.getBoolean(Constants.IMMERSIVE_MODE, false)) displaySystemBars(false) ;

		// Update the favorites panel and applications drawer display if needed
		if(adapters_update_needed) updateAdapters() ;

		prefManager = new PrefManager(this);
//		Log.e("prefManager", " === "+prefManager.isFirstTimeLaunch());
		if (prefManager.isFirstTimeLaunch() ) {
			tutoirial();
			prefManager.setFirstTimeLaunch(false);
		}
	}

	/**
	 * Perform actions when the activity is destroyed.
	 */
	@Override
	public void onDestroy()
	{
		// Unregister all remaining broadcast receivers
		if(minuteListener != null) unregisterReceiver(minuteListener) ;
		if(packagesListener != null) unregisterReceiver(packagesListener) ;
		if(shortcutLegacyListener != null) unregisterReceiver(shortcutLegacyListener) ;

		// Let the parent actions be performed
		super.onDestroy() ;
	}


	/**
	 * Convert the hidden applications from settings to internal file.
	 */
	private void convertHiddenApplications()
	{
		// Check if there are still legacy hidden applications settings
		Set<String> hiddenApplications = settings.getStringSet(Constants.HIDDEN_APPLICATIONS, null) ;
		if(hiddenApplications == null) return ;

		// Convert the settings to an internal file
		InternalFileTXT file = new InternalFileTXT(Constants.FILE_HIDDEN) ;
		file.remove() ;
		String[] app_details ;
		for(String hidden_application : hiddenApplications)
		{
			app_details = hidden_application.split(Constants.NOTIFICATION_SEPARATOR) ;
			if(app_details.length < 2) continue ;
			file.writeLine(app_details[1]) ;
		}

		// Remove the hidden applications settings
		SharedPreferences.Editor editor = settings.edit() ;
		editor.putStringSet(Constants.HIDDEN_APPLICATIONS, null).apply() ;
	}

	int x_cord = 0;
	int y_cord = 0;
	@SuppressLint("ClickableViewAccessibility")
	private void tutoirial() {
		final RelativeLayout tutoirial = findViewById(R.id.tutoirial);
		tutoirial.setVisibility(View.VISIBLE);
		imgSwipe = (ImageView)findViewById(R.id.toturial_animation);
		tutorial_des = (TextView)findViewById(R.id.tutorial_des);
//		displayDrawer(true) ;
		aniSwipe = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.tutorial_swipe_down);
		imgSwipe.startAnimation(aniSwipe);
		swipe = 0;

//		view_root.setBackgroundColor(getResources().getColor(R.color.transparent));
//		getWindow().setNavigationBarColor(getResources().getColor(R.color.translucent_gray));
//		view_root.setAlpha(0.3f);
//		homeScreen.setBackgroundColor(getResources().getColor(R.color.transparent));
		homeScreen.setAlpha(0.5f);

		tutoirial.setOnTouchListener (new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
//				Log.e("SWIPE", " == "+swipe +"= motionEvent = "+motionEvent );
//				getWindow().setNavigationBarColor(getResources().getColor(R.color.transparent)) ;
				switch (motionEvent.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						x_cord = (int) motionEvent.getRawX();
						y_cord = (int) motionEvent.getRawY();
						break;
					case MotionEvent.ACTION_MOVE:
						break;
					case MotionEvent.ACTION_UP:
						int x_cord_new = (int) motionEvent.getRawX();
						int y_cord_new = (int) motionEvent.getRawY();
//						Log.e("SWIPE", " == "+y_cord_new +"= motionEvent = "+y_cord + " = " +(y_cord_new-y_cord) );
						int yy = Math.abs(y_cord_new - y_cord);
						if(  yy > 100 && swipe < 3 ) {
							imgSwipe.clearAnimation();
							switch (swipe) {
								case 0:
									imgSwipe.setBackgroundResource(R.drawable.swipe_down);
									displayDrawer(true) ;
									swipe = 1;
									drawer.setBackgroundColor(getResources().getColor(R.color.transparent));
									drawer.setAlpha(0.5f);
									aniSwipe = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.tutorial_swipe_up);
									imgSwipe.startAnimation(aniSwipe);
									break;
								case 1:
									swipe = 2;
//									imgSwipe.setBackgroundResource(R.drawable.swipe_up);
									displayDrawer(false) ;
									drawer.setBackgroundColor(getResources().getColor(R.color.translucent_gray));
									drawer.setAlpha(1f);
									imgSwipe.startAnimation(aniSwipe);
									break;
								case 2:
									swipe = 3;
									imgSwipe.setBackgroundResource(R.drawable.double_tap);
									tutorial_des.setText(R.string.tutorial_last);
									displayFavorites(true) ;

									aniSwipe = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.click);
									imgSwipe.startAnimation(aniSwipe);
									break;
								case 3:

							}
						} else if( Math.abs(x_cord_new - x_cord) < 100 &&yy < 100 && swipe == 3) {
							swipe = 0;
							displayFavorites(false) ;
							tutoirial.setVisibility(View.GONE);
							homeScreen.setAlpha(1f);
							break;
						}
						break;
				}

				return true;
			}
		});

	}
}
