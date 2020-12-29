package org.webrtc;

import android.support.annotation.Nullable;

public interface VideoEncoderFactory {
  @Nullable
  @CalledByNative
  VideoEncoder createEncoder(VideoCodecInfo paramVideoCodecInfo);
  
  @CalledByNative
  VideoCodecInfo[] getSupportedCodecs();
  
  @CalledByNative
  default VideoCodecInfo[] getImplementations() {
    return getSupportedCodecs();
  }
  
  @CalledByNative
  default VideoEncoderSelector getEncoderSelector() {
    return null;
  }
  
  public static interface VideoEncoderSelector {
    @CalledByNative("VideoEncoderSelector")
    void onCurrentEncoder(VideoCodecInfo param1VideoCodecInfo);
    
    @Nullable
    @CalledByNative("VideoEncoderSelector")
    VideoCodecInfo onAvailableBitrate(int param1Int);
    
    @Nullable
    @CalledByNative("VideoEncoderSelector")
    VideoCodecInfo onEncoderBroken();
  }
}
