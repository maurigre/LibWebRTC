package org.webrtc;

import android.content.Context;
import android.graphics.Matrix;
import android.view.WindowManager;

interface CameraSession {
  void stop();
  
  public static interface Events {
    void onCameraOpening();
    
    void onCameraError(CameraSession param1CameraSession, String param1String);
    
    void onCameraDisconnected(CameraSession param1CameraSession);
    
    void onCameraClosed(CameraSession param1CameraSession);
    
    void onFrameCaptured(CameraSession param1CameraSession, VideoFrame param1VideoFrame);
  }
  
  public static interface CreateSessionCallback {
    void onDone(CameraSession param1CameraSession);
    
    void onFailure(CameraSession.FailureType param1FailureType, String param1String);
  }
  
  public enum FailureType {
    ERROR, DISCONNECTED;
  }
  
  static int getDeviceOrientation(Context context) {
    WindowManager wm = (WindowManager)context.getSystemService("window");
    switch (wm.getDefaultDisplay().getRotation()) {
      case 1:
        return 90;
      case 2:
        return 180;
      case 3:
        return 270;
    } 
    return 0;
  }
  
  static VideoFrame.TextureBuffer createTextureBufferWithModifiedTransformMatrix(TextureBufferImpl buffer, boolean mirror, int rotation) {
    Matrix transformMatrix = new Matrix();
    transformMatrix.preTranslate(0.5F, 0.5F);
    if (mirror)
      transformMatrix.preScale(-1.0F, 1.0F); 
    transformMatrix.preRotate(rotation);
    transformMatrix.preTranslate(-0.5F, -0.5F);
    return buffer.applyTransformMatrix(transformMatrix, buffer.getWidth(), buffer.getHeight());
  }
}
