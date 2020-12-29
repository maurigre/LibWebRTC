package org.webrtc;

import javax.microedition.khronos.egl.EGLContext;

public interface EglBase10 extends EglBase {
  public static interface Context extends EglBase.Context {
    EGLContext getRawContext();
  }
}
