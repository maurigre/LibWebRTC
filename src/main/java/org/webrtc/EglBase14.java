package org.webrtc;

import android.opengl.EGLContext;

public interface EglBase14 extends EglBase {
  public static interface Context extends EglBase.Context {
    EGLContext getRawContext();
  }
}
