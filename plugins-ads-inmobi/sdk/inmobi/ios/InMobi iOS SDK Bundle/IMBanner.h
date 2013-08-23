/**
 *   IMBanner.h
 * 
 *   Copyright (c) 2013 InMobi. All rights reserved.
*/
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "InMobi.h"
#import "IMBannerDelegate.h"

@protocol IMNetworkExtras;

@interface IMBanner : UIView

#pragma mark Ad Units
/**
 * The ad size equivalent to CGSizeMake(320, 48).
 * @deprecated Will be removed in a future release. Use IM_UNIT_320x50 instead.
 */
#define IM_UNIT_320x48        9
/**
 * Medium Rectangle size for the iPad (especially in a UISplitView's left pane).
 * The ad size equivalent to CGSizeMake(300, 250).
 */
#define IM_UNIT_300x250       10
/**
 * Leaderboard size for the iPad.
 * The ad size equivalent to CGSizeMake(728,90).
 */
#define IM_UNIT_728x90        11
/**
 * Full Banner size for the iPad (especially in a UIPopoverController or in
 * UIModalPresentationFormSheet).
 * The ad size equivalent to CGSizeMake(468x60).
 */
#define IM_UNIT_468x60        12
/**
 * Skyscraper size, designed for iPad's screen size.
 * The ad size equivalent to CGSizeMake(120x600).
 */
#define IM_UNIT_120x600       13
/**
 * Default ad size for iPhone and iPod Touch.
 * The ad size equivalent to CGSizeMake(320, 48).
 */
#define IM_UNIT_320x50        15

#pragma mark -- Constructors
/** Use this constructor to obtain an instance of IMBanner.
 * @param frame CGRect bounds for this view, typically according to the ad size requested.
 * @param adUnit Ad unit id to request the specific banner size.
 */
- (id)initWithFrame:(CGRect)frame adUnit:(int)adUnit;
/**
 * Use this constructor to obtain an instance of IMBanner with specific placement identifier
 * @param frame CGRect bounds for this view, typically according to the ad size requested.
 * @param slotId Slot Id to uniquely identify an ad slot in an app.
 */
- (id)initWithFrame:(CGRect)frame slot:(long long)slotId;

#pragma mark -- post Init util methods
 /** 
  * Call this method to refresh this view. 
  */
- (void)loadBanner;

 /**  
  Call this method to stop loading the current ad request. 
  */
- (void)stopLoading;

#pragma mark IB utils
/**
 * Utility setter for setting the ad-unit property of this object.
 * You may use this value explicitly if you're populating the view
 * using an Interface Builder, and a ad-unit value is required to be set.
 * For non IB integration, it is recommended to provide the ad-unit
 * in the constructor itself.
 * Please see above for the available values of the ad-unit.
 * @param _unit The ad-unit value as obtained from Inmobi.
 */
- (void)setAdUnit:(id)_unit;
/**
 * Utility setter for setting the slot-id property of this object.
 * You may use this value explicitly if you're populating the view
 * using an Interface Builder, and a slot-id value is required to be set.
 * For non IB integration, it is recommended to provide the slot-id
 * in the constructor itself.
 * @param _slotid The slot-id value as obtained from Inmobi.
 */
- (void)setSlotId:(long long)_slotid;

#pragma mark Optional properties
/**
 *  Delegate object that receives state change notifications from this view.
 *  Typically, this is a UIViewController instance.
 *  @note When releasing the adView in the dealloc method of your
 *  UIViewController, make sure you set its delegate to nil and remove it from
 *  its superview to prevent any chance of your application crashing.
 */
@property (nonatomic, assign) IBOutlet NSObject<IMBannerDelegate> *delegate;
/**
 *  Starts or stops the auto refresh of ads.
 *  The refresh interval is measured between the completion(success or failure)
 *  of the previous ad request and start of the next ad request. By default,
 *  the refresh interval is set to 60 seconds. Setting a new valid refresh
 *  interval value will start the auto refresh of ads if it is not already
 *  started. Use REFRESH_INTERVAL_OFF as the parameter to switch off auto
 *  refresh. When auto refresh is turned off, use the loadBanner method to
 *  manually load new ads. The SDK will not refresh ads if the screen is in the
 *  background or if the phone is locked.
 */
@property (nonatomic, assign) int refreshInterval;
/**
 * Ad networks may have additional parameters they accept. To pass these
 * parameters to them, create the ad network extras object for that network,
 * fill in the parameters, and register it here. The ad network should have a
 * header defining the interface for the 'extras' object to create.
 * @param networkExtras Extras object containing additional parameters for an
 * ad network.
 */
- (void)addAdNetworkExtras:(NSObject<IMNetworkExtras> *)networkExtras;

@end
