package org.webrtc;

import android.support.annotation.Nullable;

class NativeAndroidVideoTrackSource {
  private final long nativeAndroidVideoTrackSource;
  
  public NativeAndroidVideoTrackSource(long nativeAndroidVideoTrackSource) {
    this.nativeAndroidVideoTrackSource = nativeAndroidVideoTrackSource;
  }
  
  public void setState(boolean isLive) {
    nativeSetState(this.nativeAndroidVideoTrackSource, isLive);
  }
  
  @Nullable
  public VideoProcessor.FrameAdaptationParameters adaptFrame(VideoFrame frame) {
    return nativeAdaptFrame(this.nativeAndroidVideoTrackSource, frame.getBuffer().getWidth(), frame
        .getBuffer().getHeight(), frame.getRotation(), frame.getTimestampNs());
  }
  
  public void onFrameCaptured(VideoFrame frame) {
    nativeOnFrameCaptured(this.nativeAndroidVideoTrackSource, frame.getRotation(), frame
        .getTimestampNs(), frame.getBuffer());
  }
  
  public void adaptOutputFormat(VideoSource.AspectRatio targetLandscapeAspectRatio, @Nullable Integer maxLandscapePixelCount, VideoSource.AspectRatio targetPortraitAspectRatio, @Nullable Integer maxPortraitPixelCount, @Nullable Integer maxFps) {
    nativeAdaptOutputFormat(this.nativeAndroidVideoTrackSource, targetLandscapeAspectRatio.width, targetLandscapeAspectRatio.height, maxLandscapePixelCount, targetPortraitAspectRatio.width, targetPortraitAspectRatio.height, maxPortraitPixelCount, maxFps);
  }
  
  public void setIsScreencast(boolean isScreencast) {
    nativeSetIsScreencast(this.nativeAndroidVideoTrackSource, isScreencast);
  }
  
  @CalledByNative
  static VideoProcessor.FrameAdaptationParameters createFrameAdaptationParameters(int cropX, int cropY, int cropWidth, int cropHeight, int scaleWidth, int scaleHeight, long timestampNs, boolean drop) {
    return new VideoProcessor.FrameAdaptationParameters(cropX, cropY, cropWidth, cropHeight, scaleWidth, scaleHeight, timestampNs, drop);
  }
  
  private static native void nativeSetIsScreencast(long paramLong, boolean paramBoolean);
  
  private static native void nativeSetState(long paramLong, boolean paramBoolean);
  
  private static native void nativeAdaptOutputFormat(long paramLong, int paramInt1, int paramInt2, @Nullable Integer paramInteger1, int paramInt3, int paramInt4, @Nullable Integer paramInteger2, @Nullable Integer paramInteger3);
  
  @Nullable
  private static native VideoProcessor.FrameAdaptationParameters nativeAdaptFrame(long paramLong1, int paramInt1, int paramInt2, int paramInt3, long paramLong2);
  
  private static native void nativeOnFrameCaptured(long paramLong1, int paramInt, long paramLong2, VideoFrame.Buffer paramBuffer);
}
