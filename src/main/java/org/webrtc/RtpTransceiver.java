package org.webrtc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RtpTransceiver {
  private long nativeRtpTransceiver;
  
  private RtpSender cachedSender;
  
  private RtpReceiver cachedReceiver;
  
  public enum RtpTransceiverDirection {
    SEND_RECV(0),
    SEND_ONLY(1),
    RECV_ONLY(2),
    INACTIVE(3);
    
    private final int nativeIndex;
    
    RtpTransceiverDirection(int nativeIndex) {
      this.nativeIndex = nativeIndex;
    }
    
    @CalledByNative("RtpTransceiverDirection")
    int getNativeIndex() {
      return this.nativeIndex;
    }
    
    @CalledByNative("RtpTransceiverDirection")
    static RtpTransceiverDirection fromNativeIndex(int nativeIndex) {
      for (RtpTransceiverDirection type : values()) {
        if (type.getNativeIndex() == nativeIndex)
          return type; 
      } 
      throw new IllegalArgumentException("Uknown native RtpTransceiverDirection type" + nativeIndex);
    }
  }
  
  public static final class RtpTransceiverInit {
    private final RtpTransceiver.RtpTransceiverDirection direction;
    
    private final List<String> streamIds;
    
    private final List<RtpParameters.Encoding> sendEncodings;
    
    public RtpTransceiverInit() {
      this(RtpTransceiver.RtpTransceiverDirection.SEND_RECV);
    }
    
    public RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection direction) {
      this(direction, Collections.emptyList(), Collections.emptyList());
    }
    
    public RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection direction, List<String> streamIds) {
      this(direction, streamIds, Collections.emptyList());
    }
    
    public RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection direction, List<String> streamIds, List<RtpParameters.Encoding> sendEncodings) {
      this.direction = direction;
      this.streamIds = new ArrayList<>(streamIds);
      this.sendEncodings = new ArrayList<>(sendEncodings);
    }
    
    @CalledByNative("RtpTransceiverInit")
    int getDirectionNativeIndex() {
      return this.direction.getNativeIndex();
    }
    
    @CalledByNative("RtpTransceiverInit")
    List<String> getStreamIds() {
      return new ArrayList<>(this.streamIds);
    }
    
    @CalledByNative("RtpTransceiverInit")
    List<RtpParameters.Encoding> getSendEncodings() {
      return new ArrayList<>(this.sendEncodings);
    }
  }
  
  @CalledByNative
  protected RtpTransceiver(long nativeRtpTransceiver) {
    this.nativeRtpTransceiver = nativeRtpTransceiver;
    this.cachedSender = nativeGetSender(nativeRtpTransceiver);
    this.cachedReceiver = nativeGetReceiver(nativeRtpTransceiver);
  }
  
  public MediaStreamTrack.MediaType getMediaType() {
    checkRtpTransceiverExists();
    return nativeGetMediaType(this.nativeRtpTransceiver);
  }
  
  public String getMid() {
    checkRtpTransceiverExists();
    return nativeGetMid(this.nativeRtpTransceiver);
  }
  
  public RtpSender getSender() {
    return this.cachedSender;
  }
  
  public RtpReceiver getReceiver() {
    return this.cachedReceiver;
  }
  
  public boolean isStopped() {
    checkRtpTransceiverExists();
    return nativeStopped(this.nativeRtpTransceiver);
  }
  
  public RtpTransceiverDirection getDirection() {
    checkRtpTransceiverExists();
    return nativeDirection(this.nativeRtpTransceiver);
  }
  
  public RtpTransceiverDirection getCurrentDirection() {
    checkRtpTransceiverExists();
    return nativeCurrentDirection(this.nativeRtpTransceiver);
  }
  
  public void setDirection(RtpTransceiverDirection rtpTransceiverDirection) {
    checkRtpTransceiverExists();
    nativeSetDirection(this.nativeRtpTransceiver, rtpTransceiverDirection);
  }
  
  public void stop() {
    checkRtpTransceiverExists();
    nativeStop(this.nativeRtpTransceiver);
  }
  
  @CalledByNative
  public void dispose() {
    checkRtpTransceiverExists();
    this.cachedSender.dispose();
    this.cachedReceiver.dispose();
    JniCommon.nativeReleaseRef(this.nativeRtpTransceiver);
    this.nativeRtpTransceiver = 0L;
  }
  
  private void checkRtpTransceiverExists() {
    if (this.nativeRtpTransceiver == 0L)
      throw new IllegalStateException("RtpTransceiver has been disposed."); 
  }
  
  private static native MediaStreamTrack.MediaType nativeGetMediaType(long paramLong);
  
  private static native String nativeGetMid(long paramLong);
  
  private static native RtpSender nativeGetSender(long paramLong);
  
  private static native RtpReceiver nativeGetReceiver(long paramLong);
  
  private static native boolean nativeStopped(long paramLong);
  
  private static native RtpTransceiverDirection nativeDirection(long paramLong);
  
  private static native RtpTransceiverDirection nativeCurrentDirection(long paramLong);
  
  private static native void nativeStop(long paramLong);
  
  private static native void nativeSetDirection(long paramLong, RtpTransceiverDirection paramRtpTransceiverDirection);
}
