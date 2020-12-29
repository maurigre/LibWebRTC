package org.webrtc;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;
import java.nio.ByteBuffer;

interface MediaCodecWrapper {
  void configure(MediaFormat paramMediaFormat, Surface paramSurface, MediaCrypto paramMediaCrypto, int paramInt);
  
  void start();
  
  void flush();
  
  void stop();
  
  void release();
  
  int dequeueInputBuffer(long paramLong);
  
  void queueInputBuffer(int paramInt1, int paramInt2, int paramInt3, long paramLong, int paramInt4);
  
  int dequeueOutputBuffer(MediaCodec.BufferInfo paramBufferInfo, long paramLong);
  
  void releaseOutputBuffer(int paramInt, boolean paramBoolean);
  
  MediaFormat getOutputFormat();
  
  ByteBuffer[] getInputBuffers();
  
  ByteBuffer[] getOutputBuffers();
  
  Surface createInputSurface();
  
  void setParameters(Bundle paramBundle);
}
