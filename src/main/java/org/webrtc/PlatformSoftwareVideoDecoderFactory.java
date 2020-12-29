package org.webrtc;

import android.media.MediaCodecInfo;
import android.support.annotation.Nullable;
import java.util.Arrays;

public class PlatformSoftwareVideoDecoderFactory extends MediaCodecVideoDecoderFactory {
  private static final Predicate<MediaCodecInfo> defaultAllowedPredicate = new Predicate<MediaCodecInfo>() {
      private String[] prefixWhitelist = Arrays.<String>copyOf(MediaCodecUtils.SOFTWARE_IMPLEMENTATION_PREFIXES, MediaCodecUtils.SOFTWARE_IMPLEMENTATION_PREFIXES.length);
      
      public boolean test(MediaCodecInfo arg) {
        String name = arg.getName();
        for (String prefix : this.prefixWhitelist) {
          if (name.startsWith(prefix))
            return true; 
        } 
        return false;
      }
    };
  
  public PlatformSoftwareVideoDecoderFactory(@Nullable EglBase.Context sharedContext) {
    super(sharedContext, defaultAllowedPredicate);
  }
}
