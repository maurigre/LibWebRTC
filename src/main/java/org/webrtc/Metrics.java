package org.webrtc;

import java.util.HashMap;
import java.util.Map;

public class Metrics {
  private static final String TAG = "Metrics";
  
  public final Map<String, HistogramInfo> map = new HashMap<>();
  
  public static class HistogramInfo {
    public final int min;
    
    public final int max;
    
    public final int bucketCount;
    
    public final Map<Integer, Integer> samples = new HashMap<>();
    
    @CalledByNative("HistogramInfo")
    public HistogramInfo(int min, int max, int bucketCount) {
      this.min = min;
      this.max = max;
      this.bucketCount = bucketCount;
    }
    
    @CalledByNative("HistogramInfo")
    public void addSample(int value, int numEvents) {
      this.samples.put(Integer.valueOf(value), Integer.valueOf(numEvents));
    }
  }
  
  @CalledByNative
  private void add(String name, HistogramInfo info) {
    this.map.put(name, info);
  }
  
  public static void enable() {
    nativeEnable();
  }
  
  public static Metrics getAndReset() {
    return nativeGetAndReset();
  }
  
  private static native void nativeEnable();
  
  private static native Metrics nativeGetAndReset();
}
