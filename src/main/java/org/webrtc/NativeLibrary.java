package org.webrtc;

class NativeLibrary {
  private static String TAG = "NativeLibrary";
  
  static class DefaultLoader implements NativeLibraryLoader {
    public boolean load(String name) {
      Logging.d(NativeLibrary.TAG, "Loading library: " + name);
      try {
        System.loadLibrary(name);
      } catch (UnsatisfiedLinkError e) {
        Logging.e(NativeLibrary.TAG, "Failed to load native library: " + name, e);
        return false;
      } 
      return true;
    }
  }
  
  private static Object lock = new Object();
  
  private static boolean libraryLoaded;
  
  static void initialize(NativeLibraryLoader loader, String libraryName) {
    synchronized (lock) {
      if (libraryLoaded) {
        Logging.d(TAG, "Native library has already been loaded.");
        return;
      } 
      Logging.d(TAG, "Loading native library: " + libraryName);
      libraryLoaded = loader.load(libraryName);
    } 
  }
  
  static boolean isLoaded() {
    synchronized (lock) {
      return libraryLoaded;
    } 
  }
}
