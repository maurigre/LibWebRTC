package org.webrtc;

interface BitrateAdjuster {
  void setTargets(int paramInt1, int paramInt2);
  
  void reportEncodedFrame(int paramInt);
  
  int getAdjustedBitrateBps();
  
  int getCodecConfigFramerate();
}
