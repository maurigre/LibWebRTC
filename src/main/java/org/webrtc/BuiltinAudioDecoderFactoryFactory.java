package org.webrtc;

public class BuiltinAudioDecoderFactoryFactory implements AudioDecoderFactoryFactory {
  public long createNativeAudioDecoderFactory() {
    return nativeCreateBuiltinAudioDecoderFactory();
  }
  
  private static native long nativeCreateBuiltinAudioDecoderFactory();
}
