// ----------------------------------------------------------------------------
// 
// LuaLoader.java
// Copyright (c) 2012 Corona Labs Inc. All rights reserved.
//
// ----------------------------------------------------------------------------

package CoronaProvider.ads.inmobi;

import android.app.Activity;
import android.location.Location;
import android.view.ViewGroup;
import android.util.Log;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.NamedJavaFunction;
import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaLuaEvent;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.inmobi.androidsdk.EducationType;
import com.inmobi.androidsdk.EthnicityType;
import com.inmobi.androidsdk.GenderType;
import com.inmobi.androidsdk.InMobiAdDelegate;
import com.inmobi.androidsdk.impl.InMobiAdView;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LuaLoader implements JavaFunction {
	/** Dispatcher used to call a Lua listener from the Corona runtime thread. */
	private CoronaRuntimeTaskDispatcher fDispatcher = null;

	/** String ID used to access Inneractive ads from their server. */
	private String fApplicationId = "";

	/** An InMobi banner "view" object to be displayed in an activity. */
	private InMobiAdView fInMobiBannerView = null;

	/** Absolute layout used as a container for the "fInMobiBannerView". */
	private android.widget.AbsoluteLayout fAbsoluteLayout = null;

	/** Timer used to update the InMobi ad banner at regular intervals. */
	private Timer fInMobiTimer = null;

	/** Lua registry ID to the InMobi ad listener Lua function. */
	private int fListener = CoronaLua.REFNIL;


	/**
	 * Creates a new object for displaying banner ads on the CoronaActivity
	 */
	public LuaLoader() {
		initialize();
	}

	/** Initializes member variables. */
	protected void initialize() {
		// Initialize member variables.
		fDispatcher = null;
		fApplicationId = "";
		fListener = CoronaLua.REFNIL;
	}

	/**
	 * Warning! This method is not called on the main UI thread.
	 */
	@Override
	public int invoke(LuaState L) {
		initialize();
		fDispatcher = new CoronaRuntimeTaskDispatcher( L );

		// Listen for the Corona runtime's events.
		com.ansca.corona.CoronaEnvironment.addRuntimeListener(new CoronaRuntimeEventHandler());

		// Add a module named "myTests" to Lua having the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new InitWrapper(),
			new ShowWrapper(),
			new HideWrapper(),
		};

		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Add fallback test app id
		String testAppId = "4028cb962895efc50128fc99d4b7025b";
		L.pushString( testAppId );
		L.setField( -2, "testAppId" );

		return 1;
	}

	// ads.init( providerName, appId [, listener] )
	public int init(LuaState L) {
		boolean success = false;

		String appId = L.toString( 2 );
		if ( "" == fApplicationId ) {
			if ( null != appId ) {
				fApplicationId = appId;

				int listenerIndex = 3;
				if ( CoronaLua.isListener( L, listenerIndex, "adsRequest" ) ) {
					fListener = CoronaLua.newRef( L, listenerIndex );
				}

				success = true;
			}
		} else {
			Log.v( "Corona", "WARNING: ads.init() should only be called once. The application id has already been set to :" + fApplicationId + "." );
		}

		L.pushBoolean( success );

		return 1;
	}

	// ads.show( adUnitType [, x, y] [, params] )
	public int show(LuaState L) {
		// Fetch the Corona activity.
		// If not available, then the activity is exiting out.
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			return 0;
		}

		// Throw an exception if this application does not have the following permissions.
		activity.enforceCallingOrSelfPermission(android.Manifest.permission.INTERNET, null);
		activity.enforceCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE, null);
		activity.enforceCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE, null);

		// Fetch the ad type.
		int nextArg = 1;
		String adUnitType = L.checkString( nextArg++ );
		
		// Fetch settings from Lua, if given.
		int x = 0;
		int y = 0;
		boolean testModeEnabled = true;
		int intervalInSeconds = 10;
		int index = nextArg;
		if ( L.isTable( index ) ) {
			L.getField( index, "x" );
			if ( L.isNumber( -1 ) )
			{
				x = (int)Math.round( L.toNumber( -1 ) );
			}
			L.pop( 1 );

			L.getField( index, "y" );
			if ( L.isNumber( -1 ) )
			{
				y = (int)Math.round( L.toNumber( -1 ) );
			}
			L.pop( 1 );

			L.getField( index, "testMode" );
			if ( L.isBoolean( -1 ) )
			{
				testModeEnabled = L.toBoolean( -1 );
			}
			L.pop( 1 );

			L.getField( index, "interval" );
			if ( L.isNumber( -1 ) )
			{
				intervalInSeconds = (int)Math.round( L.toNumber( -1 ) );
			}
			L.pop( 1 );
		}
		
		android.graphics.Point p = activity.convertCoronaPointToAndroidPoint( x, y );
		if ( null != p ) {
			x = p.x;
			y = p.y;
		}

		// Display the ad.
		showInMobiAd( adUnitType, x, y, intervalInSeconds, testModeEnabled );
		return 0;
	}

	public int hide(LuaState L) {
		hideInMobiAd();
		return 0;
	}

	private class InitWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "init";
		}
		
		/**
		 * Warning! This method is not called on the main UI thread.
		 */
		@Override
		public int invoke(LuaState L) {
			return init(L);
		}
	}

	private class ShowWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "show";
		}
		
		/**
		 * Warning! This method is not called on the main UI thread.
		 */
		@Override
		public int invoke(LuaState L) {
			return show(L);
		}
	}

	private class HideWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "hide";
		}
		
		/**
		 * Warning! This method is not called on the main UI thread.
		 */
		@Override
		public int invoke(LuaState L) {
			return hide(L);
		}
	}

	/** InMobi banner ad event listener. */
	private class CoronaInMobiAdDelegate implements InMobiAdDelegate {
		/** String ID used to access InMobi ads from their server. */
		private String fApplicationId;
		
		/** Set true to enable test mode on the banner ad. */
		private boolean fIsTestModeEnabled;
		
		/** Creates a new InMobi event listener. */
		public CoronaInMobiAdDelegate(String applicationId, boolean testModeEnabled) {
			fApplicationId = applicationId;
			fIsTestModeEnabled = testModeEnabled;
		}
		
		/** Called when the InMobi view object has successfull received a new ad to be displayed. */
		@Override
		public void adRequestCompleted(InMobiAdView adView) {
			// Show the banner on the screen in case it was hidden before due to a failure.
			if ((adView != null) && (adView.getHandler() != null)) {
				adView.getHandler().post(new ChangeInMobiVisibilityOperation(adView, android.view.View.VISIBLE));
			}
			
			// Notify the system about the successful request.
			raiseAdRequestEvent(false);
		}
		
		/** Called when the InMobi view object failed to receive a new ad from the InMobi server. */
		@Override
		public void adRequestFailed(InMobiAdView adView) {
			// Log the failure.
			Log.v("Corona", "InMobi ad request failed");
			
			// Hide the ad banner after a failure. This way something else can be displayed in its place.
			// This allows users to tap behind where the banner is supposed to be.
			if ((adView != null) && (adView.getHandler() != null)) {
				adView.getHandler().post(new ChangeInMobiVisibilityOperation(adView, android.view.View.GONE));
			}
			
			// Notify the system about this failure.
			raiseAdRequestEvent(true);
		}
		
		/** Runnable class used to change the visibility state of an InMobiAdView banner. */
		private class ChangeInMobiVisibilityOperation implements Runnable {
			private InMobiAdView fAdView;
			private int fVisibilityState;
			public ChangeInMobiVisibilityOperation(InMobiAdView adView, int visibilityState) {
				fAdView = adView;
				fVisibilityState = visibilityState;
			}
			public void run() {
				if (fAdView != null) {
					fAdView.setVisibility(fVisibilityState);
				}
			}
		}
		
		/**
		 * To be called by this class to raise a Corona AdsRequestEvent.
		 * Sends notification to Corona when an ad request has succeeded or failed.
		 * @param isError Set true if the request failed. Set false if succeeded.
		 */
		private void raiseAdRequestEvent(final boolean isError) {
			// Set up a dispatcher which allows us to send a task to the Corona runtime thread from another thread.
			// This way we can call the given Lua function on the same thread that Lua runs in.
			// This dispatcher will only send tasks to the Corona runtime that owns the given Lua state object.
			// Once the Corona runtime is disposed/destroyed, which happens when the Corona activy is destroyed,
			// then this dispatcher will no longer be able to send tasks.
			final CoronaRuntimeTaskDispatcher dispatcher = fDispatcher;
			
			// Create a task that will call the given Lua function.
			// This task's execute() method will be called on the Corona runtime thread, just before rendering a frame.
			com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
				@Override
				public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
					// *** We are now running on the Corona runtime thread. ***
					try {
						// Fetch the Corona runtime's Lua state.
						LuaState L = runtime.getLuaState();

						CoronaLua.newEvent( L, CoronaLuaEvent.ADSREQUEST_TYPE );

						L.pushBoolean( isError );
						L.setField( -2, CoronaLuaEvent.ISERROR_KEY );

						L.pushString( "inmobi" );
						L.setField( -2, CoronaLuaEvent.PROVIDER_KEY );

						// Dispatch event table at top of stack
						CoronaLua.dispatchEvent( L, fListener, 0 );
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};
			
			// Send the above task to the Corona runtime asynchronously.
			// The send() method will do nothing if the Corona runtime is no longer available, which can
			// happen if the runtime was disposed/destroyed after the user has exited the Corona activity.
			dispatcher.send(task);
		}
		
		/** The application ID to be used when requesting a banner ad from InMobi's server. */
		@Override
		public String siteId() {
			return fApplicationId;
		}

		/** Gets the age of the current user. */
		@Override
		public int age() {
			return 0;
		}
		
		/** Gets the area code that the current user lives in. */
		@Override
		public String areaCode() {
			return null;
		}
		
		/** Gets the user's current location. */
		@Override
		public Location currentLocation() {
			return null;
		}

		/** Gets the current user's date of birth. */
		@Override
		public Date dateOfBirth() {
			return null;
		}
		
		/** Gets the current user's current education level. */
		@Override
		public EducationType education() {
			return null;
		}
		
		/** Gets the current user's ethnicity/race. */
		@Override
		public EthnicityType ethnicity() {
			return null;
		}
		
		/** Gets the current user's gender. */
		@Override
		public GenderType gender() {
			return null;
		}
		
		/** Gets the current user's income level. */
		@Override
		public int income() {
			return 0;
		}

		/** Gets the users interests such as hobbies. */
		@Override
		public String interests() {
			return null;
		}
		
		/** Determines if it is okay to fetch device's current location. */
		@Override
		public boolean isLocationInquiryAllowed() {
			return false;
		}
		
		/** Determines if this app is allowed to provide current device location. */
		@Override
		public boolean isPublisherProvidingLocation() {
			return false;
		}

		/** Gets the keywords to be associated with the assigned banner ad. */
		@Override
		public String keywords() {
			return null;
		}
		
		/** Gets the current user's zip code. */
		@Override
		public String postalCode() {
			return null;
		}
		
		/** Gets the search string  to be associated with the assigned banner ad. */
		@Override
		public String searchString() {
			return null;
		}
		
		/** Determines if test mode has been enabled. */
		@Override
		public boolean testMode() {
			return fIsTestModeEnabled;
		}
	}
	
	/**
	 * Shows an InMobi ad view on this manager's parent activity.
	 * @param bannerTypeName String key used to identify the dimensions of the banner to show, such as "banner320x48".
	 * @param x The pixel position where the top-left corner of the banner will be shown. Must be at least zero.
	 * @param y The pixel position where the top-left corner of the banner will be shown. Must be at least zero.
	 * @param intervalInSeconds How often the ad should be changed for a new one, in seconds.
	 * @param testModeEnabled Set true to enable the banner's test mode, as defined by InMobi.
	 */
	public void showInMobiAd(
		final String bannerTypeName, final float x, final float y,
		final double intervalInSeconds, final boolean testModeEnabled)
	{
		// Fetch the Corona activity.
		// If not available, then the activity is exiting out.
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			return ;
		}
		
		// Do not continue if an application ID was not assigned.
		if ((fApplicationId == null) || (fApplicationId.length() <= 0)) {
			return;
		}
		
		// Copy the reference of the current application ID to be used in the below threaded operation.
		// This is to avoid a race condition in case the ID changes on us from another thread.
		final String copyOfApplicationId = fApplicationId;
		
		// Run the "show" operation on the UI thread. Executes immediately if we're already on the UI thread.
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// Hide the last banner.
				hideInMobiAd();
				
				// Fetch the Corona activity, if still available.
				CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
				if (activity == null) {
					return ;
				}

				// Get the banner type name's unique ID, width, and height.
				int bannerTypeId;
				int bannerWidth;
				int bannerHeight;
				String lowerCaseBannerTypeName = bannerTypeName.trim().toLowerCase();
				if (lowerCaseBannerTypeName.equals("banner320x48")) {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_320X48;
					bannerWidth = 320;
					bannerHeight = 48;
				}
				else if (lowerCaseBannerTypeName.equals("banner300x250")) {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_300X250;
					bannerWidth = 300;
					bannerHeight = 250;
				}
				else if (lowerCaseBannerTypeName.equals("banner728x90")) {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_728X90;
					bannerWidth = 728;
					bannerHeight = 90;
				}
				else if (lowerCaseBannerTypeName.equals("banner468x60")) {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_468X60;
					bannerWidth = 468;
					bannerHeight = 60;
				}
				else if (lowerCaseBannerTypeName.equals("banner120x600")) {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_120X600;
					bannerWidth = 120;
					bannerHeight = 600;
				}
				else {
					bannerTypeId = InMobiAdDelegate.INMOBI_AD_UNIT_320X48;
					bannerWidth = 320;
					bannerHeight = 48;
					Log.v("Corona", "InMobi does not support banner name '" + bannerTypeName +
					      "' given to ads.show() function. Defaulting to 'banner320x48'.");
				}
				
				// Convert width and height to density independent coordinates.
				float displayDensity = activity.getResources().getDisplayMetrics().density;
				bannerWidth = (int)((bannerWidth * displayDensity) + 0.5f);
				bannerHeight = (int)((bannerHeight * displayDensity) + 0.5f);
				
				// Display the banner ad onscreen.
				CoronaInMobiAdDelegate delegate = new CoronaInMobiAdDelegate(copyOfApplicationId, testModeEnabled);
				fInMobiBannerView = InMobiAdView.requestAdUnitWithDelegate(
							CoronaEnvironment.getApplicationContext(), delegate, activity, bannerTypeId);
				if (fInMobiBannerView == null) {
					return;
				}
				fInMobiBannerView.setLayoutParams(new android.view.ViewGroup.LayoutParams((int)x + bannerWidth, (int)y + bannerHeight));
				fInMobiBannerView.setPadding((int)x, (int)y, 0, 0);
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					// Warning: Ads have major rendering problems on Android 4.x devices caused by buggy hardware acceleration.
					//          We can work-around this issue by using a software renderer for drawing the ads instead.
					try {
						java.lang.reflect.Method setLayerTypeMethod = android.view.ViewGroup.class.getMethod(
								"setLayerType", new Class[] {Integer.TYPE, android.graphics.Paint.class});
						setLayerTypeMethod.invoke(fInMobiBannerView, new Object[] { 1, null });
					}
					catch (Exception ex) { }
				}
				fAbsoluteLayout = new android.widget.AbsoluteLayout(activity);
				activity.getOverlayView().addView(fAbsoluteLayout);
				fAbsoluteLayout.addView(fInMobiBannerView);
				fInMobiBannerView.loadNewAd();
				
				// Set up a timer to update the banner ad.
				long intervalInMilliseconds = (long)intervalInSeconds * 1000;
				fInMobiTimer = new Timer();
				fInMobiTimer.schedule(new TimerTask() {
					public void run() {
						synchronized (this) {
							if (fInMobiBannerView != null) {
								fInMobiBannerView.loadNewAd();
							}
						}
					}
				}, intervalInMilliseconds, intervalInMilliseconds);
			}
		});
	}
	
	/**
	 * Hides the currently shown InMobi ad.
	 */
	public void hideInMobiAd() {
		// Fetch the Corona activity, if still available.
		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			return ;
		}
		
		// Stop here if a banner is not currently shown.
		if (fInMobiBannerView == null) {
			return;
		}
		
		// Remove the banner view from the activity.
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (fInMobiTimer != null) {
					fInMobiTimer.cancel();
					fInMobiTimer.purge();
					fInMobiTimer = null;
				}
				if (fInMobiBannerView != null) {
					fInMobiBannerView.stopReceivingNotifications();
					ViewGroup group = (ViewGroup)fInMobiBannerView.getParent();
					if (group != null) {
						group.removeView(fInMobiBannerView);
					}
					fInMobiBannerView = null;
				}
				if (fAbsoluteLayout != null) {
					ViewGroup group = (ViewGroup)fAbsoluteLayout.getParent();
					if (group != null) {
						group.removeView(fAbsoluteLayout);
					}
					fAbsoluteLayout = null;
				}
			}
		});
	}
	
	/**
	 * Determines if an InMobi ad is currently shown onscreen.
	 * @return Returns true if the ad is currently shown. Returns false if not.
	 */
	public boolean isInMobiAdShown() {
		return (fInMobiBannerView != null);
	}

	/** Receives and handles Corona runtime events. */
	private class CoronaRuntimeEventHandler implements com.ansca.corona.CoronaRuntimeListener {
		/**
		 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
		 * This is the application's opportunity to register custom APIs into Lua.
		 * <p>
		 * Warning! This method is not called on the main thread.
		 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
		 *                Provides a LuaState object that allows the application to extend the Lua API.
		 */
		@Override
		public void onLoaded(com.ansca.corona.CoronaRuntime runtime) {
		}

		/**
		 * Called just after the Corona runtime has executed the "main.lua" file.
		 * <p>
		 * Warning! This method is not called on the main thread.
		 * @param runtime Reference to the CoronaRuntime object that has just been started.
		 */
		@Override
		public void onStarted(com.ansca.corona.CoronaRuntime runtime) {
		}
		
		/**
		 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
		 * and other Corona related operations. This can happen when another Android activity (ie: window) has
		 * been displayed, when the screen has been powered off, or when the screen lock is shown.
		 * <p>
		 * Warning! This method is not called on the main thread.
		 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
		 */
		@Override
		public void onSuspended(com.ansca.corona.CoronaRuntime runtime) {
		}
		
		/**
		 * Called just after the Corona runtime has been resumed after a suspend.
		 * <p>
		 * Warning! This method is not called on the main thread.
		 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
		 */
		@Override
		public void onResumed(com.ansca.corona.CoronaRuntime runtime) {
		}
		
		/**
		 * Called just before the Corona runtime terminates.
		 * <p>
		 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
		 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
		 * method is called. This does not mean that the application is exiting.
		 * <p>
		 * Warning! This method is not called on the main thread.
		 * @param runtime Reference to the CoronaRuntime object that is being terminated.
		 */
		@Override
		public void onExiting(com.ansca.corona.CoronaRuntime runtime) {
			// Remove the last display ad if shown.
			// This way it will not continue to request new ads in the background.
			hideInMobiAd();

			// Stop listening for runtime events.
			com.ansca.corona.CoronaEnvironment.removeRuntimeListener(this);
		}
	}
}
