// ----------------------------------------------------------------------------
// 
// CoronaIOSInMobiAdsProvider.h
// Copyright (c) 2012 Corona Labs Inc. All rights reserved.
//
// ----------------------------------------------------------------------------

#ifndef _CoronaIOSInMobiAdsProvider_H__
#define _CoronaIOSInMobiAdsProvider_H__

#include "CoronaLua.h"

// ----------------------------------------------------------------------------

CORONA_EXPORT int luaopen_CoronaProvider_ads_inmobi( lua_State *L );

// ----------------------------------------------------------------------------

@class NSString;
@class IMBanner;
@protocol IMBannerDelegate;
@protocol CoronaRuntime;

namespace Corona
{

// ----------------------------------------------------------------------------

class IOSInMobiAdsProvider
{
	public:
		typedef IOSInMobiAdsProvider Self;

	public:
		static const char kName[];

	public:
		static int Open( lua_State *L );

	protected:
		static int Finalizer( lua_State *L );

	protected:
		static Self *GetSelf( lua_State *L );
		static int Init( lua_State *L );
		static int Show( lua_State *L );
		static int Hide( lua_State *L );

	public:
		IOSInMobiAdsProvider( id<CoronaRuntime> runtime );
		virtual ~IOSInMobiAdsProvider();

	public:
		bool Init( lua_State *L, const char *appId, int listenerIndex );
		bool Show( lua_State *L, const char *adUnitType, int index );
		void Hide();

	public:
		void DispatchEvent( bool isError ) const;
		void WillEnterFullScreen( IMBanner *view ) const;
		void DidExitFullScreen( IMBanner *view ) const;

	protected:
		id< IMBannerDelegate > fDelegate;
		id<CoronaRuntime> fRuntime;
		IMBanner *fAd;
		NSString *fAppId;
		CoronaLuaRef fListener;
};

// ----------------------------------------------------------------------------

} // namespace Corona

// ----------------------------------------------------------------------------

#endif // _CoronaIOSInMobiAdsProvider_H__
