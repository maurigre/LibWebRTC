package org.webrtc;

class BaseBitrateAdjuster implements BitrateAdjuster {
  protected int targetBitrateBps;
  
  protected int targetFps;
  
  public void setTargets(int targetBitrateBps, int targetFps) {
    this.targetBitrateBps = targetBitrateBps;
    this.targetFps = targetFps;
  }
  
  public void reportEncodedFrame(int size) {}
  
  public int getAdjustedBitrateBps() {
    return this.targetBitrateBps;
  }
  
  public int getCodecConfigFramerate() {
    return this.targetFps;
  }
}
