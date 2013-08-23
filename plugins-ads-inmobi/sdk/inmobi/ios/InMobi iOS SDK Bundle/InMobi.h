//
//  InMobi.h
//  General functions common to all InMobi SDKs
//
//  Copyright (c) 2013 InMobi. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>

// Console log levels
typedef enum {
    // No logs.
    IMLogLevelNone      = 0,

    // Minimal set of logs for debugging.
    IMLogLevelDebug     = 1,

    // Log everything
    // @note: Please turn off verbose mode before wide distribution like
    // AppStore. Keeping the verbose mode turned on might impact performance.
    IMLogLevelVerbose   = 2,
} IMLogLevel;

// Device Id collection masks
typedef enum {
    // Use default ids for sdk device id collection. (default)
    IMDeviceIdMaskIncludeDefaultIds = 0,

    // Exclude odin1 identifier from sdk device id collection.
    IMDeviceIdMaskExcludeODIN1 = 1<<0,

    // Exclude advertiser identifier from sdk device id collection. (iOS 6+)
    IMDeviceIdMaskExcludeAdvertisingId = 1<<1,

    // Exclude vendor identifier from sdk device id collection. (iOS 6+)
    IMDeviceIdMaskExcludeVendorId = 1<<2,

    // @deprecated
    // @note: This flag is deprecated as sdk does not collect UDID any more.
    //        Exclude udid identifier from sdk device id collection.
    IMDeviceIdMaskExcludeUDID = 1<<3,

    // Exclude facebook's attribution id from sdk device id collection.
    IMDeviceIdMaskExcludeFacebookAttributionId = 1<<4,
} IMDeviceIdMask;

// User ids to help deliver more relevant ads.
typedef enum {
    // User login id such as facebook, twitter, etc.
    kIMUserIdLogin,

    // For maintaining different sessions within the same login id.
    kIMUserIdSession,
} IMUserId;

// User Gender
typedef enum {
    kIMGenderNone,
    kIMGenderMale,
    kIMGenderFemale,
} IMGender;

// User Ethnicity
typedef enum {
    kIMEthnicityNone,
    kIMEthnicityMixed,
    kIMEthnicityAsian,
    kIMEthnicityBlack,
    kIMEthnicityHispanic,
    kIMEthnicityNativeAmerican,
    kIMEthnicityWhite,
    kIMEthnicityOther,
} IMEthnicity;

// User Education
typedef enum {
    kIMEducationNone,
    kIMEducationHighSchool,
    kIMEducationInCollege,
    kIMEducationBachelorsDegree,
    kIMEducationMastersDegree,
    kIMEducationDoctoralDegree,
    kIMEducationOther,
} IMEducation;

typedef enum {
    /**
     * The state of interstitial cannot be determined.
     */
    kIMInterstitialStateUnknown = 0,
    /**
     * The default state of an interstitial.
     * If an interstitial ad request fails, or if the user dismisses the
     * interstitial, the state will be changed back to init.
     */
	kIMInterstitialStateInit,
    /**
     * Indicates that an interstitial ad request is in progress.
     */
    kIMInterstitialStateLoading,
    /**
     * Indicates that an interstitial ad is ready to be displayed.
     * An interstitial ad can be displayed only if the state is ready.
     * You can call presentFromRootViewController: to display this ad.
     */
    kIMInterstitialStateReady,
    /**
     * Indicates that an interstitial ad is displayed on the user's screen.
     */
    kIMInterstitialStateActive
    
} IMInterstitialState;

typedef enum  {
    IMAdModeNetwork,
    IMAdModeAppGallery
} IMAdMode;

#pragma mark Refresh Intervals.

#define REFRESH_INTERVAL_OFF -1

// General functions common to all InMobi SDKs
@interface InMobi : NSObject

// Initialize InMobi SDKs with the Publisher Id obtained from InMobi portal.
+ (void)initialize:(NSString *)publisherSiteId;

#pragma mark Console Log Levels

// Set the console logging level.
+ (void)setLogLevel:(IMLogLevel)logLevel;

#pragma mark Device ID Mask

// This sets the Device Id Mask to restrict the Device Tracking not to be
// based on certain Device Ids.
+ (void)setDeviceIdMask:(IMDeviceIdMask)deviceIdMask;

#pragma mark SDK Information

// @return the sdk version.
+ (NSString *)getVersion;

#pragma mark User Information

// Set user's gender.
+ (void)setGender:(IMGender)gender;

// Set user's educational qualification.
+ (void)setEducation:(IMEducation)education;

// Set user's ethnicity.
+ (void)setEthnicity:(IMEthnicity)ethnicity;

// Set user's date of birth.
+ (void)setDateOfBirth:(NSDate *)dateOfBirth;
+ (void)setDateOfBirthWithMonth:(NSUInteger)month
                            day:(NSUInteger)day
                           year:(NSUInteger)year;

// Set user's annual income.
// @note Income should be in USD.
+ (void)setIncome:(NSInteger)income;

// Set user's age.
+ (void)setAge:(NSInteger)age;

// Set user's marital status.
+ (void)setMaritalStatus:(NSString *)status;

// Set whether the user has any children.
+ (void)setHasChildren:(NSString *)children;

// Set user's sexual orientation.
+ (void)setSexualOrientation:(NSString *)sexualOrientation;

// Set user's language preference.
+ (void)setLanguage:(NSString *)langugage;

// Set user's postal code.
+ (void)setPostalCode:(NSString *)postalCode;

// Set user's area code.
+ (void)setAreaCode:(NSString *)areaCode;

// Set user's interests (contextually relevant strings comma separated).
// Example: @"cars,bikes,racing"
+ (void)setInterests:(NSString *)interests;

#pragma mark User Location

// Use this to set the user's current location to deliver more relevant ads.
// However do not use Core Location just for advertising, make sure it is used
// for more beneficial reasons as well.  It is both a good idea and part of
// Apple's guidelines.
+ (void)setLocationWithLatitude:(CGFloat)latitude
                      longitude:(CGFloat)longitude
                       accuracy:(CGFloat)accuracyInMeters;

// Provide user location for city level targeting.
+ (void)setLocationWithCity:(NSString *)city
                      state:(NSString *)state
                    country:(NSString *)country;

#pragma mark User IDs

// Set user ids such as facebook, twitter etc to deliver more relevant ads.
+ (void)addUserID:(IMUserId)userId withValue:(NSString *)idValue;

// Remove the user ids which was set before. This fails silently if the id type
// was not set before.
+ (void)removeUserID:(IMUserId)userId;

@end
