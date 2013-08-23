//
//  IMAdMInMobiParams.h
//  InMobi
//

#import "IMNetworkExtras.h"
#import <UIKit/UIKit.h>

/**
 * Additional parameters that you want to pass to InMobi ad network
 */
@interface IMInMobiNetworkExtras : NSObject<IMNetworkExtras>

/**
 * A free form NSDictionary for any demographic information,
 * not available via InMobi class.
 */
@property (nonatomic,retain) NSDictionary *additionaParameters;

/**
 * A free form set of keywords, separated by ','.
 * Eg: "sports,cars,bikes"
 */
@property (nonatomic,copy) NSString *keywords;
/**
 * The animation transition, when a banner ad is refresh.
 * @note: Applicable for banner ads only.
 */
@property (nonatomic,assign) UIViewAnimationTransition transition;

/**
 * Ref-tag utils
 */
@property (nonatomic,copy) NSString *refTagKey,*refTagValue;
@end
