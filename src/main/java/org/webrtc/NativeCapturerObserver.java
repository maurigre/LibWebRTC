package org.webrtc;

class NativeCapturerObserver implements CapturerObserver {
  private final NativeAndroidVideoTrackSource nativeAndroidVideoTrackSource;
  
  @CalledByNative
  public NativeCapturerObserver(long nativeSource) {
    this.nativeAndroidVideoTrackSource = new NativeAndroidVideoTrackSource(nativeSource);
  }
  
  public void onCapturerStarted(boolean success) {
    this.nativeAndroidVideoTrackSource.setState(success);
  }
  
  public void onCapturerStopped() {
    this.nativeAndroidVideoTrackSource.setState(false);
  }
  
  public void onFrameCaptured(VideoFrame frame) {
    VideoProcessor.FrameAdaptationParameters parameters = this.nativeAndroidVideoTrackSource.adaptFrame(frame);
    if (parameters == null)
      return; 
    VideoFrame.Buffer adaptedBuffer = frame.getBuffer().cropAndScale(parameters.cropX, parameters.cropY, parameters.cropWidth, parameters.cropHeight, parameters.scaleWidth, parameters.scaleHeight);
    this.nativeAndroidVideoTrackSource.onFrameCaptured(new VideoFrame(adaptedBuffer, frame
          .getRotation(), parameters.timestampNs));
    adaptedBuffer.release();
  }
}
