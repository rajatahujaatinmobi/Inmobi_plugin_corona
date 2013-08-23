/**
 *   IMInterstitial.h
 *   InMobi AdNetwork SDK
 *   Copyright 2013 InMobi Technology Services Ltd. All rights reserved.
 */

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "InMobi.h"
#import "IMInterstitialDelegate.h"

@protocol IMNetworkExtras;

/**
 *  IMInterstitial is the class that displays an Interstitial Ad.
 *  Interstitials are full screen advertisements that are shown at natural
 *  transition points in your application such as between game levels, when
 *  switching news stories, in general when transitioning from one view controller
 *  to another. It is best to request for an interstitial several seconds before
 *  when it is actually needed, so that it can preload its content and become
 *  ready to present, and when the time comes, it can be immediately presented to
 *  the user with a smooth experience.
 */
@interface IMInterstitial : NSObject

 /**  Use this constructor to initialize an interstitial object. */
- (id)init;

 /**  
  * Use this constructor to initialize an interstitial object with specific placement identifier.
  * @param slotId The slot-id, as obtained from Inmobi.
  */
- (id)initWithSlot:(long long)slotId;

/**
 *  Delegate object that receives state change notifications from this
 *  interstitial object. Typically, this is a UIViewController instance.
 *  @warning In your UIViewController, make sure to set its delegate to
 *  nil to prevent any chance of your application crashing.
 *     - (void)dealloc {
 *         IMInterstitial.delegate = nil;
 *         [IMInterstitial release]; IMInterstitial = nil;
 *         [super dealloc];
 *     }
 */
@property (nonatomic, assign) NSObject<IMInterstitialDelegate> *delegate;
/**
 *  Makes an interstitial ad request. This is best to do several seconds before
 *  the interstitial is needed to preload its content. Once, it is fetched and
 *  is ready to display, you can present it using presentFromRootViewController:
 *  method.
 */
- (void)loadInterstitial;
/*
 *  Call this method to stop loading the current interstitial ad request.
 */
- (void)stopLoading;

#pragma mark IB util methods
/** 
 * Utility setter for setting the slot-id property of this object.
 * You may use this value explicitly if you're populating the view 
 * using an Interface Builder, and a slot-id value is required to be set.
 * For non IB integration, it is recommended to provide the slot-id 
 * in the constructor itself.
 * @param _slotid The slot-id value, as obtained from Inmobi.
 */
- (void)setSlotID:(long long)_slotid;

#pragma mark Post-Request
/**
 *  Returns the state of the interstitial ad. The delegate's
 *  interstitialDidFinishRequest: will be called when this switches from the
 *  kIMInterstitialStateInit state to the kIMInterstitialStateReady state.
 */
@property (nonatomic, assign, readonly) IMInterstitialState state;
/**
 *  Set the adMode property to switch to app gallery mode.
 *  The default value is network.
 */
@property (nonatomic,assign) IMAdMode adMode;
/**
 *  This presents the interstitial ad that takes over the entire screen until
 *  the user dismisses it. This has no effect unless the interstitial state is
 *  kIMInterstitialStateReady and/or the delegate's interstitialDidReceiveAd:
 *  has been received. After the interstitial has been dismissed by the user,
 *  the delegate's interstitialDidDismissScreen: will be called.
 *  @param _animated Show the interstitial by using an animation. This is similar to
 *  presenting a Modal-View-Controller like animation, from the bottom.
 */
- (void)presentInterstitialAnimated:(BOOL)_animated;
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
