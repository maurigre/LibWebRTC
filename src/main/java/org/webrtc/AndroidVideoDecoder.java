package org.webrtc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

class AndroidVideoDecoder implements VideoDecoder, VideoSink {
  private static final String TAG = "AndroidVideoDecoder";
  
  private static final String MEDIA_FORMAT_KEY_STRIDE = "stride";
  
  private static final String MEDIA_FORMAT_KEY_SLICE_HEIGHT = "slice-height";
  
  private static final String MEDIA_FORMAT_KEY_CROP_LEFT = "crop-left";
  
  private static final String MEDIA_FORMAT_KEY_CROP_RIGHT = "crop-right";
  
  private static final String MEDIA_FORMAT_KEY_CROP_TOP = "crop-top";
  
  private static final String MEDIA_FORMAT_KEY_CROP_BOTTOM = "crop-bottom";
  
  private static final int MEDIA_CODEC_RELEASE_TIMEOUT_MS = 5000;
  
  private static final int DEQUEUE_INPUT_TIMEOUT_US = 500000;
  
  private static final int DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = 100000;
  
  private final MediaCodecWrapperFactory mediaCodecWrapperFactory;
  
  private final String codecName;
  
  private final VideoCodecMimeType codecType;
  
  private final BlockingDeque<FrameInfo> frameInfos;
  
  private int colorFormat;
  
  @Nullable
  private Thread outputThread;
  
  private ThreadUtils.ThreadChecker outputThreadChecker;
  
  private ThreadUtils.ThreadChecker decoderThreadChecker;
  
  private volatile boolean running;
  
  @Nullable
  private volatile Exception shutdownException;
  
  private static class FrameInfo {
    final long decodeStartTimeMs;
    
    final int rotation;
    
    FrameInfo(long decodeStartTimeMs, int rotation) {
      this.decodeStartTimeMs = decodeStartTimeMs;
      this.rotation = rotation;
    }
  }
  
  private final Object dimensionLock = new Object();
  
  private int width;
  
  private int height;
  
  private int stride;
  
  private int sliceHeight;
  
  private boolean hasDecodedFirstFrame;
  
  private boolean keyFrameRequired;
  
  @Nullable
  private final EglBase.Context sharedContext;
  
  @Nullable
  private SurfaceTextureHelper surfaceTextureHelper;
  
  @Nullable
  private Surface surface;
  
  private static class DecodedTextureMetadata {
    final long presentationTimestampUs;
    
    final Integer decodeTimeMs;
    
    DecodedTextureMetadata(long presentationTimestampUs, Integer decodeTimeMs) {
      this.presentationTimestampUs = presentationTimestampUs;
      this.decodeTimeMs = decodeTimeMs;
    }
  }
  
  private final Object renderedTextureMetadataLock = new Object();
  
  @Nullable
  private DecodedTextureMetadata renderedTextureMetadata;
  
  @Nullable
  private VideoDecoder.Callback callback;
  
  @Nullable
  private MediaCodecWrapper codec;
  
  AndroidVideoDecoder(MediaCodecWrapperFactory mediaCodecWrapperFactory, String codecName, VideoCodecMimeType codecType, int colorFormat, @Nullable EglBase.Context sharedContext) {
    if (!isSupportedColorFormat(colorFormat))
      throw new IllegalArgumentException("Unsupported color format: " + colorFormat); 
    Logging.d("AndroidVideoDecoder", "ctor name: " + codecName + " type: " + codecType + " color format: " + colorFormat + " context: " + sharedContext);
    this.mediaCodecWrapperFactory = mediaCodecWrapperFactory;
    this.codecName = codecName;
    this.codecType = codecType;
    this.colorFormat = colorFormat;
    this.sharedContext = sharedContext;
    this.frameInfos = new LinkedBlockingDeque<>();
  }
  
  public VideoCodecStatus initDecode(VideoDecoder.Settings settings, VideoDecoder.Callback callback) {
    this.decoderThreadChecker = new ThreadUtils.ThreadChecker();
    this.callback = callback;
    if (this.sharedContext != null) {
      this.surfaceTextureHelper = createSurfaceTextureHelper();
      this.surface = new Surface(this.surfaceTextureHelper.getSurfaceTexture());
      this.surfaceTextureHelper.startListening(this);
    } 
    return initDecodeInternal(settings.width, settings.height);
  }
  
  private VideoCodecStatus initDecodeInternal(int width, int height) {
    this.decoderThreadChecker.checkIsOnValidThread();
    Logging.d("AndroidVideoDecoder", "initDecodeInternal name: " + this.codecName + " type: " + this.codecType + " width: " + width + " height: " + height);
    if (this.outputThread != null) {
      Logging.e("AndroidVideoDecoder", "initDecodeInternal called while the codec is already running");
      return VideoCodecStatus.FALLBACK_SOFTWARE;
    } 
    this.width = width;
    this.height = height;
    this.stride = width;
    this.sliceHeight = height;
    this.hasDecodedFirstFrame = false;
    this.keyFrameRequired = true;
    try {
      this.codec = this.mediaCodecWrapperFactory.createByCodecName(this.codecName);
    } catch (IOException|IllegalArgumentException e) {
      Logging.e("AndroidVideoDecoder", "Cannot create media decoder " + this.codecName);
      return VideoCodecStatus.FALLBACK_SOFTWARE;
    } 
    try {
      MediaFormat format = MediaFormat.createVideoFormat(this.codecType.mimeType(), width, height);
      if (this.sharedContext == null)
        format.setInteger("color-format", this.colorFormat); 
      this.codec.configure(format, this.surface, null, 0);
      this.codec.start();
    } catch (IllegalStateException e) {
      Logging.e("AndroidVideoDecoder", "initDecode failed", e);
      release();
      return VideoCodecStatus.FALLBACK_SOFTWARE;
    } 
    this.running = true;
    this.outputThread = createOutputThread();
    this.outputThread.start();
    Logging.d("AndroidVideoDecoder", "initDecodeInternal done");
    return VideoCodecStatus.OK;
  }
  
  public VideoCodecStatus decode(EncodedImage frame, VideoDecoder.DecodeInfo info) {
    int width, height, index;
    ByteBuffer buffer;
    this.decoderThreadChecker.checkIsOnValidThread();
    if (this.codec == null || this.callback == null) {
      Logging.d("AndroidVideoDecoder", "decode uninitalized, codec: " + ((this.codec != null) ? 1 : 0) + ", callback: " + this.callback);
      return VideoCodecStatus.UNINITIALIZED;
    } 
    if (frame.buffer == null) {
      Logging.e("AndroidVideoDecoder", "decode() - no input data");
      return VideoCodecStatus.ERR_PARAMETER;
    } 
    int size = frame.buffer.remaining();
    if (size == 0) {
      Logging.e("AndroidVideoDecoder", "decode() - input buffer empty");
      return VideoCodecStatus.ERR_PARAMETER;
    } 
    synchronized (this.dimensionLock) {
      width = this.width;
      height = this.height;
    } 
    if (frame.encodedWidth * frame.encodedHeight > 0 && (frame.encodedWidth != width || frame.encodedHeight != height)) {
      VideoCodecStatus status = reinitDecode(frame.encodedWidth, frame.encodedHeight);
      if (status != VideoCodecStatus.OK)
        return status; 
    } 
    if (this.keyFrameRequired) {
      if (frame.frameType != EncodedImage.FrameType.VideoFrameKey) {
        Logging.e("AndroidVideoDecoder", "decode() - key frame required first");
        return VideoCodecStatus.NO_OUTPUT;
      } 
      if (!frame.completeFrame) {
        Logging.e("AndroidVideoDecoder", "decode() - complete frame required first");
        return VideoCodecStatus.NO_OUTPUT;
      } 
    } 
    try {
      index = this.codec.dequeueInputBuffer(500000L);
    } catch (IllegalStateException e) {
      Logging.e("AndroidVideoDecoder", "dequeueInputBuffer failed", e);
      return VideoCodecStatus.ERROR;
    } 
    if (index < 0) {
      Logging.e("AndroidVideoDecoder", "decode() - no HW buffers available; decoder falling behind");
      return VideoCodecStatus.ERROR;
    } 
    try {
      buffer = this.codec.getInputBuffers()[index];
    } catch (IllegalStateException e) {
      Logging.e("AndroidVideoDecoder", "getInputBuffers failed", e);
      return VideoCodecStatus.ERROR;
    } 
    if (buffer.capacity() < size) {
      Logging.e("AndroidVideoDecoder", "decode() - HW buffer too small");
      return VideoCodecStatus.ERROR;
    } 
    buffer.put(frame.buffer);
    this.frameInfos.offer(new FrameInfo(SystemClock.elapsedRealtime(), frame.rotation));
    try {
      this.codec.queueInputBuffer(index, 0, size, TimeUnit.NANOSECONDS
          .toMicros(frame.captureTimeNs), 0);
    } catch (IllegalStateException e) {
      Logging.e("AndroidVideoDecoder", "queueInputBuffer failed", e);
      this.frameInfos.pollLast();
      return VideoCodecStatus.ERROR;
    } 
    if (this.keyFrameRequired)
      this.keyFrameRequired = false; 
    return VideoCodecStatus.OK;
  }
  
  public boolean getPrefersLateDecoding() {
    return true;
  }
  
  public String getImplementationName() {
    return this.codecName;
  }
  
  public VideoCodecStatus release() {
    Logging.d("AndroidVideoDecoder", "release");
    VideoCodecStatus status = releaseInternal();
    if (this.surface != null) {
      releaseSurface();
      this.surface = null;
      this.surfaceTextureHelper.stopListening();
      this.surfaceTextureHelper.dispose();
      this.surfaceTextureHelper = null;
    } 
    synchronized (this.renderedTextureMetadataLock) {
      this.renderedTextureMetadata = null;
    } 
    this.callback = null;
    this.frameInfos.clear();
    return status;
  }
  
  private VideoCodecStatus releaseInternal() {
    if (!this.running) {
      Logging.d("AndroidVideoDecoder", "release: Decoder is not running.");
      return VideoCodecStatus.OK;
    } 
    try {
      this.running = false;
      if (!ThreadUtils.joinUninterruptibly(this.outputThread, 5000L)) {
        Logging.e("AndroidVideoDecoder", "Media decoder release timeout", new RuntimeException());
        return VideoCodecStatus.TIMEOUT;
      } 
      if (this.shutdownException != null) {
        Logging.e("AndroidVideoDecoder", "Media decoder release error", new RuntimeException(this.shutdownException));
        this.shutdownException = null;
        return VideoCodecStatus.ERROR;
      } 
    } finally {
      this.codec = null;
      this.outputThread = null;
    } 
    return VideoCodecStatus.OK;
  }
  
  private VideoCodecStatus reinitDecode(int newWidth, int newHeight) {
    this.decoderThreadChecker.checkIsOnValidThread();
    VideoCodecStatus status = releaseInternal();
    if (status != VideoCodecStatus.OK)
      return status; 
    return initDecodeInternal(newWidth, newHeight);
  }
  
  private Thread createOutputThread() {
    return new Thread("AndroidVideoDecoder.outputThread") {
        public void run() {
          AndroidVideoDecoder.this.outputThreadChecker = new ThreadUtils.ThreadChecker();
          while (AndroidVideoDecoder.this.running)
            AndroidVideoDecoder.this.deliverDecodedFrame(); 
          AndroidVideoDecoder.this.releaseCodecOnOutputThread();
        }
      };
  }
  
  protected void deliverDecodedFrame() {
    this.outputThreadChecker.checkIsOnValidThread();
    try {
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      int result = this.codec.dequeueOutputBuffer(info, 100000L);
      if (result == -2) {
        reformat(this.codec.getOutputFormat());
        return;
      } 
      if (result < 0) {
        Logging.v("AndroidVideoDecoder", "dequeueOutputBuffer returned " + result);
        return;
      } 
      FrameInfo frameInfo = this.frameInfos.poll();
      Integer decodeTimeMs = null;
      int rotation = 0;
      if (frameInfo != null) {
        decodeTimeMs = Integer.valueOf((int)(SystemClock.elapsedRealtime() - frameInfo.decodeStartTimeMs));
        rotation = frameInfo.rotation;
      } 
      this.hasDecodedFirstFrame = true;
      if (this.surfaceTextureHelper != null) {
        deliverTextureFrame(result, info, rotation, decodeTimeMs);
      } else {
        deliverByteFrame(result, info, rotation, decodeTimeMs);
      } 
    } catch (IllegalStateException e) {
      Logging.e("AndroidVideoDecoder", "deliverDecodedFrame failed", e);
    } 
  }
  
  private void deliverTextureFrame(int index, MediaCodec.BufferInfo info, int rotation, Integer decodeTimeMs) {
    int width;
    int height;
    synchronized (this.dimensionLock) {
      width = this.width;
      height = this.height;
    } 
    synchronized (this.renderedTextureMetadataLock) {
      if (this.renderedTextureMetadata != null) {
        this.codec.releaseOutputBuffer(index, false);
        return;
      } 
      this.surfaceTextureHelper.setTextureSize(width, height);
      this.surfaceTextureHelper.setFrameRotation(rotation);
      this.renderedTextureMetadata = new DecodedTextureMetadata(info.presentationTimeUs, decodeTimeMs);
      this.codec.releaseOutputBuffer(index, true);
    } 
  }
  
  public void onFrame(VideoFrame frame) {
    Integer decodeTimeMs;
    long timestampNs;
    synchronized (this.renderedTextureMetadataLock) {
      if (this.renderedTextureMetadata == null)
        throw new IllegalStateException("Rendered texture metadata was null in onTextureFrameAvailable."); 
      timestampNs = this.renderedTextureMetadata.presentationTimestampUs * 1000L;
      decodeTimeMs = this.renderedTextureMetadata.decodeTimeMs;
      this.renderedTextureMetadata = null;
    } 
    VideoFrame frameWithModifiedTimeStamp = new VideoFrame(frame.getBuffer(), frame.getRotation(), timestampNs);
    this.callback.onDecodedFrame(frameWithModifiedTimeStamp, decodeTimeMs, null);
  }
  
  private void deliverByteFrame(int result, MediaCodec.BufferInfo info, int rotation, Integer decodeTimeMs) {
    int width, height, stride, sliceHeight;
    VideoFrame.Buffer frameBuffer;
    synchronized (this.dimensionLock) {
      width = this.width;
      height = this.height;
      stride = this.stride;
      sliceHeight = this.sliceHeight;
    } 
    if (info.size < width * height * 3 / 2) {
      Logging.e("AndroidVideoDecoder", "Insufficient output buffer size: " + info.size);
      return;
    } 
    if (info.size < stride * height * 3 / 2 && sliceHeight == height && stride > width)
      stride = info.size * 2 / height * 3; 
    ByteBuffer buffer = this.codec.getOutputBuffers()[result];
    buffer.position(info.offset);
    buffer.limit(info.offset + info.size);
    buffer = buffer.slice();
    if (this.colorFormat == 19) {
      frameBuffer = copyI420Buffer(buffer, stride, sliceHeight, width, height);
    } else {
      frameBuffer = copyNV12ToI420Buffer(buffer, stride, sliceHeight, width, height);
    } 
    this.codec.releaseOutputBuffer(result, false);
    long presentationTimeNs = info.presentationTimeUs * 1000L;
    VideoFrame frame = new VideoFrame(frameBuffer, rotation, presentationTimeNs);
    this.callback.onDecodedFrame(frame, decodeTimeMs, null);
    frame.release();
  }
  
  private VideoFrame.Buffer copyNV12ToI420Buffer(ByteBuffer buffer, int stride, int sliceHeight, int width, int height) {
    return (new NV12Buffer(width, height, stride, sliceHeight, buffer, null))
      .toI420();
  }
  
  private VideoFrame.Buffer copyI420Buffer(ByteBuffer buffer, int stride, int sliceHeight, int width, int height) {
    if (stride % 2 != 0)
      throw new AssertionError("Stride is not divisible by two: " + stride); 
    int chromaWidth = (width + 1) / 2;
    int chromaHeight = (sliceHeight % 2 == 0) ? ((height + 1) / 2) : (height / 2);
    int uvStride = stride / 2;
    int yPos = 0;
    int yEnd = 0 + stride * height;
    int uPos = 0 + stride * sliceHeight;
    int uEnd = uPos + uvStride * chromaHeight;
    int vPos = uPos + uvStride * sliceHeight / 2;
    int vEnd = vPos + uvStride * chromaHeight;
    VideoFrame.I420Buffer frameBuffer = allocateI420Buffer(width, height);
    buffer.limit(yEnd);
    buffer.position(0);
    copyPlane(buffer
        .slice(), stride, frameBuffer.getDataY(), frameBuffer.getStrideY(), width, height);
    buffer.limit(uEnd);
    buffer.position(uPos);
    copyPlane(buffer.slice(), uvStride, frameBuffer.getDataU(), frameBuffer.getStrideU(), chromaWidth, chromaHeight);
    if (sliceHeight % 2 == 1) {
      buffer.position(uPos + uvStride * (chromaHeight - 1));
      ByteBuffer dataU = frameBuffer.getDataU();
      dataU.position(frameBuffer.getStrideU() * chromaHeight);
      dataU.put(buffer);
    } 
    buffer.limit(vEnd);
    buffer.position(vPos);
    copyPlane(buffer.slice(), uvStride, frameBuffer.getDataV(), frameBuffer.getStrideV(), chromaWidth, chromaHeight);
    if (sliceHeight % 2 == 1) {
      buffer.position(vPos + uvStride * (chromaHeight - 1));
      ByteBuffer dataV = frameBuffer.getDataV();
      dataV.position(frameBuffer.getStrideV() * chromaHeight);
      dataV.put(buffer);
    } 
    return frameBuffer;
  }
  
  private void reformat(MediaFormat format) {
    int newWidth, newHeight;
    this.outputThreadChecker.checkIsOnValidThread();
    Logging.d("AndroidVideoDecoder", "Decoder format changed: " + format.toString());
    if (format.containsKey("crop-left") && format
      .containsKey("crop-right") && format
      .containsKey("crop-bottom") && format
      .containsKey("crop-top")) {
      newWidth = 1 + format.getInteger("crop-right") - format.getInteger("crop-left");
      newHeight = 1 + format.getInteger("crop-bottom") - format.getInteger("crop-top");
    } else {
      newWidth = format.getInteger("width");
      newHeight = format.getInteger("height");
    } 
    synchronized (this.dimensionLock) {
      if (this.hasDecodedFirstFrame && (this.width != newWidth || this.height != newHeight)) {
        stopOnOutputThread(new RuntimeException("Unexpected size change. Configured " + this.width + "*" + this.height + ". New " + newWidth + "*" + newHeight));
        return;
      } 
      this.width = newWidth;
      this.height = newHeight;
    } 
    if (this.surfaceTextureHelper == null && format.containsKey("color-format")) {
      this.colorFormat = format.getInteger("color-format");
      Logging.d("AndroidVideoDecoder", "Color: 0x" + Integer.toHexString(this.colorFormat));
      if (!isSupportedColorFormat(this.colorFormat)) {
        stopOnOutputThread(new IllegalStateException("Unsupported color format: " + this.colorFormat));
        return;
      } 
    } 
    synchronized (this.dimensionLock) {
      if (format.containsKey("stride"))
        this.stride = format.getInteger("stride"); 
      if (format.containsKey("slice-height"))
        this.sliceHeight = format.getInteger("slice-height"); 
      Logging.d("AndroidVideoDecoder", "Frame stride and slice height: " + this.stride + " x " + this.sliceHeight);
      this.stride = Math.max(this.width, this.stride);
      this.sliceHeight = Math.max(this.height, this.sliceHeight);
    } 
  }
  
  private void releaseCodecOnOutputThread() {
    this.outputThreadChecker.checkIsOnValidThread();
    Logging.d("AndroidVideoDecoder", "Releasing MediaCodec on output thread");
    try {
      this.codec.stop();
    } catch (Exception e) {
      Logging.e("AndroidVideoDecoder", "Media decoder stop failed", e);
    } 
    try {
      this.codec.release();
    } catch (Exception e) {
      Logging.e("AndroidVideoDecoder", "Media decoder release failed", e);
      this.shutdownException = e;
    } 
    Logging.d("AndroidVideoDecoder", "Release on output thread done");
  }
  
  private void stopOnOutputThread(Exception e) {
    this.outputThreadChecker.checkIsOnValidThread();
    this.running = false;
    this.shutdownException = e;
  }
  
  private boolean isSupportedColorFormat(int colorFormat) {
    for (int supported : MediaCodecUtils.DECODER_COLOR_FORMATS) {
      if (supported == colorFormat)
        return true; 
    } 
    return false;
  }
  
  protected SurfaceTextureHelper createSurfaceTextureHelper() {
    return SurfaceTextureHelper.create("decoder-texture-thread", this.sharedContext);
  }
  
  protected void releaseSurface() {
    this.surface.release();
  }
  
  protected VideoFrame.I420Buffer allocateI420Buffer(int width, int height) {
    return JavaI420Buffer.allocate(width, height);
  }
  
  protected void copyPlane(ByteBuffer src, int srcStride, ByteBuffer dst, int dstStride, int width, int height) {
    YuvHelper.copyPlane(src, srcStride, dst, dstStride, width, height);
  }
}
