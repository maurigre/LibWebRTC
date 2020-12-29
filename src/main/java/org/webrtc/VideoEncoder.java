package org.webrtc;

import android.support.annotation.Nullable;

public interface VideoEncoder {
  public static class Settings {
    public final int numberOfCores;
    
    public final int width;
    
    public final int height;
    
    public final int startBitrate;
    
    public final int maxFramerate;
    
    public final int numberOfSimulcastStreams;
    
    public final boolean automaticResizeOn;
    
    public final VideoEncoder.Capabilities capabilities;
    
    @Deprecated
    public Settings(int numberOfCores, int width, int height, int startBitrate, int maxFramerate, int numberOfSimulcastStreams, boolean automaticResizeOn) {
      this(numberOfCores, width, height, startBitrate, maxFramerate, numberOfSimulcastStreams, automaticResizeOn, new VideoEncoder.Capabilities(false));
    }
    
    @CalledByNative("Settings")
    public Settings(int numberOfCores, int width, int height, int startBitrate, int maxFramerate, int numberOfSimulcastStreams, boolean automaticResizeOn, VideoEncoder.Capabilities capabilities) {
      this.numberOfCores = numberOfCores;
      this.width = width;
      this.height = height;
      this.startBitrate = startBitrate;
      this.maxFramerate = maxFramerate;
      this.numberOfSimulcastStreams = numberOfSimulcastStreams;
      this.automaticResizeOn = automaticResizeOn;
      this.capabilities = capabilities;
    }
  }
  
  public static class Capabilities {
    public final boolean lossNotification;
    
    @CalledByNative("Capabilities")
    public Capabilities(boolean lossNotification) {
      this.lossNotification = lossNotification;
    }
  }
  
  public static class EncodeInfo {
    public final EncodedImage.FrameType[] frameTypes;
    
    @CalledByNative("EncodeInfo")
    public EncodeInfo(EncodedImage.FrameType[] frameTypes) {
      this.frameTypes = frameTypes;
    }
  }
  
  public static class CodecSpecificInfo {}
  
  public static class CodecSpecificInfoVP8 extends CodecSpecificInfo {}
  
  public static class CodecSpecificInfoVP9 extends CodecSpecificInfo {}
  
  public static class CodecSpecificInfoH264 extends CodecSpecificInfo {}
  
  public static class BitrateAllocation {
    public final int[][] bitratesBbs;
    
    @CalledByNative("BitrateAllocation")
    public BitrateAllocation(int[][] bitratesBbs) {
      this.bitratesBbs = bitratesBbs;
    }
    
    public int getSum() {
      int sum = 0;
      for (int[] spatialLayer : this.bitratesBbs) {
        for (int bitrate : spatialLayer)
          sum += bitrate; 
      } 
      return sum;
    }
  }
  
  public static class ScalingSettings {
    public final boolean on;
    
    @Nullable
    public final Integer low;
    
    @Nullable
    public final Integer high;
    
    public static final ScalingSettings OFF = new ScalingSettings();
    
    public ScalingSettings(int low, int high) {
      this.on = true;
      this.low = Integer.valueOf(low);
      this.high = Integer.valueOf(high);
    }
    
    private ScalingSettings() {
      this.on = false;
      this.low = null;
      this.high = null;
    }
    
    @Deprecated
    public ScalingSettings(boolean on) {
      this.on = on;
      this.low = null;
      this.high = null;
    }
    
    @Deprecated
    public ScalingSettings(boolean on, int low, int high) {
      this.on = on;
      this.low = Integer.valueOf(low);
      this.high = Integer.valueOf(high);
    }
    
    public String toString() {
      return this.on ? ("[ " + this.low + ", " + this.high + " ]") : "OFF";
    }
  }
  
  public static class ResolutionBitrateLimits {
    public final int frameSizePixels;
    
    public final int minStartBitrateBps;
    
    public final int minBitrateBps;
    
    public final int maxBitrateBps;
    
    public ResolutionBitrateLimits(int frameSizePixels, int minStartBitrateBps, int minBitrateBps, int maxBitrateBps) {
      this.frameSizePixels = frameSizePixels;
      this.minStartBitrateBps = minStartBitrateBps;
      this.minBitrateBps = minBitrateBps;
      this.maxBitrateBps = maxBitrateBps;
    }
    
    @CalledByNative("ResolutionBitrateLimits")
    public int getFrameSizePixels() {
      return this.frameSizePixels;
    }
    
    @CalledByNative("ResolutionBitrateLimits")
    public int getMinStartBitrateBps() {
      return this.minStartBitrateBps;
    }
    
    @CalledByNative("ResolutionBitrateLimits")
    public int getMinBitrateBps() {
      return this.minBitrateBps;
    }
    
    @CalledByNative("ResolutionBitrateLimits")
    public int getMaxBitrateBps() {
      return this.maxBitrateBps;
    }
  }
  
  @CalledByNative
  default long createNativeVideoEncoder() {
    return 0L;
  }
  
  @CalledByNative
  default boolean isHardwareEncoder() {
    return true;
  }
  
  @CalledByNative
  VideoCodecStatus initEncode(Settings paramSettings, Callback paramCallback);
  
  @CalledByNative
  VideoCodecStatus release();
  
  @CalledByNative
  VideoCodecStatus encode(VideoFrame paramVideoFrame, EncodeInfo paramEncodeInfo);
  
  @CalledByNative
  VideoCodecStatus setRateAllocation(BitrateAllocation paramBitrateAllocation, int paramInt);
  
  @CalledByNative
  ScalingSettings getScalingSettings();
  
  @CalledByNative
  default ResolutionBitrateLimits[] getResolutionBitrateLimits() {
    ResolutionBitrateLimits[] bitrate_limits = new ResolutionBitrateLimits[0];
    return bitrate_limits;
  }
  
  @CalledByNative
  String getImplementationName();
  
  public static interface Callback {
    void onEncodedFrame(EncodedImage param1EncodedImage, VideoEncoder.CodecSpecificInfo param1CodecSpecificInfo);
  }
}
