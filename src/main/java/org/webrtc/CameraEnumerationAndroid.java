package org.webrtc;

import android.graphics.ImageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraEnumerationAndroid {
  private static final String TAG = "CameraEnumerationAndroid";
  
  static final ArrayList<Size> COMMON_RESOLUTIONS = new ArrayList<>(Arrays.asList(new Size[] { 
          new Size(160, 120), new Size(240, 160), new Size(320, 240), new Size(400, 240), new Size(480, 320), new Size(640, 360), new Size(640, 480), new Size(768, 480), new Size(854, 480), new Size(800, 600), 
          new Size(960, 540), new Size(960, 640), new Size(1024, 576), new Size(1024, 600), new Size(1280, 720), new Size(1280, 1024), new Size(1920, 1080), new Size(1920, 1440), new Size(2560, 1440), new Size(3840, 2160) }));
  
  public static class CaptureFormat {
    public final int width;
    
    public final int height;
    
    public final FramerateRange framerate;
    
    public static class FramerateRange {
      public int min;
      
      public int max;
      
      public FramerateRange(int min, int max) {
        this.min = min;
        this.max = max;
      }
      
      public String toString() {
        return "[" + (this.min / 1000.0F) + ":" + (this.max / 1000.0F) + "]";
      }
      
      public boolean equals(Object other) {
        if (!(other instanceof FramerateRange))
          return false; 
        FramerateRange otherFramerate = (FramerateRange)other;
        return (this.min == otherFramerate.min && this.max == otherFramerate.max);
      }
      
      public int hashCode() {
        return 1 + 65537 * this.min + this.max;
      }
    }
    
    public final int imageFormat = 17;
    
    public CaptureFormat(int width, int height, int minFramerate, int maxFramerate) {
      this.width = width;
      this.height = height;
      this.framerate = new FramerateRange(minFramerate, maxFramerate);
    }
    
    public CaptureFormat(int width, int height, FramerateRange framerate) {
      this.width = width;
      this.height = height;
      this.framerate = framerate;
    }
    
    public int frameSize() {
      return frameSize(this.width, this.height, 17);
    }
    
    public static int frameSize(int width, int height, int imageFormat) {
      if (imageFormat != 17)
        throw new UnsupportedOperationException("Don't know how to calculate the frame size of non-NV21 image formats."); 
      return width * height * ImageFormat.getBitsPerPixel(imageFormat) / 8;
    }
    
    public String toString() {
      return this.width + "x" + this.height + "@" + this.framerate;
    }
    
    public boolean equals(Object other) {
      if (!(other instanceof CaptureFormat))
        return false; 
      CaptureFormat otherFormat = (CaptureFormat)other;
      return (this.width == otherFormat.width && this.height == otherFormat.height && this.framerate
        .equals(otherFormat.framerate));
    }
    
    public int hashCode() {
      return 1 + (this.width * 65497 + this.height) * 251 + this.framerate.hashCode();
    }
  }
  
  public static class FramerateRange {
    public int min;
    
    public int max;
    
    public FramerateRange(int min, int max) {
      this.min = min;
      this.max = max;
    }
    
    public String toString() {
      return "[" + (this.min / 1000.0F) + ":" + (this.max / 1000.0F) + "]";
    }
    
    public boolean equals(Object other) {
      if (!(other instanceof FramerateRange))
        return false; 
      FramerateRange otherFramerate = (FramerateRange)other;
      return (this.min == otherFramerate.min && this.max == otherFramerate.max);
    }
    
    public int hashCode() {
      return 1 + 65537 * this.min + this.max;
    }
  }
  
  private static abstract class ClosestComparator<T> implements Comparator<T> {
    private ClosestComparator() {}
    
    public int compare(T t1, T t2) {
      return diff(t1) - diff(t2);
    }
    
    abstract int diff(T param1T);
  }
  
  public static CaptureFormat.FramerateRange getClosestSupportedFramerateRange(List<CaptureFormat.FramerateRange> supportedFramerates, final int requestedFps) {
    return Collections.<CaptureFormat.FramerateRange>min(supportedFramerates, new ClosestComparator<CaptureFormat.FramerateRange>() {
          private static final int MAX_FPS_DIFF_THRESHOLD = 5000;
          
          private static final int MAX_FPS_LOW_DIFF_WEIGHT = 1;
          
          private static final int MAX_FPS_HIGH_DIFF_WEIGHT = 3;
          
          private static final int MIN_FPS_THRESHOLD = 8000;
          
          private static final int MIN_FPS_LOW_VALUE_WEIGHT = 1;
          
          private static final int MIN_FPS_HIGH_VALUE_WEIGHT = 4;
          
          private int progressivePenalty(int value, int threshold, int lowWeight, int highWeight) {
            return (value < threshold) ? (value * lowWeight) : (
              threshold * lowWeight + (value - threshold) * highWeight);
          }
          
          int diff(CameraEnumerationAndroid.CaptureFormat.FramerateRange range) {
            int minFpsError = progressivePenalty(range.min, 8000, 1, 4);
            int maxFpsError = progressivePenalty(Math.abs(requestedFps * 1000 - range.max), 5000, 1, 3);
            return minFpsError + maxFpsError;
          }
        });
  }
  
  public static Size getClosestSupportedSize(List<Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
    return Collections.<Size>min(supportedSizes, new ClosestComparator<Size>() {
          int diff(Size size) {
            return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
          }
        });
  }
  
  static void reportCameraResolution(Histogram histogram, Size resolution) {
    int index = COMMON_RESOLUTIONS.indexOf(resolution);
    histogram.addSample(index + 1);
  }
}
