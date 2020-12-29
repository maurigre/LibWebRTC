package org.webrtc.voiceengine;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.support.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import org.webrtc.Logging;

public class WebRtcAudioEffects {
  private static final boolean DEBUG = false;
  
  private static final String TAG = "WebRtcAudioEffects";
  
  private static final UUID AOSP_ACOUSTIC_ECHO_CANCELER = UUID.fromString("bb392ec0-8d4d-11e0-a896-0002a5d5c51b");
  
  private static final UUID AOSP_NOISE_SUPPRESSOR = UUID.fromString("c06c8400-8e06-11e0-9cb6-0002a5d5c51b");
  
  @Nullable
  private static AudioEffect.Descriptor[] cachedEffects;
  
  @Nullable
  private AcousticEchoCanceler aec;
  
  @Nullable
  private NoiseSuppressor ns;
  
  private boolean shouldEnableAec;
  
  private boolean shouldEnableNs;
  
  public static boolean isAcousticEchoCancelerSupported() {
    return isAcousticEchoCancelerEffectAvailable();
  }
  
  public static boolean isNoiseSuppressorSupported() {
    return isNoiseSuppressorEffectAvailable();
  }
  
  public static boolean isAcousticEchoCancelerBlacklisted() {
    List<String> blackListedModels = WebRtcAudioUtils.getBlackListedModelsForAecUsage();
    boolean isBlacklisted = blackListedModels.contains(Build.MODEL);
    if (isBlacklisted)
      Logging.w("WebRtcAudioEffects", Build.MODEL + " is blacklisted for HW AEC usage!"); 
    return isBlacklisted;
  }
  
  public static boolean isNoiseSuppressorBlacklisted() {
    List<String> blackListedModels = WebRtcAudioUtils.getBlackListedModelsForNsUsage();
    boolean isBlacklisted = blackListedModels.contains(Build.MODEL);
    if (isBlacklisted)
      Logging.w("WebRtcAudioEffects", Build.MODEL + " is blacklisted for HW NS usage!"); 
    return isBlacklisted;
  }
  
  private static boolean isAcousticEchoCancelerExcludedByUUID() {
    if (Build.VERSION.SDK_INT < 18)
      return false; 
    for (AudioEffect.Descriptor d : getAvailableEffects()) {
      if (d.type.equals(AudioEffect.EFFECT_TYPE_AEC) && d.uuid
        .equals(AOSP_ACOUSTIC_ECHO_CANCELER))
        return true; 
    } 
    return false;
  }
  
  private static boolean isNoiseSuppressorExcludedByUUID() {
    if (Build.VERSION.SDK_INT < 18)
      return false; 
    for (AudioEffect.Descriptor d : getAvailableEffects()) {
      if (d.type.equals(AudioEffect.EFFECT_TYPE_NS) && d.uuid.equals(AOSP_NOISE_SUPPRESSOR))
        return true; 
    } 
    return false;
  }
  
  private static boolean isAcousticEchoCancelerEffectAvailable() {
    if (Build.VERSION.SDK_INT < 18)
      return false; 
    return isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AEC);
  }
  
  private static boolean isNoiseSuppressorEffectAvailable() {
    if (Build.VERSION.SDK_INT < 18)
      return false; 
    return isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_NS);
  }
  
  public static boolean canUseAcousticEchoCanceler() {
    boolean canUseAcousticEchoCanceler = (isAcousticEchoCancelerSupported() && !WebRtcAudioUtils.useWebRtcBasedAcousticEchoCanceler() && !isAcousticEchoCancelerBlacklisted() && !isAcousticEchoCancelerExcludedByUUID());
    Logging.d("WebRtcAudioEffects", "canUseAcousticEchoCanceler: " + canUseAcousticEchoCanceler);
    return canUseAcousticEchoCanceler;
  }
  
  public static boolean canUseNoiseSuppressor() {
    boolean canUseNoiseSuppressor = (isNoiseSuppressorSupported() && !WebRtcAudioUtils.useWebRtcBasedNoiseSuppressor() && !isNoiseSuppressorBlacklisted() && !isNoiseSuppressorExcludedByUUID());
    Logging.d("WebRtcAudioEffects", "canUseNoiseSuppressor: " + canUseNoiseSuppressor);
    return canUseNoiseSuppressor;
  }
  
  public static WebRtcAudioEffects create() {
    return new WebRtcAudioEffects();
  }
  
  private WebRtcAudioEffects() {
    Logging.d("WebRtcAudioEffects", "ctor" + WebRtcAudioUtils.getThreadInfo());
  }
  
  public boolean setAEC(boolean enable) {
    Logging.d("WebRtcAudioEffects", "setAEC(" + enable + ")");
    if (!canUseAcousticEchoCanceler()) {
      Logging.w("WebRtcAudioEffects", "Platform AEC is not supported");
      this.shouldEnableAec = false;
      return false;
    } 
    if (this.aec != null && enable != this.shouldEnableAec) {
      Logging.e("WebRtcAudioEffects", "Platform AEC state can't be modified while recording");
      return false;
    } 
    this.shouldEnableAec = enable;
    return true;
  }
  
  public boolean setNS(boolean enable) {
    Logging.d("WebRtcAudioEffects", "setNS(" + enable + ")");
    if (!canUseNoiseSuppressor()) {
      Logging.w("WebRtcAudioEffects", "Platform NS is not supported");
      this.shouldEnableNs = false;
      return false;
    } 
    if (this.ns != null && enable != this.shouldEnableNs) {
      Logging.e("WebRtcAudioEffects", "Platform NS state can't be modified while recording");
      return false;
    } 
    this.shouldEnableNs = enable;
    return true;
  }
  
  public void enable(int audioSession) {
    Logging.d("WebRtcAudioEffects", "enable(audioSession=" + audioSession + ")");
    assertTrue((this.aec == null));
    assertTrue((this.ns == null));
    if (isAcousticEchoCancelerSupported()) {
      this.aec = AcousticEchoCanceler.create(audioSession);
      if (this.aec != null) {
        boolean enabled = this.aec.getEnabled();
        boolean enable = (this.shouldEnableAec && canUseAcousticEchoCanceler());
        if (this.aec.setEnabled(enable) != 0)
          Logging.e("WebRtcAudioEffects", "Failed to set the AcousticEchoCanceler state"); 
        Logging.d("WebRtcAudioEffects", "AcousticEchoCanceler: was " + (enabled ? "enabled" : "disabled") + ", enable: " + enable + ", is now: " + (
            
            this.aec.getEnabled() ? "enabled" : "disabled"));
      } else {
        Logging.e("WebRtcAudioEffects", "Failed to create the AcousticEchoCanceler instance");
      } 
    } 
    if (isNoiseSuppressorSupported()) {
      this.ns = NoiseSuppressor.create(audioSession);
      if (this.ns != null) {
        boolean enabled = this.ns.getEnabled();
        boolean enable = (this.shouldEnableNs && canUseNoiseSuppressor());
        if (this.ns.setEnabled(enable) != 0)
          Logging.e("WebRtcAudioEffects", "Failed to set the NoiseSuppressor state"); 
        Logging.d("WebRtcAudioEffects", "NoiseSuppressor: was " + (enabled ? "enabled" : "disabled") + ", enable: " + enable + ", is now: " + (
            this.ns.getEnabled() ? "enabled" : "disabled"));
      } else {
        Logging.e("WebRtcAudioEffects", "Failed to create the NoiseSuppressor instance");
      } 
    } 
  }
  
  public void release() {
    Logging.d("WebRtcAudioEffects", "release");
    if (this.aec != null) {
      this.aec.release();
      this.aec = null;
    } 
    if (this.ns != null) {
      this.ns.release();
      this.ns = null;
    } 
  }
  
  private boolean effectTypeIsVoIP(UUID type) {
    if (Build.VERSION.SDK_INT < 18)
      return false; 
    return ((AudioEffect.EFFECT_TYPE_AEC.equals(type) && isAcousticEchoCancelerSupported()) || (AudioEffect.EFFECT_TYPE_NS
      .equals(type) && isNoiseSuppressorSupported()));
  }
  
  private static void assertTrue(boolean condition) {
    if (!condition)
      throw new AssertionError("Expected condition to be true"); 
  }
  
  @Nullable
  private static AudioEffect.Descriptor[] getAvailableEffects() {
    if (cachedEffects != null)
      return cachedEffects; 
    cachedEffects = AudioEffect.queryEffects();
    return cachedEffects;
  }
  
  private static boolean isEffectTypeAvailable(UUID effectType) {
    AudioEffect.Descriptor[] effects = getAvailableEffects();
    if (effects == null)
      return false; 
    for (AudioEffect.Descriptor d : effects) {
      if (d.type.equals(effectType))
        return true; 
    } 
    return false;
  }
}
