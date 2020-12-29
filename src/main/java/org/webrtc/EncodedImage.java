package org.webrtc;

import android.support.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class EncodedImage implements RefCounted {
  private final RefCountDelegate refCountDelegate;
  
  public final ByteBuffer buffer;
  
  public final int encodedWidth;
  
  public final int encodedHeight;
  
  public final long captureTimeMs;
  
  public final long captureTimeNs;
  
  public final FrameType frameType;
  
  public final int rotation;
  
  public final boolean completeFrame;
  
  @Nullable
  public final Integer qp;
  
  public enum FrameType {
    EmptyFrame(0),
    VideoFrameKey(3),
    VideoFrameDelta(4);
    
    private final int nativeIndex;
    
    FrameType(int nativeIndex) {
      this.nativeIndex = nativeIndex;
    }
    
    public int getNative() {
      return this.nativeIndex;
    }
    
    @CalledByNative("FrameType")
    static FrameType fromNativeIndex(int nativeIndex) {
      for (FrameType type : values()) {
        if (type.getNative() == nativeIndex)
          return type; 
      } 
      throw new IllegalArgumentException("Unknown native frame type: " + nativeIndex);
    }
  }
  
  public void retain() {
    this.refCountDelegate.retain();
  }
  
  public void release() {
    this.refCountDelegate.release();
  }
  
  @CalledByNative
  private EncodedImage(ByteBuffer buffer, @Nullable Runnable releaseCallback, int encodedWidth, int encodedHeight, long captureTimeNs, FrameType frameType, int rotation, boolean completeFrame, @Nullable Integer qp) {
    this.buffer = buffer;
    this.encodedWidth = encodedWidth;
    this.encodedHeight = encodedHeight;
    this.captureTimeMs = TimeUnit.NANOSECONDS.toMillis(captureTimeNs);
    this.captureTimeNs = captureTimeNs;
    this.frameType = frameType;
    this.rotation = rotation;
    this.completeFrame = completeFrame;
    this.qp = qp;
    this.refCountDelegate = new RefCountDelegate(releaseCallback);
  }
  
  @CalledByNative
  private ByteBuffer getBuffer() {
    return this.buffer;
  }
  
  @CalledByNative
  private int getEncodedWidth() {
    return this.encodedWidth;
  }
  
  @CalledByNative
  private int getEncodedHeight() {
    return this.encodedHeight;
  }
  
  @CalledByNative
  private long getCaptureTimeNs() {
    return this.captureTimeNs;
  }
  
  @CalledByNative
  private int getFrameType() {
    return this.frameType.getNative();
  }
  
  @CalledByNative
  private int getRotation() {
    return this.rotation;
  }
  
  @CalledByNative
  private boolean getCompleteFrame() {
    return this.completeFrame;
  }
  
  @CalledByNative
  @Nullable
  private Integer getQp() {
    return this.qp;
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static class Builder {
    private ByteBuffer buffer;
    
    @Nullable
    private Runnable releaseCallback;
    
    private int encodedWidth;
    
    private int encodedHeight;
    
    private long captureTimeNs;
    
    private EncodedImage.FrameType frameType;
    
    private int rotation;
    
    private boolean completeFrame;
    
    @Nullable
    private Integer qp;
    
    private Builder() {}
    
    public Builder setBuffer(ByteBuffer buffer, @Nullable Runnable releaseCallback) {
      this.buffer = buffer;
      this.releaseCallback = releaseCallback;
      return this;
    }
    
    public Builder setEncodedWidth(int encodedWidth) {
      this.encodedWidth = encodedWidth;
      return this;
    }
    
    public Builder setEncodedHeight(int encodedHeight) {
      this.encodedHeight = encodedHeight;
      return this;
    }
    
    @Deprecated
    public Builder setCaptureTimeMs(long captureTimeMs) {
      this.captureTimeNs = TimeUnit.MILLISECONDS.toNanos(captureTimeMs);
      return this;
    }
    
    public Builder setCaptureTimeNs(long captureTimeNs) {
      this.captureTimeNs = captureTimeNs;
      return this;
    }
    
    public Builder setFrameType(EncodedImage.FrameType frameType) {
      this.frameType = frameType;
      return this;
    }
    
    public Builder setRotation(int rotation) {
      this.rotation = rotation;
      return this;
    }
    
    public Builder setCompleteFrame(boolean completeFrame) {
      this.completeFrame = completeFrame;
      return this;
    }
    
    public Builder setQp(@Nullable Integer qp) {
      this.qp = qp;
      return this;
    }
    
    public EncodedImage createEncodedImage() {
      return new EncodedImage(this.buffer, this.releaseCallback, this.encodedWidth, this.encodedHeight, this.captureTimeNs, this.frameType, this.rotation, this.completeFrame, this.qp);
    }
  }
}
