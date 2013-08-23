//
//  IMError
//  InMobi Mediation SDK
//
//  Copyright (c) 2013 InMobi Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

extern NSString *const kInMobiErrorDomain;

/// NSError codes for IMAdM error domain
typedef enum {
    /// The ad request is invalid, refer to localizedDescription for more
    /// information.
    kIMErrorInvalidRequest = 0,
    
    /// No ads were returned from the Ad Network
    kIMErrorNoFill,
    
    /// Mediation encountered an error
    kIMErrorInternal,
    
    /// Ad Format Not Supported
    kIMErrorAdFormatNotSupported,
    
    //// Ad request/rendering timed out.
    kIMErrorTimeout,
    
    //// Ad request was cancelled.
    kIMErrorRequestCancelled
    
} IMErrorCode;

/// This class represents the error generated due to invalid request parameters
/// or when the request fails to load
@interface IMError : NSError

@end
