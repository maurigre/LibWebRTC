package org.webrtc;

import java.nio.ByteBuffer;

public class DataChannel {
  private long nativeDataChannel;
  
  private long nativeObserver;
  
  public static class Init {
    public boolean ordered = true;
    
    public int maxRetransmitTimeMs = -1;
    
    public int maxRetransmits = -1;
    
    public String protocol = "";
    
    public boolean negotiated;
    
    public int id = -1;
    
    @CalledByNative("Init")
    boolean getOrdered() {
      return this.ordered;
    }
    
    @CalledByNative("Init")
    int getMaxRetransmitTimeMs() {
      return this.maxRetransmitTimeMs;
    }
    
    @CalledByNative("Init")
    int getMaxRetransmits() {
      return this.maxRetransmits;
    }
    
    @CalledByNative("Init")
    String getProtocol() {
      return this.protocol;
    }
    
    @CalledByNative("Init")
    boolean getNegotiated() {
      return this.negotiated;
    }
    
    @CalledByNative("Init")
    int getId() {
      return this.id;
    }
  }
  
  public static class Buffer {
    public final ByteBuffer data;
    
    public final boolean binary;
    
    @CalledByNative("Buffer")
    public Buffer(ByteBuffer data, boolean binary) {
      this.data = data;
      this.binary = binary;
    }
  }
  
  public static interface Observer {
    @CalledByNative("Observer")
    void onBufferedAmountChange(long param1Long);
    
    @CalledByNative("Observer")
    void onStateChange();
    
    @CalledByNative("Observer")
    void onMessage(DataChannel.Buffer param1Buffer);
  }
  
  public enum State {
    CONNECTING, OPEN, CLOSING, CLOSED;
    
    @CalledByNative("State")
    static State fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  @CalledByNative
  public DataChannel(long nativeDataChannel) {
    this.nativeDataChannel = nativeDataChannel;
  }
  
  public void registerObserver(Observer observer) {
    checkDataChannelExists();
    if (this.nativeObserver != 0L)
      nativeUnregisterObserver(this.nativeObserver); 
    this.nativeObserver = nativeRegisterObserver(observer);
  }
  
  public void unregisterObserver() {
    checkDataChannelExists();
    nativeUnregisterObserver(this.nativeObserver);
  }
  
  public String label() {
    checkDataChannelExists();
    return nativeLabel();
  }
  
  public int id() {
    checkDataChannelExists();
    return nativeId();
  }
  
  public State state() {
    checkDataChannelExists();
    return nativeState();
  }
  
  public long bufferedAmount() {
    checkDataChannelExists();
    return nativeBufferedAmount();
  }
  
  public void close() {
    checkDataChannelExists();
    nativeClose();
  }
  
  public boolean send(Buffer buffer) {
    checkDataChannelExists();
    byte[] data = new byte[buffer.data.remaining()];
    buffer.data.get(data);
    return nativeSend(data, buffer.binary);
  }
  
  public void dispose() {
    checkDataChannelExists();
    JniCommon.nativeReleaseRef(this.nativeDataChannel);
    this.nativeDataChannel = 0L;
  }
  
  @CalledByNative
  long getNativeDataChannel() {
    return this.nativeDataChannel;
  }
  
  private void checkDataChannelExists() {
    if (this.nativeDataChannel == 0L)
      throw new IllegalStateException("DataChannel has been disposed."); 
  }
  
  private native long nativeRegisterObserver(Observer paramObserver);
  
  private native void nativeUnregisterObserver(long paramLong);
  
  private native String nativeLabel();
  
  private native int nativeId();
  
  private native State nativeState();
  
  private native long nativeBufferedAmount();
  
  private native void nativeClose();
  
  private native boolean nativeSend(byte[] paramArrayOfbyte, boolean paramBoolean);
}
