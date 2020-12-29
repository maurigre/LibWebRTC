package org.webrtc;

class JNILogging {
  private final Loggable loggable;
  
  public JNILogging(Loggable loggable) {
    this.loggable = loggable;
  }
  
  @CalledByNative
  public void logToInjectable(String message, Integer severity, String tag) {
    this.loggable.onLogMessage(message, Logging.Severity.values()[severity.intValue()], tag);
  }
}
