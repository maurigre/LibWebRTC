package org.webrtc;

import java.util.List;

public interface CameraEnumerator {
  String[] getDeviceNames();
  
  boolean isFrontFacing(String paramString);
  
  boolean isBackFacing(String paramString);
  
  List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String paramString);
  
  CameraVideoCapturer createCapturer(String paramString, CameraVideoCapturer.CameraEventsHandler paramCameraEventsHandler);
}
