package org.webrtc;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SoftwareVideoDecoderFactory implements VideoDecoderFactory {
  @Deprecated
  @Nullable
  public VideoDecoder createDecoder(String codecType) {
    return createDecoder(new VideoCodecInfo(codecType, new HashMap<>()));
  }
  
  @Nullable
  public VideoDecoder createDecoder(VideoCodecInfo codecType) {
    if (codecType.getName().equalsIgnoreCase("VP8"))
      return new LibvpxVp8Decoder(); 
    if (codecType.getName().equalsIgnoreCase("VP9") && LibvpxVp9Decoder.nativeIsSupported())
      return new LibvpxVp9Decoder(); 
    return null;
  }
  
  public VideoCodecInfo[] getSupportedCodecs() {
    return supportedCodecs();
  }
  
  static VideoCodecInfo[] supportedCodecs() {
    List<VideoCodecInfo> codecs = new ArrayList<>();
    codecs.add(new VideoCodecInfo("VP8", new HashMap<>()));
    if (LibvpxVp9Decoder.nativeIsSupported())
      codecs.add(new VideoCodecInfo("VP9", new HashMap<>())); 
    return codecs.<VideoCodecInfo>toArray(new VideoCodecInfo[codecs.size()]);
  }
}
