//
//  MusicControls.m
//  
//
//  Created by Juan Gonzalez on 12/16/16.
//  Updated by Gaven Henry on 11/7/17 for iOS 11 compatibility & new features
//  Updated by Leo Schubert 11/25/17 for making the plugin work without a category of MainViewController + adding the getInfo call
//

#import "MusicControls.h"
#import "MusicControlsInfo.h"

//save the passed in info globally so we can configure the enabled/disabled commands and skip intervals
MusicControlsInfo * musicControlsSettings;

@implementation MusicControls
- (void) setObject:(NSObject*)obj inDict:(NSMutableDictionary*)dict forKey:(id<NSCopying>)key {
    if (obj==nil) {return;}
    [dict setObject:obj forKey:key];
}

- (void) create: (CDVInvokedUrlCommand *) command {
    NSDictionary * musicControlsInfoDict = [command.arguments objectAtIndex:0];
    MusicControlsInfo * musicControlsInfo = [[MusicControlsInfo alloc] initWithDictionary:musicControlsInfoDict];
    musicControlsSettings = musicControlsInfo;
    
    if (!NSClassFromString(@"MPNowPlayingInfoCenter")) {
        return;
    }
    
    [self.commandDelegate runInBackground:^{
        MPNowPlayingInfoCenter * nowPlayingInfoCenter =  [MPNowPlayingInfoCenter defaultCenter];
        NSDictionary * nowPlayingInfo = nowPlayingInfoCenter.nowPlayingInfo;
        NSMutableDictionary * updatedNowPlayingInfo = [NSMutableDictionary dictionaryWithDictionary:nowPlayingInfo];
        
        MPMediaItemArtwork * mediaItemArtwork = [self createCoverArtwork:[musicControlsInfo cover]];
        NSNumber * duration = [NSNumber numberWithDouble:[musicControlsInfo duration]];
        NSNumber * elapsed = [NSNumber numberWithDouble:[musicControlsInfo elapsed]];
        NSNumber * playbackRate = musicControlsInfo.isPlaying ? @(1.0) : @(0.0);
        
        if (mediaItemArtwork != nil) {
            [updatedNowPlayingInfo setObject:mediaItemArtwork forKey:MPMediaItemPropertyArtwork];
        }
        [self setObject:musicControlsInfo.artist inDict:updatedNowPlayingInfo forKey:MPMediaItemPropertyArtist];
        [self setObject:musicControlsInfo.track inDict:updatedNowPlayingInfo forKey:MPMediaItemPropertyTitle];
        [self setObject:musicControlsInfo.album inDict:updatedNowPlayingInfo forKey:MPMediaItemPropertyAlbumTitle];
        [self setObject:duration inDict:updatedNowPlayingInfo forKey:MPMediaItemPropertyPlaybackDuration];
        [self setObject:elapsed inDict:updatedNowPlayingInfo forKey:MPNowPlayingInfoPropertyElapsedPlaybackTime];
        [updatedNowPlayingInfo setObject:playbackRate forKey:MPNowPlayingInfoPropertyPlaybackRate];
        
        nowPlayingInfoCenter.nowPlayingInfo = updatedNowPlayingInfo;
    }];
}

//everything except artwork is converted back
- (void) getInfo: (CDVInvokedUrlCommand *) command {
    MPNowPlayingInfoCenter * center =  [MPNowPlayingInfoCenter defaultCenter];
    NSDictionary * info = center.nowPlayingInfo;
    NSMutableDictionary* outDict = [NSMutableDictionary dictionary];
    for(NSString* key in info) {
        NSObject* val=info[key];
        if (key==MPMediaItemPropertyArtwork) { //Artwork not JSON compatible
            continue;
        } else if (key==MPNowPlayingInfoPropertyElapsedPlaybackTime) {
            outDict[@"elapsed"]=val;
        } else if (key==MPMediaItemPropertyPlaybackDuration) {
            outDict[@"duration"]=val;
        } else if (key==MPMediaItemPropertyArtist) {
            outDict[@"artist"]=val;
        } else if (key==MPMediaItemPropertyTitle) {
            outDict[@"track"]=val;
        } else if (key==MPMediaItemPropertyAlbumTitle) {
            outDict[@"album"]=val;
        } else if (key==MPMediaItemPropertyAlbumArtist) {
            outDict[@"albumArtist"]=val;
        } else if (key==MPMediaItemPropertyComposer) {
            outDict[@"composer"]=val;
        } else if (key==MPMediaItemPropertyGenre) {
            outDict[@"genre"]=val;
        } else {
            NSInteger len=0;
            if ([key hasPrefix:@"MPNowPlayingInfoProperty"]) {
                len=@"MPNowPlayingInfoProperty".length;
            } else if ([key hasPrefix:@"MPMediaItemProperty"]) {
                len=@"MPMediaItemProperty".length;
            }
            if (len>0) { //cut the MP... apple prefixes
                NSString *first=[[key substringWithRange:NSMakeRange(len,1)] lowercaseString];
                NSString* newkey=[first stringByAppendingString:[key substringFromIndex:len+1]];
                outDict[newkey]=val;
            } else {
                outDict[key]=val;
            }
        }
    }
    CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:outDict];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) updateIsPlaying: (CDVInvokedUrlCommand *) command {
    NSDictionary * musicControlsInfoDict = [command.arguments objectAtIndex:0];
    MusicControlsInfo * musicControlsInfo = [[MusicControlsInfo alloc] initWithDictionary:musicControlsInfoDict];
    NSNumber * elapsed = [NSNumber numberWithDouble:[musicControlsInfo elapsed]];
    NSNumber * playbackRate = musicControlsInfo.isPlaying ? @(1.0) : @(0.0);
    
    if (!NSClassFromString(@"MPNowPlayingInfoCenter")) {
        return;
    }

    MPNowPlayingInfoCenter * nowPlayingCenter = [MPNowPlayingInfoCenter defaultCenter];
    NSMutableDictionary * updatedNowPlayingInfo = [NSMutableDictionary dictionaryWithDictionary:nowPlayingCenter.nowPlayingInfo];
    
    [updatedNowPlayingInfo setObject:elapsed forKey:MPNowPlayingInfoPropertyElapsedPlaybackTime];
    [updatedNowPlayingInfo setObject:playbackRate forKey:MPNowPlayingInfoPropertyPlaybackRate];
    nowPlayingCenter.nowPlayingInfo = updatedNowPlayingInfo;
}

// this was performing the full function of updateIsPlaying and just adding elapsed time update as well
// moved the elapsed update into updateIsPlaying and made this just pass through to reduce code duplication
- (void) updateElapsed: (CDVInvokedUrlCommand *) command {
    [self updateIsPlaying:(command)];
}

- (void) destroy: (CDVInvokedUrlCommand *) command {
    [self deregisterMusicControlsEventListener];
}

- (void) watch: (CDVInvokedUrlCommand *) command {
    [self registerMusicControlsEventListener];
    [self setLatestEventCallbackId:command.callbackId];
}

- (MPMediaItemArtwork *) createCoverArtwork: (NSString *) coverUri {
    UIImage * coverImage = nil;
    
    if (coverUri == nil) {
        return nil;
    }
    
    if ([coverUri hasPrefix:@"http://"] || [coverUri hasPrefix:@"https://"]) {
        NSURL * coverImageUrl = [NSURL URLWithString:coverUri];
        NSData * coverImageData = [NSData dataWithContentsOfURL: coverImageUrl];
        
        coverImage = [UIImage imageWithData: coverImageData];
    }
    else if ([coverUri hasPrefix:@"file://"]) {
        NSString * fullCoverImagePath = [coverUri stringByReplacingOccurrencesOfString:@"file://" withString:@""];
        
        if ([[NSFileManager defaultManager] fileExistsAtPath: fullCoverImagePath]) {
            coverImage = [[UIImage alloc] initWithContentsOfFile: fullCoverImagePath];
        }
    }
    else if (![coverUri isEqual:@""]) {
        NSString * baseCoverImagePath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString * fullCoverImagePath = [NSString stringWithFormat:@"%@%@", baseCoverImagePath, coverUri];
    
        if ([[NSFileManager defaultManager] fileExistsAtPath:fullCoverImagePath]) {
            coverImage = [UIImage imageNamed:fullCoverImagePath];
        }
    }
    else {
        coverImage = [UIImage imageNamed:@"none"];
    }
    
    return [self isCoverImageValid:coverImage] ? [[MPMediaItemArtwork alloc] initWithImage:coverImage] : nil;
}

- (bool) isCoverImageValid: (UIImage *) coverImage {
    return coverImage != nil && ([coverImage CIImage] != nil || [coverImage CGImage] != nil);
}

//Handle seeking with the progress slider on lockscreen or control center
- (MPRemoteCommandHandlerStatus)changedThumbSliderOnLockScreen:(MPChangePlaybackPositionCommandEvent *)event {
    NSString * seekTo = [NSString stringWithFormat:@"{\"message\":\"music-controls-seek-to\",\"position\":\"%f\"}", event.positionTime];
    CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:seekTo];
    pluginResult.associatedObject = @{@"position":[NSNumber numberWithDouble: event.positionTime]};
    [self.commandDelegate sendPluginResult:pluginResult callbackId:[self latestEventCallbackId]];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (void)sendPluginAction:(NSString*)action
{
    NSString * jsonAction = [NSString stringWithFormat:@"{\"message\":\"%@\"}", action];
    CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:jsonAction];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestEventCallbackId];
}

//Handle the skip forward event
- (void) skipForwardEvent:(MPSkipIntervalCommandEvent *)event {
    [self sendPluginAction:@"music-controls-skip-forward"];
}

//Handle the skip backward event
- (void) skipBackwardEvent:(MPSkipIntervalCommandEvent *)event {
    [self sendPluginAction:@"music-controls-skip-backward"];
}

//Handler for the common actions
- (MPRemoteCommandHandlerStatus) remoteEvent:(MPRemoteCommandEvent *)event {
    MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
    NSString * action= nil ;
    MPRemoteCommand* cmd=event.command;
    if (center.playCommand==cmd) {
        action = @"music-controls-play";
    } else if (center.pauseCommand==cmd) {
        action = @"music-controls-pause";
    } else if (center.stopCommand==cmd) {
        action = @"music-controls-destroy";
    } else if (center.nextTrackCommand==cmd) {
        action = @"music-controls-next";
    } else if (center.previousTrackCommand==cmd) {
        action = @"music-controls-previous";
    } else if (center.togglePlayPauseCommand==cmd) {
        action = @"music-controls-toggle-play-pause";
    }
    if(action != nil && self.latestEventCallbackId!=nil){
        [self sendPluginAction:action];
    }
    return MPRemoteCommandHandlerStatusSuccess;
}

//There are only 3 button slots available so next/prev track and skip forward/back cannot both be enabled
//skip forward/back will take precedence if both are enabled
- (void) registerMusicControlsEventListener {
    if (_didRegister) {
        return;
    }
    _didRegister = true;
    //register required event handlers for standard controls
    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [commandCenter.playCommand setEnabled:true];
    [commandCenter.playCommand addTarget:self action:@selector(remoteEvent:)];
    [commandCenter.pauseCommand setEnabled:true];
    [commandCenter.pauseCommand addTarget:self action:@selector(remoteEvent:)];
    [commandCenter.stopCommand setEnabled:true];
    [commandCenter.stopCommand addTarget:self action:@selector(remoteEvent:)];
    [commandCenter.togglePlayPauseCommand setEnabled:true];
    [commandCenter.togglePlayPauseCommand addTarget:self action:@selector(remoteEvent:)];
    if(musicControlsSettings.hasNext){
        [commandCenter.nextTrackCommand setEnabled:true];
        [commandCenter.nextTrackCommand addTarget:self action:@selector(remoteEvent:)];
    }
    if(musicControlsSettings.hasPrev){
        [commandCenter.previousTrackCommand setEnabled:true];
        [commandCenter.previousTrackCommand addTarget:self action:@selector(remoteEvent:)];
    }
    
    //Some functions are not available in earlier versions
    if(musicControlsSettings.hasSkipForward){
        commandCenter.skipForwardCommand.preferredIntervals = @[@(musicControlsSettings.skipForwardInterval)];
        [commandCenter.skipForwardCommand setEnabled:true];
        [commandCenter.skipForwardCommand addTarget: self action:@selector(skipForwardEvent:)];
    }
    if(musicControlsSettings.hasSkipBackward){
        commandCenter.skipBackwardCommand.preferredIntervals = @[@(musicControlsSettings.skipBackwardInterval)];
        [commandCenter.skipBackwardCommand setEnabled:true];
        [commandCenter.skipBackwardCommand addTarget: self action:@selector(skipBackwardEvent:)];
    }
    if(musicControlsSettings.hasScrubbing){
        [commandCenter.changePlaybackPositionCommand setEnabled:true];
        [commandCenter.changePlaybackPositionCommand addTarget:self action:@selector(changedThumbSliderOnLockScreen:)];
    }
}

- (void) deregisterMusicControlsEventListener {
    if (!_didRegister) {
        return;
    }
    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [commandCenter.playCommand removeTarget:self];
    [commandCenter.pauseCommand removeTarget:self];
    [commandCenter.stopCommand removeTarget:self];
    [commandCenter.togglePlayPauseCommand removeTarget:self];
    [commandCenter.nextTrackCommand removeTarget:self];
    [commandCenter.previousTrackCommand removeTarget:self];
    [commandCenter.changePlaybackPositionCommand setEnabled:false];
    [commandCenter.changePlaybackPositionCommand removeTarget:self action:NULL];
    [commandCenter.skipForwardCommand removeTarget:self];
    [commandCenter.skipBackwardCommand removeTarget:self];
    
    [self setLatestEventCallbackId:nil];
    _didRegister=false;
}

- (void) dealloc {
    [self deregisterMusicControlsEventListener];
}

@end