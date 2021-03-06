package it.fdev.unisaconnect;

import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crittercism.app.Crittercism;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.fdev.mailSync.MailChecker;
import it.fdev.scraper.esse3.Esse3BasicScraper.LoadStates;
import it.fdev.unisaconnect.data.SharedPrefDataManager;
import it.fdev.utils.ListAdapterDrawer;
import it.fdev.utils.ListAdapterDrawer.ListItemDrawer;
import it.fdev.utils.MyFragmentInterface;
import it.fdev.utils.UpdateChecker;
import it.fdev.utils.Utils;

//import com.google.analytics.tracking.android.EasyTracker;

public class MainActivity extends AppCompatActivity {

	public static boolean isTestingAPK = false;
	public static final String INTENT_LAUNCH_FRAGMENT = "it.fdev.launch_fragment";
	public final static String BROADCAST_LOADING_MESSAGE = "it.fdev.loading_message";
	public final static String BROADCAST_ERROR = "it.fdev.error";

	// Fragments which can be started at application startup (do not require
	// special configuration to start)
	public static enum BootableFragmentsEnum {
		ACCOUNT, MENSA, WEBMAIL, STUDENT_SERVICES, STAFF_SEARCH, TIMETABLE, MAP, WEATHER, LIBRETTO, ESSE3_WEB, APPELLI, PRESENZE, PAGAMENTI, PREFERENCES, NEWS_ALL, WEB_RADIO, BIBLIO_SEARCH, TIPO_CORSO, BUS
	}

	// References to fragments used to save the fragment to boot in the
	// preferences
	public static HashMap<BootableFragmentsEnum, Class<? extends MyFragmentInterface>> fragmentsIDs = new HashMap<MainActivity.BootableFragmentsEnum, Class<? extends MyFragmentInterface>>();
	static {
		fragmentsIDs.put(BootableFragmentsEnum.ACCOUNT, FragmentAccount.class);
		fragmentsIDs.put(BootableFragmentsEnum.MENSA, FragmentMensa.class);
		fragmentsIDs.put(BootableFragmentsEnum.WEBMAIL, FragmentWebmailWeb.class);
		fragmentsIDs.put(BootableFragmentsEnum.STUDENT_SERVICES, FragmentStudentServices.class);
		fragmentsIDs.put(BootableFragmentsEnum.STAFF_SEARCH, FragmentStaffSearch.class);
		fragmentsIDs.put(BootableFragmentsEnum.TIMETABLE, FragmentTimetable.class);
		fragmentsIDs.put(BootableFragmentsEnum.MAP, FragmentMap.class);
		fragmentsIDs.put(BootableFragmentsEnum.WEATHER, FragmentWeather.class);
		fragmentsIDs.put(BootableFragmentsEnum.LIBRETTO, FragmentLibretto.class);
		fragmentsIDs.put(BootableFragmentsEnum.ESSE3_WEB, FragmentEsse3Web.class);
		fragmentsIDs.put(BootableFragmentsEnum.APPELLI, FragmentAppelli.class);
		fragmentsIDs.put(BootableFragmentsEnum.PRESENZE, FragmentPresenze.class);
		fragmentsIDs.put(BootableFragmentsEnum.PAGAMENTI, FragmentPagamenti.class);
		fragmentsIDs.put(BootableFragmentsEnum.NEWS_ALL, FragmentNews.class);
		fragmentsIDs.put(BootableFragmentsEnum.WEB_RADIO, FragmentWebRadio.class);
		fragmentsIDs.put(BootableFragmentsEnum.BIBLIO_SEARCH, FragmentBiblioPrepareSearch.class);
		fragmentsIDs.put(BootableFragmentsEnum.PREFERENCES, FragmentPreferences.class);
		fragmentsIDs.put(BootableFragmentsEnum.TIPO_CORSO, FragmentTipoCorso.class);
	}

	public static final HashSet<Integer> actions = new HashSet<Integer>();
	static {
		actions.add(R.id.action_search_button);
		actions.add(R.id.action_accept_button);
		actions.add(R.id.action_edit_button);
		actions.add(R.id.action_add_button);
		actions.add(R.id.action_cancel_button);
		actions.add(R.id.action_refresh_button);
        actions.add(R.id.action_twitter_button);
        actions.add(R.id.action_feed_button);
	}

	private String mAppName;

	private static final int VALID_NAVIGATION_DRAWER_ELEMENTS_NUM = 9; 	// When testing is disabled only these elements are shown
	// To enable testing, in the account preferences as
	// username enter the string in Utils.TOGGLE_TESTING_STRING ("testing!")

	private IntentFilter mIntentFilter = new IntentFilter();
	private final BroadcastReceiver mHandlerBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			onNewBroadcast(context, intent);
		}
	};

	private static Set<Integer> mActionsToShow = new HashSet<Integer>();
	private boolean menuAlreadyToggled = false; 		// After the first time in onPostCreate the menu shouldn't be toggled automatically
	private boolean showMenuOnStartup = true; 			// Show the sliding menu on startup
	private FragmentManager mFragmentManager;
	private Menu mOptionsMenu;
	private ActionBar mActionBar;
	private ActionBarDrawerToggle mDrawerToggle;
	private String mActionbarTitle;
	private View loadingAnimationContainer;
	private ProgressBar loadingAnimationSpinner;
	private TextView loadingAnimationText;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private SharedPrefDataManager mDataManager;
	private List<WeakReference<Fragment>> mFragments = new ArrayList<WeakReference<Fragment>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			// The catch blocks exceptions before initialization of crittercism
			// isTestingAPK is true only if the app wasn't signed with a
			// production key (debug build from eclipse)
			isTestingAPK = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
			if (isTestingAPK) {
				Log.d(Utils.TAG, "The applications is running as debuggable!");
			}
		} catch (Exception e) {
			isTestingAPK = false;
		}

		// Initialize Crittercism
		Crittercism.initialize(getApplicationContext(), "5135ccc2558d6a05f7000024");
		Crittercism.setOptOutStatus(isTestingAPK);

		// Initialize Google Analytics
//		EasyTracker.getInstance().setContext(this);
//		EasyTracker.getInstance().activityStart(this);

		mFragmentManager = getSupportFragmentManager();

		mIntentFilter.addAction(BROADCAST_LOADING_MESSAGE);
		mIntentFilter.addAction(BROADCAST_ERROR);

		mDataManager = SharedPrefDataManager.getInstance(this);

		mAppName = getString(R.string.app_name);
		mActionbarTitle = mAppName;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_logo);
            ActivityManager.TaskDescription tDesc = new ActivityManager.TaskDescription(getString(R.string.app_name), bm, getResources().getColor(R.color.orange_actionbar_recents));
            setTaskDescription(tDesc);
        }

		// set the Above View
		setContentView(R.layout.activity_main);
		loadingAnimationContainer = findViewById(R.id.content_loading_container);
		loadingAnimationSpinner = (ProgressBar) findViewById(R.id.content_loading_spinner);
		loadingAnimationText = (TextView) findViewById(R.id.content_loading_text);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// Set spinner color
		loadingAnimationSpinner.getIndeterminateDrawable().setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.MULTIPLY);

		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.ic_drawer_shadow, GravityCompat.START);

		// customize the actionbar
		mActionBar = getSupportActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.orange_dark));
        }

		// Initialize Drawer
		initializeDrawer();

		// Initialize image downloader used by fragments
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder().cacheOnDisk(true).build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).defaultDisplayImageOptions(defaultOptions).memoryCacheExtraOptions(100, 100).build();
		ImageLoader.getInstance().init(config);

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("launch_fragment")) { // Intent contains fragment to show
			onNewIntent(getIntent());
		} else {
			// Start the last used fragment
			// Class<? extends MyFragment> fragmentClassToBoot =
			// sharedPref.getBootFragmentClass();
			// MyFragment fragmentToBoot;
			// try {
			// fragmentToBoot = fragmentClassToBoot.newInstance();
			// } catch (Exception e) {
			// fragmentToBoot = new WifiPreferencesFragment();
			// }
			switchContent(BootableFragmentsEnum.STUDENT_SERVICES, true);
		}

		// setupUpxAppBanner();
		new UpdateChecker(this).start();

		// WVersionManager versionManager = new WVersionManager(this);
		// versionManager.setVersionContentUrl("http://fdev.eu/unisaconnect/version");
		// // your update content url, see the response format below
		// versionManager.checkVersion();

		MailChecker.autoSetAlarm(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mHandlerBroadcast, mIntentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(mHandlerBroadcast);
		} catch (IllegalArgumentException e) {
			// Receiver was not registered. Ignore
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
//		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mActionsToShow = new HashSet<Integer>();
	}

	public void onNewBroadcast(Context context, Intent intent) {
		try {
			Log.d(Utils.TAG, "BROADCAST RECEIVED: " + intent.getAction());
			if (BROADCAST_LOADING_MESSAGE.equals(intent.getAction())) {
				int messageRes = intent.getIntExtra("message_res", 0);
				setLoadingText(messageRes);
			} else if (BROADCAST_ERROR.equals(intent.getAction())) {
				LoadStates state = (LoadStates) intent.getSerializableExtra("status");
				switch (state) {
				case NO_INTERNET:
					Utils.goToInternetError(this, null);
					break;
				case WRONG_DATA:
				case NO_DATA:
					Utils.createAlert(this, getString(R.string.dati_errati), BootableFragmentsEnum.ACCOUNT, false);
					break;
				case ESSE3_PROBLEM:
					String message = null;
					if (intent.hasExtra("message")) {
						message = intent.getStringExtra("message");
					} else {
						message = getString(R.string.problema_di_connessione_generico);
					}
					Utils.createAlert(this, message, BootableFragmentsEnum.STUDENT_SERVICES, false);
					break;
				case UNKNOWN_PROBLEM:
				default:
					Utils.createAlert(this, getString(R.string.problema_di_connessione_generico), null, true);
					break;
				}
				setLoadingVisible(false, false);
				int messageRes = intent.getIntExtra("message_res", 0);
				setLoadingText(messageRes);
			}
		} catch (Exception e) {
			Log.e(Utils.TAG, "onReceiveBroadcast exception", e);
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		try {
			setIntent(intent);
			Log.d(Utils.TAG, "Intent received");
			// Get the intent, verify the action and get the query
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				Log.d(Utils.TAG, "ACTION_SEARCH");
				String query = intent.getStringExtra(SearchManager.QUERY);
				executeSearch(query);
				return;
			} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				return;
			} else if (INTENT_LAUNCH_FRAGMENT.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Log.d(Utils.TAG, "Switch!");
					super.onNewIntent(intent);
					BootableFragmentsEnum fragmentToBootEnum = (BootableFragmentsEnum) extras.getSerializable("launch_fragment");
					switchContent(fragmentToBootEnum, true);
					showMenuOnStartup = false;
				}
			}
		} catch (Exception e) {
			Log.e(Utils.TAG, "onNewIntent exception", e);
		}
	}

	// Actionbar items
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			MyFragmentInterface fragment = getContentFrame();
			if (fragment == null) {
				return true;
			}
			switch (item.getItemId()) {
				case android.R.id.home:
					toggleDrawer();
					return true;
				case R.id.action_add_button:
					fragment.actionAdd();
					return true;
				case R.id.action_edit_button:
					fragment.actionEdit();
					return true;
				case R.id.action_accept_button:
					fragment.actionAccept();
					return true;
				case R.id.action_refresh_button:
					fragment.actionRefresh();
					return true;
				case R.id.action_cancel_button:
					fragment.actionCancel();
					return true;
				case R.id.action_twitter_button:
					fragment.actionTwitter();
					return true;
	            case R.id.action_feed_button:
                    fragment.actionFeed();
                    return true;
			}
		} catch (ClassCastException e) {
			Log.e(Utils.TAG, e.getMessage());
		} catch (NullPointerException e) {
			// Do nothing
		}
		return super.onOptionsItemSelected(item);
	}

	public void goToLastFrame() {
		if (mFragmentManager.getBackStackEntryCount() == 1) {
			setDrawerOpen(true);
		} else {
			mFragmentManager.popBackStack();
		}
	}

	/**
	 * http://stackoverflow.com/questions/9984089/memory-issues-fragments Memory management
	 */
	@Override
	public void onAttachFragment(Fragment fragment) {
		mFragments.add(new WeakReference<Fragment>(fragment));
	}

	private void recycleFragments() {
		FragmentTransaction ft = mFragmentManager.beginTransaction();
		for (WeakReference<Fragment> ref : mFragments) {
			Fragment fragment = ref.get();
			if (fragment != null) {
				ft.remove(fragment);
			}
		}
		ft.commit();
		mFragments.clear();
	}

	/**
	 * Changes the fragment shown. Using Enum to avoid creation of duplicate fragments which would be deferenced if similar fragment already exists as switchsContent(Fragment newFragment) does
	 *
	 * @param newFragmentEnum
	 *            fragment enum to show
	 * @param emptyBackStack
	 *            if true all existing fragments are removed from the backstack
	 */
	public void switchContent(BootableFragmentsEnum newFragmentEnum, boolean emptyBackStack) {
		if (newFragmentEnum == null)
			return;
		if (!fragmentsIDs.containsKey(newFragmentEnum)) {
			return;
		}
		try {
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			// fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
			// android.R.anim.fade_out);
			Fragment currFrag = mFragmentManager.findFragmentById(R.id.content_frame);
			Class<? extends Fragment> currClass = null;
			if (currFrag != null) {
				currClass = mFragmentManager.findFragmentById(R.id.content_frame).getClass(); 	// Classe del fragment attualmente visualizzato
			}
			Class<? extends MyFragmentInterface> newClass = fragmentsIDs.get(newFragmentEnum); 	// Classe del fragment da visualizzare
			if (currFrag == null || !newClass.equals(currClass)) { 								// I fragment sono diversi
				Fragment fragmentToShow = (Fragment) newClass.newInstance();
				if (emptyBackStack) {
					mFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
					recycleFragments();
				}
				fragmentTransaction.replace(R.id.content_frame, fragmentToShow, fragmentToShow.getClass().toString());
				fragmentTransaction.addToBackStack(null);
				fragmentTransaction.commit();
				mDataManager.setBootFragmentClass(newClass);
				// mDataManager.saveData();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		setDrawerOpen(false);
		setLoadingVisible(false, false);
		clearKeyboardFocus();
	}

	/**
	 * Changes the fragment shown
	 *
	 * @param newFragment
	 *            fragment frame to show
	 */
	public void switchContent(Fragment newFragment) {
		if (newFragment == null)
			return;

		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		// fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
		// android.R.anim.fade_out);
		boolean isSameFragment;
		String currentFragmentClass;
		try {
			currentFragmentClass = mFragmentManager.findFragmentById(R.id.content_frame).getClass().toString();
		} catch (Exception e) {
			currentFragmentClass = null;
		}
		String newFragmentClass = newFragment.getClass().toString();
		isSameFragment = newFragmentClass.equals(currentFragmentClass);
		if (!isSameFragment) {
			Fragment fragmentToReplace = mFragmentManager.findFragmentByTag(newFragmentClass);
			if (fragmentToReplace != null) {
				newFragment = fragmentToReplace;
			}
			fragmentTransaction.replace(R.id.content_frame, newFragment, newFragmentClass);
			fragmentTransaction.addToBackStack(null);
			fragmentTransaction.commit();
		}
		setDrawerOpen(false);
		setLoadingVisible(false, false);
		clearKeyboardFocus();
	}

	/**
	 * Versioni recenti di Android hanno un bug sull'actionbar una volta impostata come nascosta un action, il menu dev'essere invalidado affinchè venga mostrata nuovamente
	 */
	public void reloadActionButtons(MyFragmentInterface newFragment) {
		if (mOptionsMenu != null && newFragment != null) {
			try {
				mActionsToShow = newFragment.getActionsToShow();
				if (mActionsToShow == null) {
					mActionsToShow = new HashSet<Integer>();
				}
				supportInvalidateOptionsMenu();
			} catch (Exception e) {
				Log.e(Utils.TAG, "Error in reloadActionButtons", e);
			}
		}
	}

	public void setLoadingVisible(final boolean showActionbarAnimation) {
		if (mOptionsMenu != null) {
			if (mActionsToShow == null) {
				mActionsToShow = new HashSet<Integer>();
			}
			if (showActionbarAnimation) {
				mActionsToShow.add(R.id.action_loading_animation);
			} else {
				mActionsToShow.remove(R.id.action_loading_animation);
			}
			supportInvalidateOptionsMenu();
		}
	}

	public void setLoadingVisible(boolean showActionbarAnimation, boolean showContentFrameAnimation) {
		setLoadingVisible(showActionbarAnimation);
		if (showContentFrameAnimation) {
			loadingAnimationContainer.setVisibility(View.VISIBLE);
			loadingAnimationText.setVisibility(View.GONE);
		} else {
			loadingAnimationContainer.setVisibility(View.GONE);
			loadingAnimationText.setVisibility(View.GONE);
		}
	}

	public void setLoadingText(int resId) {
		try {
			setLoadingText(getString(resId));
		} catch (Exception e) {
			// Trying to display an error that is not present in the strings
			Log.w(Utils.TAG, "Trying to display an error that is not present in the strings", e);
		}
	}

	public void setLoadingText(String text) {
		Log.d(Utils.TAG, "TXT: " + text);
		if (text == null || text.isEmpty()) {
			loadingAnimationText.setText("");
			loadingAnimationText.setVisibility(View.GONE);
		} else {
			loadingAnimationText.setText(text);
			loadingAnimationText.setVisibility(View.VISIBLE);
		}
	}

	public void executeSearch(String query) {
		MyFragmentInterface fragment = getContentFrame();
		if (fragment != null) {
			fragment.executeSearch(query);
		}
	}

	public void updateActionbarTitle() {
		String title;
		if (!isDrawerOpen() && mActionbarTitle != null && menuAlreadyToggled) {
			title = mActionbarTitle;
		} else {
			title = mAppName;
		}
		if (mActionBar != null) { // When screen rotates and the fragment is
			// recreated
			mActionBar.setTitle(title);
		}
	}

	public void setActionbarTitle(int titleResId) {
		if (titleResId <= 0) {
			titleResId = R.string.app_name;
		}
		try {
			mActionbarTitle = getString(titleResId);
		} catch (Exception e) {
			mActionbarTitle = null;
		}
		updateActionbarTitle();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (savedInstanceState != null)
			menuAlreadyToggled = savedInstanceState.getBoolean("menuAlreadyToggled");
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!menuAlreadyToggled && showMenuOnStartup) {
					setDrawerOpen(true);
				}
				menuAlreadyToggled = true;
				mDrawerToggle.syncState();
			}
		}, 0);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (isDrawerOpen()) {
			for (Integer cItem : actions) {
				menu.findItem(cItem).setVisible(false);
			}
			return true;
		}

		if (mActionsToShow == null || mActionsToShow.isEmpty()) {
			MyFragmentInterface fragment = getContentFrame();
			if (fragment != null) {
				mActionsToShow = fragment.getActionsToShow();
			}
		}

		if (mActionsToShow == null) {
			mActionsToShow = new HashSet<Integer>();
		}
		if (mActionsToShow.contains(R.id.action_loading_animation)) {
			mActionsToShow.add(R.id.action_refresh_button);
		}
		for (Integer cItem : actions) {
			menu.findItem(cItem).setVisible(mActionsToShow.contains(cItem));
		}
		MenuItem refrButton = mOptionsMenu.findItem(R.id.action_refresh_button);
		if (mActionsToShow.contains(R.id.action_loading_animation)) {
			// refrButton.setActionView(R.layout.refresh_action_view);
			MenuItemCompat.setActionView(refrButton, R.layout.refresh_action_view);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.actionbar_menu, menu);
		mOptionsMenu = menu;

		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search_button));
		// Assumes current activity is the searchable activity
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(true); // Iconify the widget; do not expand it by default

		// SearchView searchWidget = (SearchView)
		// menu.findItem(R.id.action_search_button).getActionView();
		// searchWidget.setOnQueryTextListener(this);
		// return super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("menuAlreadyToggled", menuAlreadyToggled);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			MyFragmentInterface fragment = getContentFrame();
			if (fragment != null && !fragment.goBack())
				return true;

			if (mFragmentManager.getBackStackEntryCount() == 1) {
				if (isDrawerOpen())
					finish();
				else
					setDrawerOpen(true);
				return true;
			}
			setLoadingVisible(false, false);
			setDrawerOpen(false);
		}
		return super.onKeyUp(keyCode, event);
	}

	private void clearKeyboardFocus() {
		try {
			View focus = getCurrentFocus();
			if (focus != null) {
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
				focus.clearFocus();
			}
		} catch (Exception e) {
			// Ho avuto nullpointerexception su focus.clearFocus();
			// Non è un'eccezione importante
		}
	}

	/**
	 * DRAWER METHODS
	 */
	private void initializeDrawer() {
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				MainActivity.this.onDrawerClosed();
			}

			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				MainActivity.this.onDrawerOpened();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerLayout.setFocusableInTouchMode(false); // http://stackoverflow.com/questions/18293726/android-onbackpressed-not-being-called-when-navigation-drawer-open

		Resources resources = getResources();
		String[] menuText = resources.getStringArray(R.array.menu_items);
		TypedArray imgs = resources.obtainTypedArray(R.array.menu_icons);
		ArrayList<ListItemDrawer> listItem = new ArrayList<ListItemDrawer>();
		for (int i = 0; i < VALID_NAVIGATION_DRAWER_ELEMENTS_NUM; i++) {
			// On Gingerbread if no background is pecified it becomes black on
			// scroll
			listItem.add(new ListItemDrawer(menuText[i], imgs.getResourceId(i, -1)));
		}
		if (mDataManager.isTestingingEnabled()) {
			for (int i = VALID_NAVIGATION_DRAWER_ELEMENTS_NUM; i < menuText.length; i++) {
				// On Gingerbread if no background is specified it becomes black
				// on scroll
				listItem.add(new ListItemDrawer(menuText[i], imgs.getResourceId(i, -1), true));
			}
		}
		ListAdapterDrawer adapter = new ListAdapterDrawer(this, listItem);
		imgs.recycle();
		mDrawerList.setAdapter(adapter);
	}

	private MyFragmentInterface getContentFrame() {
		try {
			return (MyFragmentInterface) mFragmentManager.findFragmentById(R.id.content_frame);
		} catch (Exception e) {
			e.printStackTrace();
			Log.w(Utils.TAG, "Cannot cast frame to MyFrame");
		}
		return null;
	}

	private void selectItem(int position) {
		BootableFragmentsEnum newContent = null;
		switch (position) {
		case 0:
			newContent = BootableFragmentsEnum.STUDENT_SERVICES;
			break;
		case 1:
			newContent = BootableFragmentsEnum.MENSA;
			break;
		case 2:
			newContent = BootableFragmentsEnum.STAFF_SEARCH;
			break;
		case 3:
			newContent = BootableFragmentsEnum.WEATHER;
			break;
		case 4:
			newContent = BootableFragmentsEnum.TIMETABLE;
			break;
		case 5:
			newContent = BootableFragmentsEnum.NEWS_ALL;
			break;
		case 6:
			newContent = BootableFragmentsEnum.MAP;
			break;
		case 7:
			newContent = BootableFragmentsEnum.WEB_RADIO;
			break;
		case 8:
			int curVersion;
			try {
				curVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
				curVersion = -1;
			}
			Utils.sendSupportMail(this, "Riguardo \"Unisa Connect\"...", "------------------------\n" + "Versione Android: " + Build.VERSION.SDK_INT + "\n" + "Versione Unisa Connect: " + curVersion + "\n" + "------------------------\n");
			break;
		case 9:
			newContent = BootableFragmentsEnum.BUS;
			break;
		default:
			newContent = null;
		}
		if (newContent != null) {
			switchContent(newContent, true);
		}
	}

	// Events
	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			view.setSelected(true);
			selectItem(position);
		}
	}

	public void onDrawerOpened() {
		updateActionbarTitle(); // Update title to Unisa Connect
		supportInvalidateOptionsMenu(); // Hide action buttons
		clearKeyboardFocus();
	}

	public void onDrawerClosed() {
		updateActionbarTitle(); // Update title to fragment's title
		supportInvalidateOptionsMenu(); // Show action buttons
		clearKeyboardFocus();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggle
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	// Utils
	public void toggleDrawer() {
		setDrawerOpen(!isDrawerOpen());
	}

	public void setDrawerOpen(boolean open) {
		if (mDrawerLayout == null)
			return;
		if (open) {
			mDrawerLayout.openDrawer(mDrawerList);
		} else {
			mDrawerLayout.closeDrawer(mDrawerList);
		}
	}

	public boolean isDrawerOpen() {
		if (mDrawerLayout == null)
			return false;
		return mDrawerLayout.isDrawerOpen(mDrawerList);
	}

	// private void setupUpxAppBanner() {
	// final String voteURL = "http://www.agoratelematica.it/upperapp/#vota";
	// final View banner = findViewById(R.id.banner);
	//
	// View logo = banner.findViewById(R.id.banner_logo);
	// View text = banner.findViewById(R.id.banner_text);
	// View close = banner.findViewById(R.id.banner_close_button);
	//
	// // 8 DIC 2013
	// final long expirationMillis = 1386543599L * 1000L;
	// Calendar lastVoteDay = new GregorianCalendar();
	// lastVoteDay.setTime(new Date(expirationMillis));
	// Calendar now = new GregorianCalendar();
	// now.setTime(new Date());
	// // Se non si può più votare
	// if (now.after(lastVoteDay)) {
	// banner.setVisibility(View.GONE);
	// return;
	// }
	//
	// final Animation hideBannerFadeOutAnimation =
	// AnimationUtils.loadAnimation(getApplicationContext(),
	// android.R.anim.fade_out);
	// hideBannerFadeOutAnimation.setAnimationListener(new AnimationListener() {
	// @Override
	// public void onAnimationEnd(Animation animation) {
	// banner.setVisibility(View.GONE);
	// }
	// @Override
	// public void onAnimationRepeat(Animation animation) {
	// }
	// @Override
	// public void onAnimationStart(Animation animation) {
	// }
	// });
	//
	// OnClickListener startBrowserListener = new OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// EasyTracker.getTracker().sendEvent("ui_action", "button_press",
	// "upxapp_vote", 1L);
	// Intent i = new Intent(Intent.ACTION_VIEW);
	// i.setData(Uri.parse(voteURL));
	// startActivity(i);
	// }
	// };
	//
	// logo.setOnClickListener(startBrowserListener);
	// text.setOnClickListener(startBrowserListener);
	//
	// close.setOnClickListener(new OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// EasyTracker.getTracker().sendEvent("ui_action", "button_press",
	// "upxapp_close", 1L);
	// banner.startAnimation(hideBannerFadeOutAnimation);
	// }
	// });
	// }

}
