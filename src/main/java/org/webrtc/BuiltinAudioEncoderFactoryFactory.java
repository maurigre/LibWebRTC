package org.webrtc;

public class BuiltinAudioEncoderFactoryFactory implements AudioEncoderFactoryFactory {
  public long createNativeAudioEncoderFactory() {
    return nativeCreateBuiltinAudioEncoderFactory();
  }
  
  private static native long nativeCreateBuiltinAudioEncoderFactory();
}
