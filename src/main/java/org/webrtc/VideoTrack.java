package org.webrtc;

import java.util.IdentityHashMap;
import java.util.Iterator;

public class VideoTrack extends MediaStreamTrack {
  private final IdentityHashMap<VideoSink, Long> sinks = new IdentityHashMap<>();
  
  public VideoTrack(long nativeTrack) {
    super(nativeTrack);
  }
  
  public void addSink(VideoSink sink) {
    if (sink == null)
      throw new IllegalArgumentException("The VideoSink is not allowed to be null"); 
    if (!this.sinks.containsKey(sink)) {
      long nativeSink = nativeWrapSink(sink);
      this.sinks.put(sink, Long.valueOf(nativeSink));
      nativeAddSink(getNativeMediaStreamTrack(), nativeSink);
    } 
  }
  
  public void removeSink(VideoSink sink) {
    Long nativeSink = this.sinks.remove(sink);
    if (nativeSink != null) {
      nativeRemoveSink(getNativeMediaStreamTrack(), nativeSink.longValue());
      nativeFreeSink(nativeSink.longValue());
    } 
  }
  
  public void dispose() {
    for (Iterator<Long> iterator = this.sinks.values().iterator(); iterator.hasNext(); ) {
      long nativeSink = ((Long)iterator.next()).longValue();
      nativeRemoveSink(getNativeMediaStreamTrack(), nativeSink);
      nativeFreeSink(nativeSink);
    } 
    this.sinks.clear();
    super.dispose();
  }
  
  long getNativeVideoTrack() {
    return getNativeMediaStreamTrack();
  }
  
  private static native void nativeAddSink(long paramLong1, long paramLong2);
  
  private static native void nativeRemoveSink(long paramLong1, long paramLong2);
  
  private static native long nativeWrapSink(VideoSink paramVideoSink);
  
  private static native void nativeFreeSink(long paramLong);
}
