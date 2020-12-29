package org.webrtc;

import android.media.MediaRecorder;

public interface CameraVideoCapturer extends VideoCapturer {
  void switchCamera(CameraSwitchHandler paramCameraSwitchHandler);
  
  void switchCamera(CameraSwitchHandler paramCameraSwitchHandler, String paramString);
  
  @Deprecated
  default void addMediaRecorderToCamera(MediaRecorder mediaRecorder, MediaRecorderHandler resultHandler) {
    throw new UnsupportedOperationException("Deprecated and not implemented.");
  }
  
  @Deprecated
  default void removeMediaRecorderFromCamera(MediaRecorderHandler resultHandler) {
    throw new UnsupportedOperationException("Deprecated and not implemented.");
  }
  
  public static class CameraStatistics {
    private static final String TAG = "CameraStatistics";
    
    private static final int CAMERA_OBSERVER_PERIOD_MS = 2000;
    
    private static final int CAMERA_FREEZE_REPORT_TIMOUT_MS = 4000;
    
    private final SurfaceTextureHelper surfaceTextureHelper;
    
    private final CameraVideoCapturer.CameraEventsHandler eventsHandler;
    
    private int frameCount;
    
    private int freezePeriodCount;
    
    private final Runnable cameraObserver = new Runnable() {
        public void run() {
          int cameraFps = Math.round(CameraVideoCapturer.CameraStatistics.this.frameCount * 1000.0F / 2000.0F);
          Logging.d("CameraStatistics", "Camera fps: " + cameraFps + ".");
          if (CameraVideoCapturer.CameraStatistics.this.frameCount == 0) {
            ++CameraVideoCapturer.CameraStatistics.this.freezePeriodCount;
            if (2000 * CameraVideoCapturer.CameraStatistics.this.freezePeriodCount >= 4000 && CameraVideoCapturer.CameraStatistics.this
              .eventsHandler != null) {
              Logging.e("CameraStatistics", "Camera freezed.");
              if (CameraVideoCapturer.CameraStatistics.this.surfaceTextureHelper.isTextureInUse()) {
                CameraVideoCapturer.CameraStatistics.this.eventsHandler.onCameraFreezed("Camera failure. Client must return video buffers.");
              } else {
                CameraVideoCapturer.CameraStatistics.this.eventsHandler.onCameraFreezed("Camera failure.");
              } 
              return;
            } 
          } else {
            CameraVideoCapturer.CameraStatistics.this.freezePeriodCount = 0;
          } 
          CameraVideoCapturer.CameraStatistics.this.frameCount = 0;
          CameraVideoCapturer.CameraStatistics.this.surfaceTextureHelper.getHandler().postDelayed(this, 2000L);
        }
      };
    
    public CameraStatistics(SurfaceTextureHelper surfaceTextureHelper, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
      if (surfaceTextureHelper == null)
        throw new IllegalArgumentException("SurfaceTextureHelper is null"); 
      this.surfaceTextureHelper = surfaceTextureHelper;
      this.eventsHandler = eventsHandler;
      this.frameCount = 0;
      this.freezePeriodCount = 0;
      surfaceTextureHelper.getHandler().postDelayed(this.cameraObserver, 2000L);
    }
    
    private void checkThread() {
      if (Thread.currentThread() != this.surfaceTextureHelper.getHandler().getLooper().getThread())
        throw new IllegalStateException("Wrong thread"); 
    }
    
    public void addFrame() {
      checkThread();
      this.frameCount++;
    }
    
    public void release() {
      this.surfaceTextureHelper.getHandler().removeCallbacks(this.cameraObserver);
    }
  }
  
  @Deprecated
  public static interface MediaRecorderHandler {
    void onMediaRecorderSuccess();
    
    void onMediaRecorderError(String param1String);
  }
  
  public static interface CameraSwitchHandler {
    void onCameraSwitchDone(boolean param1Boolean);
    
    void onCameraSwitchError(String param1String);
  }
  
  public static interface CameraEventsHandler {
    void onCameraError(String param1String);
    
    void onCameraDisconnected();
    
    void onCameraFreezed(String param1String);
    
    void onCameraOpening(String param1String);
    
    void onFirstFrameAvailable();
    
    void onCameraClosed();
  }
}
