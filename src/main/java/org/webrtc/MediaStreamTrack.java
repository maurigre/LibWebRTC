package org.webrtc;

import android.support.annotation.Nullable;

public class MediaStreamTrack {
  public static final String AUDIO_TRACK_KIND = "audio";
  
  public static final String VIDEO_TRACK_KIND = "video";
  
  private long nativeTrack;
  
  public enum State {
    LIVE, ENDED;
    
    @CalledByNative("State")
    static State fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public enum MediaType {
    MEDIA_TYPE_AUDIO(0),
    MEDIA_TYPE_VIDEO(1);
    
    private final int nativeIndex;
    
    MediaType(int nativeIndex) {
      this.nativeIndex = nativeIndex;
    }
    
    @CalledByNative("MediaType")
    int getNative() {
      return this.nativeIndex;
    }
    
    @CalledByNative("MediaType")
    static MediaType fromNativeIndex(int nativeIndex) {
      for (MediaType type : values()) {
        if (type.getNative() == nativeIndex)
          return type; 
      } 
      throw new IllegalArgumentException("Unknown native media type: " + nativeIndex);
    }
  }
  
  @Nullable
  static MediaStreamTrack createMediaStreamTrack(long nativeTrack) {
    if (nativeTrack == 0L)
      return null; 
    String trackKind = nativeGetKind(nativeTrack);
    if (trackKind.equals("audio"))
      return new AudioTrack(nativeTrack); 
    if (trackKind.equals("video"))
      return new VideoTrack(nativeTrack); 
    return null;
  }
  
  public MediaStreamTrack(long nativeTrack) {
    if (nativeTrack == 0L)
      throw new IllegalArgumentException("nativeTrack may not be null"); 
    this.nativeTrack = nativeTrack;
  }
  
  public String id() {
    checkMediaStreamTrackExists();
    return nativeGetId(this.nativeTrack);
  }
  
  public String kind() {
    checkMediaStreamTrackExists();
    return nativeGetKind(this.nativeTrack);
  }
  
  public boolean enabled() {
    checkMediaStreamTrackExists();
    return nativeGetEnabled(this.nativeTrack);
  }
  
  public boolean setEnabled(boolean enable) {
    checkMediaStreamTrackExists();
    return nativeSetEnabled(this.nativeTrack, enable);
  }
  
  public State state() {
    checkMediaStreamTrackExists();
    return nativeGetState(this.nativeTrack);
  }
  
  public void dispose() {
    checkMediaStreamTrackExists();
    JniCommon.nativeReleaseRef(this.nativeTrack);
    this.nativeTrack = 0L;
  }
  
  long getNativeMediaStreamTrack() {
    checkMediaStreamTrackExists();
    return this.nativeTrack;
  }
  
  private void checkMediaStreamTrackExists() {
    if (this.nativeTrack == 0L)
      throw new IllegalStateException("MediaStreamTrack has been disposed."); 
  }
  
  private static native String nativeGetId(long paramLong);
  
  private static native String nativeGetKind(long paramLong);
  
  private static native boolean nativeGetEnabled(long paramLong);
  
  private static native boolean nativeSetEnabled(long paramLong, boolean paramBoolean);
  
  private static native State nativeGetState(long paramLong);
}
