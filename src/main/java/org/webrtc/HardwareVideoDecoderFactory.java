package org.webrtc;

import android.media.MediaCodecInfo;
import android.support.annotation.Nullable;
import java.util.Arrays;

public class HardwareVideoDecoderFactory extends MediaCodecVideoDecoderFactory {
  private static final Predicate<MediaCodecInfo> defaultAllowedPredicate = new Predicate<MediaCodecInfo>() {
      private String[] prefixBlacklist = Arrays.<String>copyOf(MediaCodecUtils.SOFTWARE_IMPLEMENTATION_PREFIXES, MediaCodecUtils.SOFTWARE_IMPLEMENTATION_PREFIXES.length);
      
      public boolean test(MediaCodecInfo arg) {
        String name = arg.getName();
        for (String prefix : this.prefixBlacklist) {
          if (name.startsWith(prefix))
            return false; 
        } 
        return true;
      }
    };
  
  @Deprecated
  public HardwareVideoDecoderFactory() {
    this(null);
  }
  
  public HardwareVideoDecoderFactory(@Nullable EglBase.Context sharedContext) {
    this(sharedContext, null);
  }
  
  public HardwareVideoDecoderFactory(@Nullable EglBase.Context sharedContext, @Nullable Predicate<MediaCodecInfo> codecAllowedPredicate) {
    super(sharedContext, 
        (codecAllowedPredicate == null) ? defaultAllowedPredicate : 
        codecAllowedPredicate.and(defaultAllowedPredicate));
  }
}
