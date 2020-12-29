package org.webrtc;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

class EglBase10Impl implements EglBase10 {
  private static final String TAG = "EglBase10Impl";
  
  private static final int EGL_CONTEXT_CLIENT_VERSION = 12440;
  
  private final EGL10 egl;
  
  private EGLContext eglContext;
  
  @Nullable
  private EGLConfig eglConfig;
  
  private EGLDisplay eglDisplay;
  
  private EGLSurface eglSurface = EGL10.EGL_NO_SURFACE;
  
  private static class Context implements EglBase10.Context {
    private final EGLContext eglContext;
    
    public EGLContext getRawContext() {
      return this.eglContext;
    }
    
    public long getNativeEglContext() {
      return 0L;
    }
    
    public Context(EGLContext eglContext) {
      this.eglContext = eglContext;
    }
  }
  
  public EglBase10Impl(EGLContext sharedContext, int[] configAttributes) {
    this.egl = (EGL10)EGLContext.getEGL();
    this.eglDisplay = getEglDisplay();
    this.eglConfig = getEglConfig(this.eglDisplay, configAttributes);
    int openGlesVersion = EglBase.getOpenGlesVersionFromConfig(configAttributes);
    Logging.d("EglBase10Impl", "Using OpenGL ES version " + openGlesVersion);
    this.eglContext = createEglContext(sharedContext, this.eglDisplay, this.eglConfig, openGlesVersion);
  }
  
  public void createSurface(Surface surface) {
    class FakeSurfaceHolder implements SurfaceHolder {
      private final Surface surface;
      
      FakeSurfaceHolder(Surface surface) {
        this.surface = surface;
      }
      
      public void addCallback(SurfaceHolder.Callback callback) {}
      
      public void removeCallback(SurfaceHolder.Callback callback) {}
      
      public boolean isCreating() {
        return false;
      }
      
      @Deprecated
      public void setType(int i) {}
      
      public void setFixedSize(int i, int i2) {}
      
      public void setSizeFromLayout() {}
      
      public void setFormat(int i) {}
      
      public void setKeepScreenOn(boolean b) {}
      
      @Nullable
      public Canvas lockCanvas() {
        return null;
      }
      
      @Nullable
      public Canvas lockCanvas(Rect rect) {
        return null;
      }
      
      public void unlockCanvasAndPost(Canvas canvas) {}
      
      @Nullable
      public Rect getSurfaceFrame() {
        return null;
      }
      
      public Surface getSurface() {
        return this.surface;
      }
    };
    createSurfaceInternal(new FakeSurfaceHolder(surface));
  }
  
  public void createSurface(SurfaceTexture surfaceTexture) {
    createSurfaceInternal(surfaceTexture);
  }
  
  private void createSurfaceInternal(Object nativeWindow) {
    if (!(nativeWindow instanceof SurfaceHolder) && !(nativeWindow instanceof SurfaceTexture))
      throw new IllegalStateException("Input must be either a SurfaceHolder or SurfaceTexture"); 
    checkIsNotReleased();
    if (this.eglSurface != EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("Already has an EGLSurface"); 
    int[] surfaceAttribs = { 12344 };
    this.eglSurface = this.egl.eglCreateWindowSurface(this.eglDisplay, this.eglConfig, nativeWindow, surfaceAttribs);
    if (this.eglSurface == EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("Failed to create window surface: 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
  }
  
  public void createDummyPbufferSurface() {
    createPbufferSurface(1, 1);
  }
  
  public void createPbufferSurface(int width, int height) {
    checkIsNotReleased();
    if (this.eglSurface != EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("Already has an EGLSurface"); 
    int[] surfaceAttribs = { 12375, width, 12374, height, 12344 };
    this.eglSurface = this.egl.eglCreatePbufferSurface(this.eglDisplay, this.eglConfig, surfaceAttribs);
    if (this.eglSurface == EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("Failed to create pixel buffer surface with size " + width + "x" + height + ": 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
  }
  
  public EglBase.Context getEglBaseContext() {
    return new Context(this.eglContext);
  }
  
  public boolean hasSurface() {
    return (this.eglSurface != EGL10.EGL_NO_SURFACE);
  }
  
  public int surfaceWidth() {
    int[] widthArray = new int[1];
    this.egl.eglQuerySurface(this.eglDisplay, this.eglSurface, 12375, widthArray);
    return widthArray[0];
  }
  
  public int surfaceHeight() {
    int[] heightArray = new int[1];
    this.egl.eglQuerySurface(this.eglDisplay, this.eglSurface, 12374, heightArray);
    return heightArray[0];
  }
  
  public void releaseSurface() {
    if (this.eglSurface != EGL10.EGL_NO_SURFACE) {
      this.egl.eglDestroySurface(this.eglDisplay, this.eglSurface);
      this.eglSurface = EGL10.EGL_NO_SURFACE;
    } 
  }
  
  private void checkIsNotReleased() {
    if (this.eglDisplay == EGL10.EGL_NO_DISPLAY || this.eglContext == EGL10.EGL_NO_CONTEXT || this.eglConfig == null)
      throw new RuntimeException("This object has been released"); 
  }
  
  public void release() {
    checkIsNotReleased();
    releaseSurface();
    detachCurrent();
    this.egl.eglDestroyContext(this.eglDisplay, this.eglContext);
    this.egl.eglTerminate(this.eglDisplay);
    this.eglContext = EGL10.EGL_NO_CONTEXT;
    this.eglDisplay = EGL10.EGL_NO_DISPLAY;
    this.eglConfig = null;
  }
  
  public void makeCurrent() {
    checkIsNotReleased();
    if (this.eglSurface == EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("No EGLSurface - can't make current"); 
    synchronized (EglBase.lock) {
      if (!this.egl.eglMakeCurrent(this.eglDisplay, this.eglSurface, this.eglSurface, this.eglContext))
        throw new RuntimeException("eglMakeCurrent failed: 0x" + 
            Integer.toHexString(this.egl.eglGetError())); 
    } 
  }
  
  public void detachCurrent() {
    synchronized (EglBase.lock) {
      if (!this.egl.eglMakeCurrent(this.eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT))
        throw new RuntimeException("eglDetachCurrent failed: 0x" + 
            Integer.toHexString(this.egl.eglGetError())); 
    } 
  }
  
  public void swapBuffers() {
    checkIsNotReleased();
    if (this.eglSurface == EGL10.EGL_NO_SURFACE)
      throw new RuntimeException("No EGLSurface - can't swap buffers"); 
    synchronized (EglBase.lock) {
      this.egl.eglSwapBuffers(this.eglDisplay, this.eglSurface);
    } 
  }
  
  public void swapBuffers(long timeStampNs) {
    swapBuffers();
  }
  
  private EGLDisplay getEglDisplay() {
    EGLDisplay eglDisplay = this.egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL10.EGL_NO_DISPLAY)
      throw new RuntimeException("Unable to get EGL10 display: 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
    int[] version = new int[2];
    if (!this.egl.eglInitialize(eglDisplay, version))
      throw new RuntimeException("Unable to initialize EGL10: 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
    return eglDisplay;
  }
  
  private EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] configAttributes) {
    EGLConfig[] configs = new EGLConfig[1];
    int[] numConfigs = new int[1];
    if (!this.egl.eglChooseConfig(eglDisplay, configAttributes, configs, configs.length, numConfigs))
      throw new RuntimeException("eglChooseConfig failed: 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
    if (numConfigs[0] <= 0)
      throw new RuntimeException("Unable to find any matching EGL config"); 
    EGLConfig eglConfig = configs[0];
    if (eglConfig == null)
      throw new RuntimeException("eglChooseConfig returned null"); 
    return eglConfig;
  }
  
  private EGLContext createEglContext(@Nullable EGLContext sharedContext, EGLDisplay eglDisplay, EGLConfig eglConfig, int openGlesVersion) {
    EGLContext eglContext;
    if (sharedContext != null && sharedContext == EGL10.EGL_NO_CONTEXT)
      throw new RuntimeException("Invalid sharedContext"); 
    int[] contextAttributes = { 12440, openGlesVersion, 12344 };
    EGLContext rootContext = (sharedContext == null) ? EGL10.EGL_NO_CONTEXT : sharedContext;
    synchronized (EglBase.lock) {
      eglContext = this.egl.eglCreateContext(eglDisplay, eglConfig, rootContext, contextAttributes);
    } 
    if (eglContext == EGL10.EGL_NO_CONTEXT)
      throw new RuntimeException("Failed to create EGL context: 0x" + 
          Integer.toHexString(this.egl.eglGetError())); 
    return eglContext;
  }
}
