package org.webrtc;

class DynamicBitrateAdjuster extends BaseBitrateAdjuster {
  private static final double BITRATE_ADJUSTMENT_SEC = 3.0D;
  
  private static final double BITRATE_ADJUSTMENT_MAX_SCALE = 4.0D;
  
  private static final int BITRATE_ADJUSTMENT_STEPS = 20;
  
  private static final double BITS_PER_BYTE = 8.0D;
  
  private double deviationBytes;
  
  private double timeSinceLastAdjustmentMs;
  
  private int bitrateAdjustmentScaleExp;
  
  public void setTargets(int targetBitrateBps, int targetFps) {
    if (this.targetBitrateBps > 0 && targetBitrateBps < this.targetBitrateBps)
      this.deviationBytes = this.deviationBytes * targetBitrateBps / this.targetBitrateBps; 
    super.setTargets(targetBitrateBps, targetFps);
  }
  
  public void reportEncodedFrame(int size) {
    if (this.targetFps == 0)
      return; 
    double expectedBytesPerFrame = this.targetBitrateBps / 8.0D / this.targetFps;
    this.deviationBytes += size - expectedBytesPerFrame;
    this.timeSinceLastAdjustmentMs += 1000.0D / this.targetFps;
    double deviationThresholdBytes = this.targetBitrateBps / 8.0D;
    double deviationCap = 3.0D * deviationThresholdBytes;
    this.deviationBytes = Math.min(this.deviationBytes, deviationCap);
    this.deviationBytes = Math.max(this.deviationBytes, -deviationCap);
    if (this.timeSinceLastAdjustmentMs <= 3000.0D)
      return; 
    if (this.deviationBytes > deviationThresholdBytes) {
      int bitrateAdjustmentInc = (int)(this.deviationBytes / deviationThresholdBytes + 0.5D);
      this.bitrateAdjustmentScaleExp -= bitrateAdjustmentInc;
      this.bitrateAdjustmentScaleExp = Math.max(this.bitrateAdjustmentScaleExp, -20);
      this.deviationBytes = deviationThresholdBytes;
    } else if (this.deviationBytes < -deviationThresholdBytes) {
      int bitrateAdjustmentInc = (int)(-this.deviationBytes / deviationThresholdBytes + 0.5D);
      this.bitrateAdjustmentScaleExp += bitrateAdjustmentInc;
      this.bitrateAdjustmentScaleExp = Math.min(this.bitrateAdjustmentScaleExp, 20);
      this.deviationBytes = -deviationThresholdBytes;
    } 
    this.timeSinceLastAdjustmentMs = 0.0D;
  }
  
  private double getBitrateAdjustmentScale() {
    return Math.pow(4.0D, this.bitrateAdjustmentScaleExp / 20.0D);
  }
  
  public int getAdjustedBitrateBps() {
    return (int)(this.targetBitrateBps * getBitrateAdjustmentScale());
  }
}
