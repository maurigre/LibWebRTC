package org.webrtc;

import android.media.MediaCodecInfo;
import android.os.Build;
import android.support.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

class MediaCodecUtils {
  private static final String TAG = "MediaCodecUtils";
  
  static final String EXYNOS_PREFIX = "OMX.Exynos.";
  
  static final String INTEL_PREFIX = "OMX.Intel.";
  
  static final String NVIDIA_PREFIX = "OMX.Nvidia.";
  
  static final String QCOM_PREFIX = "OMX.qcom.";
  
  static final String[] SOFTWARE_IMPLEMENTATION_PREFIXES = new String[] { "OMX.google.", "OMX.SEC." };
  
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar32m4ka = 2141391873;
  
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar16m4ka = 2141391874;
  
  static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar64x32Tile2m8ka = 2141391875;
  
  static final int COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m = 2141391876;
  
  static final int[] DECODER_COLOR_FORMATS = new int[] { 19, 21, 2141391872, 2141391873, 2141391874, 2141391875, 2141391876 };
  
  static final int[] ENCODER_COLOR_FORMATS = new int[] { 19, 21, 2141391872, 2141391876 };
  
  static final int[] TEXTURE_COLOR_FORMATS = getTextureColorFormats();
  
  private static int[] getTextureColorFormats() {
    if (Build.VERSION.SDK_INT >= 18)
      return new int[] { 2130708361 }; 
    return new int[0];
  }
  
  @Nullable
  static Integer selectColorFormat(int[] supportedColorFormats, MediaCodecInfo.CodecCapabilities capabilities) {
    for (int supportedColorFormat : supportedColorFormats) {
      for (int codecColorFormat : capabilities.colorFormats) {
        if (codecColorFormat == supportedColorFormat)
          return Integer.valueOf(codecColorFormat); 
      } 
    } 
    return null;
  }
  
  static boolean codecSupportsType(MediaCodecInfo info, VideoCodecMimeType type) {
    for (String mimeType : info.getSupportedTypes()) {
      if (type.mimeType().equals(mimeType))
        return true; 
    } 
    return false;
  }
  
  static Map<String, String> getCodecProperties(VideoCodecMimeType type, boolean highProfile) {
    switch (type) {
      case VP8:
      case VP9:
        return new HashMap<>();
      case H264:
        return H264Utils.getDefaultH264Params(highProfile);
    } 
    throw new IllegalArgumentException("Unsupported codec: " + type);
  }
}
