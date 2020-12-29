package org.webrtc;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EglRenderer implements VideoSink {
  private static final String TAG = "EglRenderer";
  
  private static final long LOG_INTERVAL_SEC = 4L;
  
  protected final String name;
  
  private static class FrameListenerAndParams {
    public final EglRenderer.FrameListener listener;
    
    public final float scale;
    
    public final RendererCommon.GlDrawer drawer;
    
    public final boolean applyFpsReduction;
    
    public FrameListenerAndParams(EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawer, boolean applyFpsReduction) {
      this.listener = listener;
      this.scale = scale;
      this.drawer = drawer;
      this.applyFpsReduction = applyFpsReduction;
    }
  }
  
  private class EglSurfaceCreation implements Runnable {
    private Object surface;
    
    private EglSurfaceCreation() {}
    
    public synchronized void setSurface(Object surface) {
      this.surface = surface;
    }
    
    public synchronized void run() {
      if (this.surface != null && EglRenderer.this.eglBase != null && !EglRenderer.this.eglBase.hasSurface()) {
        if (this.surface instanceof Surface) {
          EglRenderer.this.eglBase.createSurface((Surface)this.surface);
        } else if (this.surface instanceof SurfaceTexture) {
          EglRenderer.this.eglBase.createSurface((SurfaceTexture)this.surface);
        } else {
          throw new IllegalStateException("Invalid surface: " + this.surface);
        } 
        EglRenderer.this.eglBase.makeCurrent();
        GLES20.glPixelStorei(3317, 1);
      } 
    }
  }
  
  private static class HandlerWithExceptionCallback extends Handler {
    private final Runnable exceptionCallback;
    
    public HandlerWithExceptionCallback(Looper looper, Runnable exceptionCallback) {
      super(looper);
      this.exceptionCallback = exceptionCallback;
    }
    
    public void dispatchMessage(Message msg) {
      try {
        super.dispatchMessage(msg);
      } catch (Exception e) {
        Logging.e("EglRenderer", "Exception on EglRenderer thread", e);
        this.exceptionCallback.run();
        throw e;
      } 
    }
  }
  
  private final Object handlerLock = new Object();
  
  @Nullable
  private Handler renderThreadHandler;
  
  private final ArrayList<FrameListenerAndParams> frameListeners = new ArrayList<>();
  
  private volatile ErrorCallback errorCallback;
  
  private final Object fpsReductionLock = new Object();
  
  private long nextFrameTimeNs;
  
  private long minRenderPeriodNs;
  
  @Nullable
  private EglBase eglBase;
  
  private final VideoFrameDrawer frameDrawer;
  
  @Nullable
  private RendererCommon.GlDrawer drawer;
  
  private boolean usePresentationTimeStamp;
  
  private final Matrix drawMatrix = new Matrix();
  
  private final Object frameLock = new Object();
  
  @Nullable
  private VideoFrame pendingFrame;
  
  private final Object layoutLock = new Object();
  
  private float layoutAspectRatio;
  
  private boolean mirrorHorizontally;
  
  private boolean mirrorVertically;
  
  private final Object statisticsLock = new Object();
  
  private int framesReceived;
  
  private int framesDropped;
  
  private int framesRendered;
  
  private long statisticsStartTimeNs;
  
  private long renderTimeNs;
  
  private long renderSwapBufferTimeNs;
  
  private final GlTextureFrameBuffer bitmapTextureFramebuffer = new GlTextureFrameBuffer(6408);
  
  private final Runnable logStatisticsRunnable = new Runnable() {
      public void run() {
        EglRenderer.this.logStatistics();
        synchronized (EglRenderer.this.handlerLock) {
          if (EglRenderer.this.renderThreadHandler != null) {
            EglRenderer.this.renderThreadHandler.removeCallbacks(EglRenderer.this.logStatisticsRunnable);
            EglRenderer.this.renderThreadHandler.postDelayed(EglRenderer.this
                .logStatisticsRunnable, TimeUnit.SECONDS.toMillis(4L));
          } 
        } 
      }
    };
  
  private final EglSurfaceCreation eglSurfaceCreationRunnable = new EglSurfaceCreation();
  
  public EglRenderer(String name) {
    this(name, new VideoFrameDrawer());
  }
  
  public EglRenderer(String name, VideoFrameDrawer videoFrameDrawer) {
    this.name = name;
    this.frameDrawer = videoFrameDrawer;
  }
  
  public void init(@Nullable EglBase.Context sharedContext, int[] configAttributes, RendererCommon.GlDrawer drawer, boolean usePresentationTimeStamp) {
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler != null)
        throw new IllegalStateException(this.name + "Already initialized"); 
      logD("Initializing EglRenderer");
      this.drawer = drawer;
      this.usePresentationTimeStamp = usePresentationTimeStamp;
      HandlerThread renderThread = new HandlerThread(this.name + "EglRenderer");
      renderThread.start();
      this
        .renderThreadHandler = new HandlerWithExceptionCallback(renderThread.getLooper(), new Runnable() {
            public void run() {
              synchronized (EglRenderer.this.handlerLock) {
                EglRenderer.this.renderThreadHandler = null;
              } 
            }
          });
      ThreadUtils.invokeAtFrontUninterruptibly(this.renderThreadHandler, () -> {
            if (sharedContext == null) {
              logD("EglBase10.create context");
              this.eglBase = EglBase.createEgl10(configAttributes);
            } else {
              logD("EglBase.create shared context");
              this.eglBase = EglBase.create(sharedContext, configAttributes);
            } 
          });
      this.renderThreadHandler.post(this.eglSurfaceCreationRunnable);
      long currentTimeNs = System.nanoTime();
      resetStatistics(currentTimeNs);
      this.renderThreadHandler.postDelayed(this.logStatisticsRunnable, TimeUnit.SECONDS
          .toMillis(4L));
    } 
  }
  
  public void init(@Nullable EglBase.Context sharedContext, int[] configAttributes, RendererCommon.GlDrawer drawer) {
    init(sharedContext, configAttributes, drawer, false);
  }
  
  public void createEglSurface(Surface surface) {
    createEglSurfaceInternal(surface);
  }
  
  public void createEglSurface(SurfaceTexture surfaceTexture) {
    createEglSurfaceInternal(surfaceTexture);
  }
  
  private void createEglSurfaceInternal(Object surface) {
    this.eglSurfaceCreationRunnable.setSurface(surface);
    postToRenderThread(this.eglSurfaceCreationRunnable);
  }
  
  public void release() {
    logD("Releasing.");
    CountDownLatch eglCleanupBarrier = new CountDownLatch(1);
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler == null) {
        logD("Already released");
        return;
      } 
      this.renderThreadHandler.removeCallbacks(this.logStatisticsRunnable);
      this.renderThreadHandler.postAtFrontOfQueue(() -> {
            synchronized (EglBase.lock) {
              GLES20.glUseProgram(0);
            } 
            if (this.drawer != null) {
              this.drawer.release();
              this.drawer = null;
            } 
            this.frameDrawer.release();
            this.bitmapTextureFramebuffer.release();
            if (this.eglBase != null) {
              logD("eglBase detach and release.");
              this.eglBase.detachCurrent();
              this.eglBase.release();
              this.eglBase = null;
            } 
            this.frameListeners.clear();
            eglCleanupBarrier.countDown();
          });
      Looper renderLooper = this.renderThreadHandler.getLooper();
      this.renderThreadHandler.post(() -> {
            logD("Quitting render thread.");
            renderLooper.quit();
          });
      this.renderThreadHandler = null;
    } 
    ThreadUtils.awaitUninterruptibly(eglCleanupBarrier);
    synchronized (this.frameLock) {
      if (this.pendingFrame != null) {
        this.pendingFrame.release();
        this.pendingFrame = null;
      } 
    } 
    logD("Releasing done.");
  }
  
  private void resetStatistics(long currentTimeNs) {
    synchronized (this.statisticsLock) {
      this.statisticsStartTimeNs = currentTimeNs;
      this.framesReceived = 0;
      this.framesDropped = 0;
      this.framesRendered = 0;
      this.renderTimeNs = 0L;
      this.renderSwapBufferTimeNs = 0L;
    } 
  }
  
  public void printStackTrace() {
    synchronized (this.handlerLock) {
      Thread renderThread = (this.renderThreadHandler == null) ? null : this.renderThreadHandler.getLooper().getThread();
      if (renderThread != null) {
        StackTraceElement[] renderStackTrace = renderThread.getStackTrace();
        if (renderStackTrace.length > 0) {
          logW("EglRenderer stack trace:");
          for (StackTraceElement traceElem : renderStackTrace)
            logW(traceElem.toString()); 
        } 
      } 
    } 
  }
  
  public void setMirror(boolean mirror) {
    logD("setMirrorHorizontally: " + mirror);
    synchronized (this.layoutLock) {
      this.mirrorHorizontally = mirror;
    } 
  }
  
  public void setMirrorVertically(boolean mirrorVertically) {
    logD("setMirrorVertically: " + mirrorVertically);
    synchronized (this.layoutLock) {
      this.mirrorVertically = mirrorVertically;
    } 
  }
  
  public void setLayoutAspectRatio(float layoutAspectRatio) {
    logD("setLayoutAspectRatio: " + layoutAspectRatio);
    synchronized (this.layoutLock) {
      this.layoutAspectRatio = layoutAspectRatio;
    } 
  }
  
  public void setFpsReduction(float fps) {
    logD("setFpsReduction: " + fps);
    synchronized (this.fpsReductionLock) {
      long previousRenderPeriodNs = this.minRenderPeriodNs;
      if (fps <= 0.0F) {
        this.minRenderPeriodNs = Long.MAX_VALUE;
      } else {
        this.minRenderPeriodNs = (long)((float)TimeUnit.SECONDS.toNanos(1L) / fps);
      } 
      if (this.minRenderPeriodNs != previousRenderPeriodNs)
        this.nextFrameTimeNs = System.nanoTime(); 
    } 
  }
  
  public void disableFpsReduction() {
    setFpsReduction(Float.POSITIVE_INFINITY);
  }
  
  public void pauseVideo() {
    setFpsReduction(0.0F);
  }
  
  public void addFrameListener(FrameListener listener, float scale) {
    addFrameListener(listener, scale, null, false);
  }
  
  public void addFrameListener(FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
    addFrameListener(listener, scale, drawerParam, false);
  }
  
  public void addFrameListener(FrameListener listener, float scale, @Nullable RendererCommon.GlDrawer drawerParam, boolean applyFpsReduction) {
    postToRenderThread(() -> {
          RendererCommon.GlDrawer listenerDrawer = (drawerParam == null) ? this.drawer : drawerParam;
          this.frameListeners.add(new FrameListenerAndParams(listener, scale, listenerDrawer, applyFpsReduction));
        });
  }
  
  public void removeFrameListener(FrameListener listener) {
    CountDownLatch latch = new CountDownLatch(1);
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler == null)
        return; 
      if (Thread.currentThread() == this.renderThreadHandler.getLooper().getThread())
        throw new RuntimeException("removeFrameListener must not be called on the render thread."); 
      postToRenderThread(() -> {
            latch.countDown();
            Iterator<FrameListenerAndParams> iter = this.frameListeners.iterator();
            while (iter.hasNext()) {
              if (((FrameListenerAndParams)iter.next()).listener == listener)
                iter.remove(); 
            } 
          });
    } 
    ThreadUtils.awaitUninterruptibly(latch);
  }
  
  public void setErrorCallback(ErrorCallback errorCallback) {
    this.errorCallback = errorCallback;
  }
  
  public void onFrame(VideoFrame frame) {
    boolean dropOldFrame;
    synchronized (this.statisticsLock) {
      this.framesReceived++;
    } 
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler == null) {
        logD("Dropping frame - Not initialized or already released.");
        return;
      } 
      synchronized (this.frameLock) {
        dropOldFrame = (this.pendingFrame != null);
        if (dropOldFrame)
          this.pendingFrame.release(); 
        this.pendingFrame = frame;
        this.pendingFrame.retain();
        this.renderThreadHandler.post(this::renderFrameOnRenderThread);
      } 
    } 
    if (dropOldFrame)
      synchronized (this.statisticsLock) {
        this.framesDropped++;
      }  
  }
  
  public void releaseEglSurface(Runnable completionCallback) {
    this.eglSurfaceCreationRunnable.setSurface(null);
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler != null) {
        this.renderThreadHandler.removeCallbacks(this.eglSurfaceCreationRunnable);
        this.renderThreadHandler.postAtFrontOfQueue(() -> {
              if (this.eglBase != null) {
                this.eglBase.detachCurrent();
                this.eglBase.releaseSurface();
              } 
              completionCallback.run();
            });
        return;
      } 
    } 
    completionCallback.run();
  }
  
  private void postToRenderThread(Runnable runnable) {
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler != null)
        this.renderThreadHandler.post(runnable); 
    } 
  }
  
  private void clearSurfaceOnRenderThread(float r, float g, float b, float a) {
    if (this.eglBase != null && this.eglBase.hasSurface()) {
      logD("clearSurface");
      GLES20.glClearColor(r, g, b, a);
      GLES20.glClear(16384);
      this.eglBase.swapBuffers();
    } 
  }
  
  public void clearImage() {
    clearImage(0.0F, 0.0F, 0.0F, 0.0F);
  }
  
  public void clearImage(float r, float g, float b, float a) {
    synchronized (this.handlerLock) {
      if (this.renderThreadHandler == null)
        return; 
      this.renderThreadHandler.postAtFrontOfQueue(() -> clearSurfaceOnRenderThread(r, g, b, a));
    } 
  }

  /**
   * Renders and releases |pendingFrame|.
   */
  private void renderFrameOnRenderThread() {
    // Fetch and render |pendingFrame|.
    final VideoFrame frame;
    synchronized (frameLock) {
      if (pendingFrame == null) {
        return;
      }
      frame = pendingFrame;
      pendingFrame = null;
    }
    if (eglBase == null || !eglBase.hasSurface()) {
      logD("Dropping frame - No surface");
      frame.release();
      return;
    }
    // Check if fps reduction is active.
    final boolean shouldRenderFrame;
    synchronized (fpsReductionLock) {
      if (minRenderPeriodNs == Long.MAX_VALUE) {
        // Rendering is paused.
        shouldRenderFrame = false;
      } else if (minRenderPeriodNs <= 0) {
        // FPS reduction is disabled.
        shouldRenderFrame = true;
      } else {
        final long currentTimeNs = System.nanoTime();
        if (currentTimeNs < nextFrameTimeNs) {
          logD("Skipping frame rendering - fps reduction is active.");
          shouldRenderFrame = false;
        } else {
          nextFrameTimeNs += minRenderPeriodNs;
          // The time for the next frame should always be in the future.
          nextFrameTimeNs = Math.max(nextFrameTimeNs, currentTimeNs);
          shouldRenderFrame = true;
        }
      }
    }

    final long startTimeNs = System.nanoTime();

    final float frameAspectRatio = frame.getRotatedWidth() / (float) frame.getRotatedHeight();
    final float drawnAspectRatio;
    synchronized (layoutLock) {
      drawnAspectRatio = layoutAspectRatio != 0f ? layoutAspectRatio : frameAspectRatio;
    }

    final float scaleX;
    final float scaleY;

    if (frameAspectRatio > drawnAspectRatio) {
      scaleX = drawnAspectRatio / frameAspectRatio;
      scaleY = 1f;
    } else {
      scaleX = 1f;
      scaleY = frameAspectRatio / drawnAspectRatio;
    }

    drawMatrix.reset();
    drawMatrix.preTranslate(0.5f, 0.5f);
    drawMatrix.preScale(mirrorHorizontally ? -1f : 1f, mirrorVertically ? -1f : 1f);
    drawMatrix.preScale(scaleX, scaleY);
    drawMatrix.preTranslate(-0.5f, -0.5f);

    try {
      if (shouldRenderFrame) {
        GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        frameDrawer.drawFrame(frame, drawer, drawMatrix, 0 /* viewportX */, 0 /* viewportY */,
                eglBase.surfaceWidth(), eglBase.surfaceHeight());

        final long swapBuffersStartTimeNs = System.nanoTime();
        if (usePresentationTimeStamp) {
          eglBase.swapBuffers(frame.getTimestampNs());
        } else {
          eglBase.swapBuffers();
        }

        final long currentTimeNs = System.nanoTime();
        synchronized (statisticsLock) {
          ++framesRendered;
          renderTimeNs += (currentTimeNs - startTimeNs);
          renderSwapBufferTimeNs += (currentTimeNs - swapBuffersStartTimeNs);
        }
      }

      notifyCallbacks(frame, shouldRenderFrame);
    } catch (GlUtil.GlOutOfMemoryException e) {
      logE("Error while drawing frame", e);
      final ErrorCallback errorCallback = this.errorCallback;
      if (errorCallback != null) {
        errorCallback.onGlOutOfMemory();
      }
      // Attempt to free up some resources.
      drawer.release();
      frameDrawer.release();
      bitmapTextureFramebuffer.release();
      // Continue here on purpose and retry again for next frame. In worst case, this is a continous
      // problem and no more frames will be drawn.
    } finally {
      frame.release();
    }
  }

  private void notifyCallbacks(VideoFrame frame, boolean wasRendered) {
    if (this.frameListeners.isEmpty())
      return; 
    this.drawMatrix.reset();
    this.drawMatrix.preTranslate(0.5F, 0.5F);
    this.drawMatrix.preScale(this.mirrorHorizontally ? -1.0F : 1.0F, this.mirrorVertically ? -1.0F : 1.0F);
    this.drawMatrix.preScale(1.0F, -1.0F);
    this.drawMatrix.preTranslate(-0.5F, -0.5F);
    Iterator<FrameListenerAndParams> it = this.frameListeners.iterator();
    while (it.hasNext()) {
      FrameListenerAndParams listenerAndParams = it.next();
      if (!wasRendered && listenerAndParams.applyFpsReduction)
        continue; 
      it.remove();
      int scaledWidth = (int)(listenerAndParams.scale * frame.getRotatedWidth());
      int scaledHeight = (int)(listenerAndParams.scale * frame.getRotatedHeight());
      if (scaledWidth == 0 || scaledHeight == 0) {
        listenerAndParams.listener.onFrame(null);
        continue;
      } 
      this.bitmapTextureFramebuffer.setSize(scaledWidth, scaledHeight);
      GLES20.glBindFramebuffer(36160, this.bitmapTextureFramebuffer.getFrameBufferId());
      GLES20.glFramebufferTexture2D(36160, 36064, 3553, this.bitmapTextureFramebuffer
          .getTextureId(), 0);
      GLES20.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GLES20.glClear(16384);
      this.frameDrawer.drawFrame(frame, listenerAndParams.drawer, this.drawMatrix, 0, 0, scaledWidth, scaledHeight);
      ByteBuffer bitmapBuffer = ByteBuffer.allocateDirect(scaledWidth * scaledHeight * 4);
      GLES20.glViewport(0, 0, scaledWidth, scaledHeight);
      GLES20.glReadPixels(0, 0, scaledWidth, scaledHeight, 6408, 5121, bitmapBuffer);
      GLES20.glBindFramebuffer(36160, 0);
      GlUtil.checkNoGLES2Error("EglRenderer.notifyCallbacks");
      Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
      bitmap.copyPixelsFromBuffer(bitmapBuffer);
      Log.i(TAG, "notifyCallbacks: MAURI");
      listenerAndParams.listener.onFrame(bitmap);
    } 
  }
  
  private String averageTimeAsString(long sumTimeNs, int count) {
    return (count <= 0) ? "NA" : (TimeUnit.NANOSECONDS.toMicros(sumTimeNs / count) + " us");
  }
  
  private void logStatistics() {
    DecimalFormat fpsFormat = new DecimalFormat("#.0");
    long currentTimeNs = System.nanoTime();
    synchronized (this.statisticsLock) {
      long elapsedTimeNs = currentTimeNs - this.statisticsStartTimeNs;
      if (elapsedTimeNs <= 0L || (this.minRenderPeriodNs == Long.MAX_VALUE && this.framesReceived == 0))
        return; 
      float renderFps = (float)(this.framesRendered * TimeUnit.SECONDS.toNanos(1L)) / (float)elapsedTimeNs;
      logD("Duration: " + TimeUnit.NANOSECONDS.toMillis(elapsedTimeNs) + " ms. Frames received: " + this.framesReceived + ". Dropped: " + this.framesDropped + ". Rendered: " + this.framesRendered + ". Render fps: " + fpsFormat
          
          .format(renderFps) + ". Average render time: " + 
          averageTimeAsString(this.renderTimeNs, this.framesRendered) + ". Average swapBuffer time: " + 
          
          averageTimeAsString(this.renderSwapBufferTimeNs, this.framesRendered) + ".");
      resetStatistics(currentTimeNs);
    } 
  }
  
  private void logE(String string, Throwable e) {
    Logging.e("EglRenderer", this.name + string, e);
  }
  
  private void logD(String string) {
    Logging.d("EglRenderer", this.name + string);
  }
  
  private void logW(String string) {
    Logging.w("EglRenderer", this.name + string);
  }
  
  public static interface ErrorCallback {
    void onGlOutOfMemory();
  }
  
  public static interface FrameListener {
    void onFrame(Bitmap param1Bitmap);
  }
}
