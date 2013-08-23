// ----------------------------------------------------------------------------
// 
// CoronaIOSInMobiAdsProvider.cpp
// Copyright (c) 2012 Corona Labs Inc. All rights reserved.
// 
// ----------------------------------------------------------------------------

#include "CoronaIOSInMobiAdsProvider.h"

#include "CoronaAssert.h"
#include "CoronaEvent.h"
#include "CoronaLibrary.h"

#import "InMobi.h"
#import "IMBanner.h"
#import "IMBannerDelegate.h"
#import <Foundation/Foundation.h>
#import "CoronaRuntime.h"

// ----------------------------------------------------------------------------

@interface InMobiDelegate : NSObject < IMBannerDelegate >
{
	Corona::IOSInMobiAdsProvider *fOwner;
}

@end


@implementation InMobiDelegate

- (id)initWithOwner:(Corona::IOSInMobiAdsProvider*)owner
{
	self = [super init];

	if ( self )
	{
		fOwner = owner;
	}

	return self;
}

- (void)adViewDidFinishRequest:(IMBanner *)adView
{
	fOwner->DispatchEvent( false );
}

- (void)adView:(IMBanner *)view didFailRequestWithError:(IMError *)error
{
	fOwner->DispatchEvent( true );
}

- (void)adViewWillPresentScreen:(IMBanner *)adView
{
	fOwner->WillEnterFullScreen( adView );
}

- (void)adViewWillDismissScreen:(IMBanner *)adView
{
}

- (void)adViewDidDismissScreen:(IMBanner *)adView
{
	fOwner->DidExitFullScreen( adView );
}

- (void)adViewWillLeaveApplication:(IMBanner *)adView
{
}

@end

// ----------------------------------------------------------------------------

CORONA_EXPORT
int luaopen_CoronaProvider_ads_inmobi( lua_State *L )
{
	return Corona::IOSInMobiAdsProvider::Open( L );
}

// ----------------------------------------------------------------------------

namespace Corona
{

// ----------------------------------------------------------------------------

const char IOSInMobiAdsProvider::kName[] = "CoronaProvider.ads.inmobi";

int
IOSInMobiAdsProvider::Open( lua_State *L )
{
	void *platformContext = CoronaLuaGetContext( L ); // lua_touserdata( L, lua_upvalueindex( 1 ) );
	id<CoronaRuntime> runtime = (id<CoronaRuntime>)platformContext;

	const char *name = lua_tostring( L, 1 ); CORONA_ASSERT( 0 == strcmp( name, kName ) );
	int result = CoronaLibraryProviderNew( L, "ads", name, "com.inmobi" );

	if ( result )
	{
		const luaL_Reg kFunctions[] =
		{
			{ "init", Self::Init },
			{ "show", Self::Show },
			{ "hide", Self::Hide },

			{ NULL, NULL }
		};

		CoronaLuaInitializeGCMetatable( L, kName, Finalizer );

		// Use 'provider' in closure for kFunctions
		Self *provider = new Self( runtime );
		CoronaLuaPushUserdata( L, provider, kName );
		luaL_openlib( L, NULL, kFunctions, 1 );

		const char kTestAppId[] = "4028cb962895efc50128fc99d4b7025b";
		lua_pushstring( L, kTestAppId );
		lua_setfield( L, -2, "testAppId" );
	}

	return result;
}

int
IOSInMobiAdsProvider::Finalizer( lua_State *L )
{
	Self *provider = (Self *)CoronaLuaToUserdata( L, 1 );
	delete provider;
	return 0;
}

IOSInMobiAdsProvider *
IOSInMobiAdsProvider::GetSelf( lua_State *L )
{
	return (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
}

// ads.init( providerName, appId [, listener] )
int
IOSInMobiAdsProvider::Init( lua_State *L )
{
	Self *provider = GetSelf( L );

	const char *appId = lua_tostring( L, 2 );

	bool success = provider->Init( L, appId, 3 );
	lua_pushboolean( L, success );

	return 1;
}

// ads.show( adUnitType [, params] )
int
IOSInMobiAdsProvider::Show( lua_State *L )
{
	Self *provider = GetSelf( L );

	const char *adUnitType = lua_tostring( L, 1 );
	int paramsIndex = 2;

	bool success = provider->Show( L, adUnitType, paramsIndex );
	lua_pushboolean( L, success );
	
	return 1;
}

// ads.hide()
int
IOSInMobiAdsProvider::Hide( lua_State *L )
{
	Self *provider = GetSelf( L );

	provider->Hide();

	return 0;
}

// ----------------------------------------------------------------------------

static int
AdUnitForString( const char *str, CGFloat& rWidth, CGFloat& rHeight )
{
	int result = IM_UNIT_320x50;
	rWidth = 320;
	rHeight = 50;

	if ( str )
	{
		if ( 0 == strcasecmp( "banner320x50", str ) )
		{
			result = IM_UNIT_320x50;
		}
		else if ( 0 == strcasecmp( "banner300x250", str ) )
		{
			result = IM_UNIT_300x250;
			rWidth = 300;
			rHeight = 250;
		}
		else if ( 0 == strcasecmp( "banner728x90", str ) )
		{
			// iPad only
			result = IM_UNIT_728x90;
			rWidth = 728;
			rHeight = 90;
		}
		else if ( 0 == strcasecmp( "banner468x60", str ) )
		{
			// iPad only
			result = IM_UNIT_468x60;
			rWidth = 468;
			rHeight = 60;
		}
		else if ( 0 == strcasecmp( "banner120x600", str ) )
		{
			// iPad only
			result = IM_UNIT_120x600;
			rWidth = 120;
			rHeight = 600;
		}
		else if ( 0 == strcasecmp( "banner320x48", str ) )
		{
			result = IM_UNIT_320x48;
		}
	}

	return result;
}

IOSInMobiAdsProvider::IOSInMobiAdsProvider( id<CoronaRuntime> runtime )
:	fDelegate( nil ),
	fRuntime( runtime ),
	fAd( nil ),
	fAppId( nil ),
	fListener( NULL )
{
}

IOSInMobiAdsProvider::~IOSInMobiAdsProvider()
{
	CoronaLuaDeleteRef( fRuntime.L, fListener );
	Hide();
	[fAppId release];
	[fDelegate release];
}

bool
IOSInMobiAdsProvider::Init( lua_State *L, const char *appId, int listenerIndex )
{
	bool result = false;

	if ( appId )
	{
		CORONA_ASSERT( NULL == fListener );

		fListener = 
			( CoronaLuaIsListener( L, listenerIndex, "adsRequest" ) ? CoronaLuaNewRef( L, 3 ) : NULL );

		fDelegate = [[InMobiDelegate alloc] initWithOwner:this];
		fAppId = [[NSString alloc] initWithUTF8String:appId];

		if ( ! appId )
		{
			NSLog( @"WARNING: No app id was supplied. A test app id will be used for ads served by InMobi." );
		}
        
        [InMobi initialize:fAppId];
        
		result = true;
	}

	return result;
}

static bool
FloatEqual( float a, float b )
{
	return fabsf( a - b ) < FLT_EPSILON;
}

bool
IOSInMobiAdsProvider::Show( lua_State *L, const char *adUnitType, int index )
{
	// If banner is already showing, hide the current ad
	if ( fAd )
	{
		Hide();
	}

	CGFloat w, h;
	int adUnit = AdUnitForString( adUnitType, w, h );

	// Defaults
	CGFloat x = 0;
	CGFloat y = 0;
	bool isTestMode = false;
	int interval = 60; // default interval

	if ( lua_istable( L, index ) )
	{
		lua_getfield( L, index, "x" );
		x = lua_tonumber( L, -1 );
		lua_pop( L, 1 );
		
		lua_getfield( L, index, "y" );
		y = lua_tonumber( L, -1 );
		lua_pop( L, 1 );

		lua_getfield( L, index, "testMode" );
		if ( lua_isboolean( L, -1 ) )
		{
			isTestMode = lua_toboolean( L, -1 );
            if(isTestMode)
            {
                NSLog(@"[INFO]: Please enable TestMode and add your DeviceID: %@ for your siteId. \n For more visit: https://www.inmobi.com/properties/index#/configure/%@/product/diagnostics. \n http://www.inmobi.com/support/art/23382291/21894911/setting-up-publisher-diagnostics/#settestmode", [[[UIDevice currentDevice] identifierForVendor] description], fAppId);
            }
		}
		lua_pop( L, 1 );

		lua_getfield( L, index, "interval" );
		if ( lua_isnumber( L, -1 ) )
		{
			interval = lua_tointeger( L, -1 );
			if ( ! lua_isnil( L, -1 ) )
			{
				if ( interval <= 0 )
				{
					interval = REFRESH_INTERVAL_OFF;
				}
				else
				{
					// 20 is minimum interval
					interval = ( 20 > interval ? 20 : interval );
				}
			}
		}
		lua_pop( L, 1 );
	}

	CORONA_ASSERT( ! fAd );

	// Convert (x,y) from Corona 'content' units to UIKit 'points'
	// NOTE: w, h are already in points, so no need to convert them
	CGPoint originContent = { x, y };
	CGPoint originPoints = [fRuntime coronaPointToUIKitPoint:originContent];
	x = originPoints.x;
	y = originPoints.y;


/*
	Rect bounds;
	bounds.xMin = x;
	bounds.yMin = y;
	bounds.xMax = x;
	bounds.yMax = y;

	// Mapping Corona rectangles to UIView rectangles. We only map the origin
	// b/c w,h are already in UIKit point sizes.
	// TODO: This isn't quite correct but captures the 80% usage cases.
	// Content scaling fortuitously works between iPhone and iPhone4,
	// but doesn't work on iPad (we punt on iPad and force the complexity on the developer)
	CGFloat sx, sy;
	const RenderingStream& stream = runtime->Stream();
	PlatformDisplayObject::CalculateContentToScreenScale( stream, runtime->Surface(), sx, sy );
	bool shouldScale = ! FloatEqual( 1.f, sx ) || ! FloatEqual( 1.f, sy );
	if ( shouldScale )
	{
		sx = 1.f / sx;
		sy = 1.f / sy;
		PlatformDisplayObject::CalculateScreenBounds( stream, sx, sy, bounds );
	}

	// w, h are already in points, so we add that in after all the mapping
	bounds.xMax = bounds.xMin + w;
	bounds.yMax = bounds.yMin + h;
*/
	CGRect r = CGRectMake( x, y, w, h );

	UIViewController *viewController = fRuntime.appViewController;

    IMBanner *view = [[IMBanner alloc] initWithFrame:r adUnit:adUnit];
    
	fAd = view;

	view.delegate = fDelegate;
	view.refreshInterval = interval;
//	[view setRefTag:@"CoronaSDK" forKey:@"ref-tag"];

	[viewController.view addSubview:view];
    [view loadBanner];

	// If the refreshInterval property is "off", we need to explicitly request the initial ad
	if ( REFRESH_INTERVAL_OFF == interval )
	{
		[view loadBanner];
	}

	return true;
}

void
IOSInMobiAdsProvider::Hide()
{
	if ( fAd )
	{
		fAd.refreshInterval = REFRESH_INTERVAL_OFF;
		fAd.delegate = nil;
		[fAd release];
		[fAd removeFromSuperview];
		fAd = nil;
	}
}

void
IOSInMobiAdsProvider::DispatchEvent( bool isError ) const
{
	const char kProviderName[] = "inmobi";

	lua_State *L = fRuntime.L;

	CoronaLuaNewEvent( L, CoronaEventAdsRequestName() );

	lua_pushstring( L, kProviderName );
	lua_setfield( L, -2, CoronaEventProviderKey() );

	lua_pushboolean( L, isError );
	lua_setfield( L, -2, CoronaEventIsErrorKey() );

	CoronaLuaDispatchEvent( L, fListener, 0 );
}

void
IOSInMobiAdsProvider::WillEnterFullScreen( IMBanner *view ) const
{
	[fRuntime suspend];
}

void
IOSInMobiAdsProvider::DidExitFullScreen( IMBanner *view ) const
{
	[fRuntime resume];
}

// ----------------------------------------------------------------------------

} // namespace Corona

// ----------------------------------------------------------------------------
