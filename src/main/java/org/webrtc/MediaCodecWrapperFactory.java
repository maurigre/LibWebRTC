package org.webrtc;

import java.io.IOException;

interface MediaCodecWrapperFactory {
  MediaCodecWrapper createByCodecName(String paramString) throws IOException;
}
