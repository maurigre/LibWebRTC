package org.webrtc;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NetworkMonitor {
  private static final String TAG = "NetworkMonitor";
  
  private final ArrayList<Long> nativeNetworkObservers;
  
  private final ArrayList<NetworkObserver> networkObservers;
  
  private static class InstanceHolder {
    static final NetworkMonitor instance = new NetworkMonitor();
  }
  
  private final Object autoDetectLock = new Object();
  
  @Nullable
  private NetworkMonitorAutoDetect autoDetect;
  
  private int numObservers;
  
  private volatile NetworkMonitorAutoDetect.ConnectionType currentConnectionType;
  
  private NetworkMonitor() {
    this.nativeNetworkObservers = new ArrayList<>();
    this.networkObservers = new ArrayList<>();
    this.numObservers = 0;
    this.currentConnectionType = NetworkMonitorAutoDetect.ConnectionType.CONNECTION_UNKNOWN;
  }
  
  @Deprecated
  public static void init(Context context) {}
  
  @CalledByNative
  public static NetworkMonitor getInstance() {
    return InstanceHolder.instance;
  }
  
  private static void assertIsTrue(boolean condition) {
    if (!condition)
      throw new AssertionError("Expected to be true"); 
  }
  
  public void startMonitoring(Context applicationContext) {
    synchronized (this.autoDetectLock) {
      this.numObservers++;
      if (this.autoDetect == null)
        this.autoDetect = createAutoDetect(applicationContext); 
      this
        .currentConnectionType = NetworkMonitorAutoDetect.getConnectionType(this.autoDetect.getCurrentNetworkState());
    } 
  }
  
  @Deprecated
  public void startMonitoring() {
    startMonitoring(ContextUtils.getApplicationContext());
  }
  
  @CalledByNative
  private void startMonitoring(@Nullable Context applicationContext, long nativeObserver) {
    Logging.d("NetworkMonitor", "Start monitoring with native observer " + nativeObserver);
    startMonitoring(
        (applicationContext != null) ? applicationContext : ContextUtils.getApplicationContext());
    synchronized (this.nativeNetworkObservers) {
      this.nativeNetworkObservers.add(Long.valueOf(nativeObserver));
    } 
    updateObserverActiveNetworkList(nativeObserver);
    notifyObserversOfConnectionTypeChange(this.currentConnectionType);
  }
  
  public void stopMonitoring() {
    synchronized (this.autoDetectLock) {
      if (--this.numObservers == 0) {
        this.autoDetect.destroy();
        this.autoDetect = null;
      } 
    } 
  }
  
  @CalledByNative
  private void stopMonitoring(long nativeObserver) {
    Logging.d("NetworkMonitor", "Stop monitoring with native observer " + nativeObserver);
    stopMonitoring();
    synchronized (this.nativeNetworkObservers) {
      this.nativeNetworkObservers.remove(Long.valueOf(nativeObserver));
    } 
  }
  
  @CalledByNative
  private boolean networkBindingSupported() {
    synchronized (this.autoDetectLock) {
      return (this.autoDetect != null && this.autoDetect.supportNetworkCallback());
    } 
  }
  
  @CalledByNative
  private static int androidSdkInt() {
    return Build.VERSION.SDK_INT;
  }
  
  private NetworkMonitorAutoDetect.ConnectionType getCurrentConnectionType() {
    return this.currentConnectionType;
  }
  
  private long getCurrentDefaultNetId() {
    synchronized (this.autoDetectLock) {
      return (this.autoDetect == null) ? -1L : this.autoDetect.getDefaultNetId();
    } 
  }
  
  private NetworkMonitorAutoDetect createAutoDetect(Context appContext) {
    return new NetworkMonitorAutoDetect(new NetworkMonitorAutoDetect.Observer() {
          public void onConnectionTypeChanged(NetworkMonitorAutoDetect.ConnectionType newConnectionType) {
            NetworkMonitor.this.updateCurrentConnectionType(newConnectionType);
          }
          
          public void onNetworkConnect(NetworkMonitorAutoDetect.NetworkInformation networkInfo) {
            NetworkMonitor.this.notifyObserversOfNetworkConnect(networkInfo);
          }
          
          public void onNetworkDisconnect(long networkHandle) {
            NetworkMonitor.this.notifyObserversOfNetworkDisconnect(networkHandle);
          }
        },  appContext);
  }
  
  public static interface NetworkObserver {
    void onConnectionTypeChanged(NetworkMonitorAutoDetect.ConnectionType param1ConnectionType);
  }
  
  private void updateCurrentConnectionType(NetworkMonitorAutoDetect.ConnectionType newConnectionType) {
    this.currentConnectionType = newConnectionType;
    notifyObserversOfConnectionTypeChange(newConnectionType);
  }
  
  private void notifyObserversOfConnectionTypeChange(NetworkMonitorAutoDetect.ConnectionType newConnectionType) {
    List<NetworkObserver> javaObservers;
    List<Long> nativeObservers = getNativeNetworkObserversSync();
    for (Long nativeObserver : nativeObservers)
      nativeNotifyConnectionTypeChanged(nativeObserver.longValue()); 
    synchronized (this.networkObservers) {
      javaObservers = new ArrayList<>(this.networkObservers);
    } 
    for (NetworkObserver observer : javaObservers)
      observer.onConnectionTypeChanged(newConnectionType); 
  }
  
  private void notifyObserversOfNetworkConnect(NetworkMonitorAutoDetect.NetworkInformation networkInfo) {
    List<Long> nativeObservers = getNativeNetworkObserversSync();
    for (Long nativeObserver : nativeObservers)
      nativeNotifyOfNetworkConnect(nativeObserver.longValue(), networkInfo); 
  }
  
  private void notifyObserversOfNetworkDisconnect(long networkHandle) {
    List<Long> nativeObservers = getNativeNetworkObserversSync();
    for (Long nativeObserver : nativeObservers)
      nativeNotifyOfNetworkDisconnect(nativeObserver.longValue(), networkHandle); 
  }
  
  private void updateObserverActiveNetworkList(long nativeObserver) {
    List<NetworkMonitorAutoDetect.NetworkInformation> networkInfoList;
    synchronized (this.autoDetectLock) {
      networkInfoList = (this.autoDetect == null) ? null : this.autoDetect.getActiveNetworkList();
    } 
    if (networkInfoList == null || networkInfoList.size() == 0)
      return; 
    NetworkMonitorAutoDetect.NetworkInformation[] networkInfos = new NetworkMonitorAutoDetect.NetworkInformation[networkInfoList.size()];
    networkInfos = networkInfoList.<NetworkMonitorAutoDetect.NetworkInformation>toArray(networkInfos);
    nativeNotifyOfActiveNetworkList(nativeObserver, networkInfos);
  }
  
  private List<Long> getNativeNetworkObserversSync() {
    synchronized (this.nativeNetworkObservers) {
      return new ArrayList<>(this.nativeNetworkObservers);
    } 
  }
  
  @Deprecated
  public static void addNetworkObserver(NetworkObserver observer) {
    getInstance().addObserver(observer);
  }
  
  public void addObserver(NetworkObserver observer) {
    synchronized (this.networkObservers) {
      this.networkObservers.add(observer);
    } 
  }
  
  @Deprecated
  public static void removeNetworkObserver(NetworkObserver observer) {
    getInstance().removeObserver(observer);
  }
  
  public void removeObserver(NetworkObserver observer) {
    synchronized (this.networkObservers) {
      this.networkObservers.remove(observer);
    } 
  }
  
  public static boolean isOnline() {
    NetworkMonitorAutoDetect.ConnectionType connectionType = getInstance().getCurrentConnectionType();
    return (connectionType != NetworkMonitorAutoDetect.ConnectionType.CONNECTION_NONE);
  }
  
  @Nullable
  NetworkMonitorAutoDetect getNetworkMonitorAutoDetect() {
    synchronized (this.autoDetectLock) {
      return this.autoDetect;
    } 
  }
  
  int getNumObservers() {
    synchronized (this.autoDetectLock) {
      return this.numObservers;
    } 
  }
  
  static NetworkMonitorAutoDetect createAndSetAutoDetectForTest(Context context) {
    NetworkMonitor networkMonitor = getInstance();
    NetworkMonitorAutoDetect autoDetect = networkMonitor.createAutoDetect(context);
    return networkMonitor.autoDetect = autoDetect;
  }
  
  private native void nativeNotifyConnectionTypeChanged(long paramLong);
  
  private native void nativeNotifyOfNetworkConnect(long paramLong, NetworkMonitorAutoDetect.NetworkInformation paramNetworkInformation);
  
  private native void nativeNotifyOfNetworkDisconnect(long paramLong1, long paramLong2);
  
  private native void nativeNotifyOfActiveNetworkList(long paramLong, NetworkMonitorAutoDetect.NetworkInformation[] paramArrayOfNetworkInformation);
}
