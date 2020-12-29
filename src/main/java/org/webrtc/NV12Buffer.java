package org.webrtc;

import android.support.annotation.Nullable;
import java.nio.ByteBuffer;

public class NV12Buffer implements VideoFrame.Buffer {
  private final int width;
  
  private final int height;
  
  private final int stride;
  
  private final int sliceHeight;
  
  private final ByteBuffer buffer;
  
  private final RefCountDelegate refCountDelegate;
  
  public NV12Buffer(int width, int height, int stride, int sliceHeight, ByteBuffer buffer, @Nullable Runnable releaseCallback) {
    this.width = width;
    this.height = height;
    this.stride = stride;
    this.sliceHeight = sliceHeight;
    this.buffer = buffer;
    this.refCountDelegate = new RefCountDelegate(releaseCallback);
  }
  
  public int getWidth() {
    return this.width;
  }
  
  public int getHeight() {
    return this.height;
  }
  
  public VideoFrame.I420Buffer toI420() {
    return (VideoFrame.I420Buffer)cropAndScale(0, 0, this.width, this.height, this.width, this.height);
  }
  
  public void retain() {
    this.refCountDelegate.retain();
  }
  
  public void release() {
    this.refCountDelegate.release();
  }
  
  public VideoFrame.Buffer cropAndScale(int cropX, int cropY, int cropWidth, int cropHeight, int scaleWidth, int scaleHeight) {
    JavaI420Buffer newBuffer = JavaI420Buffer.allocate(scaleWidth, scaleHeight);
    nativeCropAndScale(cropX, cropY, cropWidth, cropHeight, scaleWidth, scaleHeight, this.buffer, this.width, this.height, this.stride, this.sliceHeight, newBuffer
        .getDataY(), newBuffer.getStrideY(), newBuffer
        .getDataU(), newBuffer.getStrideU(), newBuffer.getDataV(), newBuffer.getStrideV());
    return newBuffer;
  }
  
  private static native void nativeCropAndScale(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, ByteBuffer paramByteBuffer1, int paramInt7, int paramInt8, int paramInt9, int paramInt10, ByteBuffer paramByteBuffer2, int paramInt11, ByteBuffer paramByteBuffer3, int paramInt12, ByteBuffer paramByteBuffer4, int paramInt13);
}
