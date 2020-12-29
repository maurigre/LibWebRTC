package org.webrtc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceViewRenderer extends SurfaceView implements SurfaceHolder.Callback, VideoSink, RendererCommon.RendererEvents {
  private static final String TAG = "SurfaceViewRenderer";
  
  private final String resourceName;
  
  private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure = new RendererCommon.VideoLayoutMeasure();
  
  private final SurfaceEglRenderer eglRenderer;
  
  private RendererCommon.RendererEvents rendererEvents;
  
  private int rotatedFrameWidth;
  
  private int rotatedFrameHeight;
  
  private boolean enableFixedSize;
  
  private int surfaceWidth;
  
  private int surfaceHeight;
  
  public SurfaceViewRenderer(Context context) {
    super(context);
    this.resourceName = getResourceName();
    this.eglRenderer = new SurfaceEglRenderer(this.resourceName);
    getHolder().addCallback(this);
    getHolder().addCallback(this.eglRenderer);
  }
  
  public SurfaceViewRenderer(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.resourceName = getResourceName();
    this.eglRenderer = new SurfaceEglRenderer(this.resourceName);
    getHolder().addCallback(this);
    getHolder().addCallback(this.eglRenderer);
  }
  
  public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
    init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
  }
  
  public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents, int[] configAttributes, RendererCommon.GlDrawer drawer) {
    ThreadUtils.checkIsOnMainThread();
    this.rendererEvents = rendererEvents;
    this.rotatedFrameWidth = 0;
    this.rotatedFrameHeight = 0;
    this.eglRenderer.init(sharedContext, this, configAttributes, drawer);
  }
  
  public void release() {
    this.eglRenderer.release();
  }
  
  public void addFrameListener(EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
    this.eglRenderer.addFrameListener(listener, scale, drawerParam);
  }
  
  public void addFrameListener(EglRenderer.FrameListener listener, float scale) {
    this.eglRenderer.addFrameListener(listener, scale);
  }
  
  public void removeFrameListener(EglRenderer.FrameListener listener) {
    this.eglRenderer.removeFrameListener(listener);
  }
  
  public void setEnableHardwareScaler(boolean enabled) {
    ThreadUtils.checkIsOnMainThread();
    this.enableFixedSize = enabled;
    updateSurfaceSize();
  }
  
  public void setMirror(boolean mirror) {
    this.eglRenderer.setMirror(mirror);
  }
  
  public void setScalingType(RendererCommon.ScalingType scalingType) {
    ThreadUtils.checkIsOnMainThread();
    this.videoLayoutMeasure.setScalingType(scalingType);
    requestLayout();
  }
  
  public void setScalingType(RendererCommon.ScalingType scalingTypeMatchOrientation, RendererCommon.ScalingType scalingTypeMismatchOrientation) {
    ThreadUtils.checkIsOnMainThread();
    this.videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
    requestLayout();
  }
  
  public void setFpsReduction(float fps) {
    this.eglRenderer.setFpsReduction(fps);
  }
  
  public void disableFpsReduction() {
    this.eglRenderer.disableFpsReduction();
  }
  
  public void pauseVideo() {
    this.eglRenderer.pauseVideo();
  }
  
  public void onFrame(VideoFrame frame) {
    this.eglRenderer.onFrame(frame);
  }
  
  protected void onMeasure(int widthSpec, int heightSpec) {
    ThreadUtils.checkIsOnMainThread();
    Point size = this.videoLayoutMeasure.measure(widthSpec, heightSpec, this.rotatedFrameWidth, this.rotatedFrameHeight);
    setMeasuredDimension(size.x, size.y);
    logD("onMeasure(). New size: " + size.x + "x" + size.y);
  }
  
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    ThreadUtils.checkIsOnMainThread();
    this.eglRenderer.setLayoutAspectRatio((right - left) / (bottom - top));
    updateSurfaceSize();
  }
  
  private void updateSurfaceSize() {
    ThreadUtils.checkIsOnMainThread();
    if (this.enableFixedSize && this.rotatedFrameWidth != 0 && this.rotatedFrameHeight != 0 && getWidth() != 0 && 
      getHeight() != 0) {
      int drawnFrameWidth, drawnFrameHeight;
      float layoutAspectRatio = getWidth() / getHeight();
      float frameAspectRatio = this.rotatedFrameWidth / this.rotatedFrameHeight;
      if (frameAspectRatio > layoutAspectRatio) {
        drawnFrameWidth = (int)(this.rotatedFrameHeight * layoutAspectRatio);
        drawnFrameHeight = this.rotatedFrameHeight;
      } else {
        drawnFrameWidth = this.rotatedFrameWidth;
        drawnFrameHeight = (int)(this.rotatedFrameWidth / layoutAspectRatio);
      } 
      int width = Math.min(getWidth(), drawnFrameWidth);
      int height = Math.min(getHeight(), drawnFrameHeight);
      logD("updateSurfaceSize. Layout size: " + getWidth() + "x" + getHeight() + ", frame size: " + this.rotatedFrameWidth + "x" + this.rotatedFrameHeight + ", requested surface size: " + width + "x" + height + ", old surface size: " + this.surfaceWidth + "x" + this.surfaceHeight);
      if (width != this.surfaceWidth || height != this.surfaceHeight) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
        getHolder().setFixedSize(width, height);
      } 
    } else {
      this.surfaceWidth = this.surfaceHeight = 0;
      getHolder().setSizeFromLayout();
    } 
  }
  
  public void surfaceCreated(SurfaceHolder holder) {
    ThreadUtils.checkIsOnMainThread();
    this.surfaceWidth = this.surfaceHeight = 0;
    updateSurfaceSize();
  }
  
  public void surfaceDestroyed(SurfaceHolder holder) {}
  
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
  
  private String getResourceName() {
    try {
      return getResources().getResourceEntryName(getId());
    } catch (android.content.res.Resources.NotFoundException e) {
      return "";
    } 
  }
  
  public void clearImage() {
    this.eglRenderer.clearImage();
  }
  
  public void onFirstFrameRendered() {
    if (this.rendererEvents != null)
      this.rendererEvents.onFirstFrameRendered(); 
  }
  
  public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
    if (this.rendererEvents != null)
      this.rendererEvents.onFrameResolutionChanged(videoWidth, videoHeight, rotation); 
    int rotatedWidth = (rotation == 0 || rotation == 180) ? videoWidth : videoHeight;
    int rotatedHeight = (rotation == 0 || rotation == 180) ? videoHeight : videoWidth;
    postOrRun(() -> {
          this.rotatedFrameWidth = rotatedWidth;
          this.rotatedFrameHeight = rotatedHeight;
          updateSurfaceSize();
          requestLayout();
        });
  }
  
  private void postOrRun(Runnable r) {
    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
      r.run();
    } else {
      post(r);
    } 
  }
  
  private void logD(String string) {
    Logging.d("SurfaceViewRenderer", this.resourceName + ": " + string);
  }
}
