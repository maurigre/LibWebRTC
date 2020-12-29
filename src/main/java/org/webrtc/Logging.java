package org.webrtc;

import android.support.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
  private static final Logger fallbackLogger = createFallbackLogger();
  
  private static volatile boolean loggingEnabled;
  
  @Nullable
  private static Loggable loggable;
  
  private static Severity loggableSeverity;
  
  private static Logger createFallbackLogger() {
    Logger fallbackLogger = Logger.getLogger("org.webrtc.Logging");
    fallbackLogger.setLevel(Level.ALL);
    return fallbackLogger;
  }
  
  static void injectLoggable(Loggable injectedLoggable, Severity severity) {
    if (injectedLoggable != null) {
      loggable = injectedLoggable;
      loggableSeverity = severity;
    } 
  }
  
  static void deleteInjectedLoggable() {
    loggable = null;
  }
  
  @Deprecated
  public enum TraceLevel {
    TRACE_NONE(0),
    TRACE_STATEINFO(1),
    TRACE_WARNING(2),
    TRACE_ERROR(4),
    TRACE_CRITICAL(8),
    TRACE_APICALL(16),
    TRACE_DEFAULT(255),
    TRACE_MODULECALL(32),
    TRACE_MEMORY(256),
    TRACE_TIMER(512),
    TRACE_STREAM(1024),
    TRACE_DEBUG(2048),
    TRACE_INFO(4096),
    TRACE_TERSEINFO(8192),
    TRACE_ALL(65535);
    
    public final int level;
    
    TraceLevel(int level) {
      this.level = level;
    }
  }
  
  public enum Severity {
    LS_VERBOSE, LS_INFO, LS_WARNING, LS_ERROR, LS_NONE;
  }
  
  public static void enableLogThreads() {
    nativeEnableLogThreads();
  }
  
  public static void enableLogTimeStamps() {
    nativeEnableLogTimeStamps();
  }
  
  @Deprecated
  public static void enableTracing(String path, EnumSet<TraceLevel> levels) {}
  
  public static synchronized void enableLogToDebugOutput(Severity severity) {
    if (loggable != null)
      throw new IllegalStateException("Logging to native debug output not supported while Loggable is injected. Delete the Loggable before calling this method."); 
    nativeEnableLogToDebugOutput(severity.ordinal());
    loggingEnabled = true;
  }
  
  public static void log(Severity severity, String tag, String message) {
    Level level;
    if (tag == null || message == null)
      throw new IllegalArgumentException("Logging tag or message may not be null."); 
    if (loggable != null) {
      if (severity.ordinal() < loggableSeverity.ordinal())
        return; 
      loggable.onLogMessage(message, severity, tag);
      return;
    } 
    if (loggingEnabled) {
      nativeLog(severity.ordinal(), tag, message);
      return;
    } 
    switch (severity) {
      case LS_ERROR:
        level = Level.SEVERE;
        break;
      case LS_WARNING:
        level = Level.WARNING;
        break;
      case LS_INFO:
        level = Level.INFO;
        break;
      default:
        level = Level.FINE;
        break;
    } 
    fallbackLogger.log(level, tag + ": " + message);
  }
  
  public static void d(String tag, String message) {
    log(Severity.LS_INFO, tag, message);
  }
  
  public static void e(String tag, String message) {
    log(Severity.LS_ERROR, tag, message);
  }
  
  public static void w(String tag, String message) {
    log(Severity.LS_WARNING, tag, message);
  }
  
  public static void e(String tag, String message, Throwable e) {
    log(Severity.LS_ERROR, tag, message);
    log(Severity.LS_ERROR, tag, e.toString());
    log(Severity.LS_ERROR, tag, getStackTraceString(e));
  }
  
  public static void w(String tag, String message, Throwable e) {
    log(Severity.LS_WARNING, tag, message);
    log(Severity.LS_WARNING, tag, e.toString());
    log(Severity.LS_WARNING, tag, getStackTraceString(e));
  }
  
  public static void v(String tag, String message) {
    log(Severity.LS_VERBOSE, tag, message);
  }
  
  private static String getStackTraceString(Throwable e) {
    if (e == null)
      return ""; 
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
  
  private static native void nativeEnableLogToDebugOutput(int paramInt);
  
  private static native void nativeEnableLogThreads();
  
  private static native void nativeEnableLogTimeStamps();
  
  private static native void nativeLog(int paramInt, String paramString1, String paramString2);
}
