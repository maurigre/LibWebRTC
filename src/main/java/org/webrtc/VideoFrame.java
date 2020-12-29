package org.webrtc;

import android.graphics.Matrix;
import java.nio.ByteBuffer;

public class VideoFrame implements RefCounted {
  private final Buffer buffer;
  
  private final int rotation;
  
  private final long timestampNs;
  
  public static interface TextureBuffer extends Buffer {
    Type getType();
    
    int getTextureId();
    
    Matrix getTransformMatrix();
    
    public enum Type {
      OES(36197),
      RGB(3553);
      
      private final int glTarget;
      
      Type(int glTarget) {
        this.glTarget = glTarget;
      }
      
      public int getGlTarget() {
        return this.glTarget;
      }
    }
  }
  
  @CalledByNative
  public VideoFrame(Buffer buffer, int rotation, long timestampNs) {
    if (buffer == null)
      throw new IllegalArgumentException("buffer not allowed to be null"); 
    if (rotation % 90 != 0)
      throw new IllegalArgumentException("rotation must be a multiple of 90"); 
    this.buffer = buffer;
    this.rotation = rotation;
    this.timestampNs = timestampNs;
  }
  
  @CalledByNative
  public Buffer getBuffer() {
    return this.buffer;
  }
  
  @CalledByNative
  public int getRotation() {
    return this.rotation;
  }
  
  @CalledByNative
  public long getTimestampNs() {
    return this.timestampNs;
  }
  
  public int getRotatedWidth() {
    if (this.rotation % 180 == 0)
      return this.buffer.getWidth(); 
    return this.buffer.getHeight();
  }
  
  public int getRotatedHeight() {
    if (this.rotation % 180 == 0)
      return this.buffer.getHeight(); 
    return this.buffer.getWidth();
  }
  
  public void retain() {
    this.buffer.retain();
  }
  
  @CalledByNative
  public void release() {
    this.buffer.release();
  }
  
  public static interface I420Buffer extends Buffer {
    @CalledByNative("I420Buffer")
    ByteBuffer getDataY();
    
    @CalledByNative("I420Buffer")
    ByteBuffer getDataU();
    
    @CalledByNative("I420Buffer")
    ByteBuffer getDataV();
    
    @CalledByNative("I420Buffer")
    int getStrideY();
    
    @CalledByNative("I420Buffer")
    int getStrideU();
    
    @CalledByNative("I420Buffer")
    int getStrideV();
  }
  
  public static interface Buffer extends RefCounted {
    @CalledByNative("Buffer")
    int getWidth();
    
    @CalledByNative("Buffer")
    int getHeight();
    
    @CalledByNative("Buffer")
    VideoFrame.I420Buffer toI420();
    
    @CalledByNative("Buffer")
    void retain();
    
    @CalledByNative("Buffer")
    void release();
    
    @CalledByNative("Buffer")
    Buffer cropAndScale(int param1Int1, int param1Int2, int param1Int3, int param1Int4, int param1Int5, int param1Int6);
  }
}
