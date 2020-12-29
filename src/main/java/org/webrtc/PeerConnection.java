package org.webrtc;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeerConnection {
  public enum IceGatheringState {
    NEW, GATHERING, COMPLETE;
    
    @CalledByNative("IceGatheringState")
    static IceGatheringState fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public enum IceConnectionState {
    NEW, CHECKING, CONNECTED, COMPLETED, FAILED, DISCONNECTED, CLOSED;
    
    @CalledByNative("IceConnectionState")
    static IceConnectionState fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public enum PeerConnectionState {
    NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED;
    
    @CalledByNative("PeerConnectionState")
    static PeerConnectionState fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public enum TlsCertPolicy {
    TLS_CERT_POLICY_SECURE, TLS_CERT_POLICY_INSECURE_NO_CHECK;
  }
  
  public enum SignalingState {
    STABLE, HAVE_LOCAL_OFFER, HAVE_LOCAL_PRANSWER, HAVE_REMOTE_OFFER, HAVE_REMOTE_PRANSWER, CLOSED;
    
    @CalledByNative("SignalingState")
    static SignalingState fromNativeIndex(int nativeIndex) {
      return values()[nativeIndex];
    }
  }
  
  public static interface Observer {
    @CalledByNative("Observer")
    void onSignalingChange(PeerConnection.SignalingState param1SignalingState);
    
    @CalledByNative("Observer")
    void onIceConnectionChange(PeerConnection.IceConnectionState param1IceConnectionState);
    
    @CalledByNative("Observer")
    default void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {}
    
    @CalledByNative("Observer")
    default void onConnectionChange(PeerConnection.PeerConnectionState newState) {}
    
    @CalledByNative("Observer")
    void onIceConnectionReceivingChange(boolean param1Boolean);
    
    @CalledByNative("Observer")
    void onIceGatheringChange(PeerConnection.IceGatheringState param1IceGatheringState);
    
    @CalledByNative("Observer")
    void onIceCandidate(IceCandidate param1IceCandidate);
    
    @CalledByNative("Observer")
    void onIceCandidatesRemoved(IceCandidate[] param1ArrayOfIceCandidate);
    
    @CalledByNative("Observer")
    default void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {}
    
    @CalledByNative("Observer")
    void onAddStream(MediaStream param1MediaStream);
    
    @CalledByNative("Observer")
    void onRemoveStream(MediaStream param1MediaStream);
    
    @CalledByNative("Observer")
    void onDataChannel(DataChannel param1DataChannel);
    
    @CalledByNative("Observer")
    void onRenegotiationNeeded();
    
    @CalledByNative("Observer")
    void onAddTrack(RtpReceiver param1RtpReceiver, MediaStream[] param1ArrayOfMediaStream);
    
    @CalledByNative("Observer")
    default void onTrack(RtpTransceiver transceiver) {}
  }
  
  public static class IceServer {
    @Deprecated
    public final String uri;
    
    public final List<String> urls;
    
    public final String username;
    
    public final String password;
    
    public final PeerConnection.TlsCertPolicy tlsCertPolicy;
    
    public final String hostname;
    
    public final List<String> tlsAlpnProtocols;
    
    public final List<String> tlsEllipticCurves;
    
    @Deprecated
    public IceServer(String uri) {
      this(uri, "", "");
    }
    
    @Deprecated
    public IceServer(String uri, String username, String password) {
      this(uri, username, password, PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE);
    }
    
    @Deprecated
    public IceServer(String uri, String username, String password, PeerConnection.TlsCertPolicy tlsCertPolicy) {
      this(uri, username, password, tlsCertPolicy, "");
    }
    
    @Deprecated
    public IceServer(String uri, String username, String password, PeerConnection.TlsCertPolicy tlsCertPolicy, String hostname) {
      this(uri, Collections.singletonList(uri), username, password, tlsCertPolicy, hostname, null, null);
    }
    
    private IceServer(String uri, List<String> urls, String username, String password, PeerConnection.TlsCertPolicy tlsCertPolicy, String hostname, List<String> tlsAlpnProtocols, List<String> tlsEllipticCurves) {
      if (uri == null || urls == null || urls.isEmpty())
        throw new IllegalArgumentException("uri == null || urls == null || urls.isEmpty()"); 
      for (String it : urls) {
        if (it == null)
          throw new IllegalArgumentException("urls element is null: " + urls); 
      } 
      if (username == null)
        throw new IllegalArgumentException("username == null"); 
      if (password == null)
        throw new IllegalArgumentException("password == null"); 
      if (hostname == null)
        throw new IllegalArgumentException("hostname == null"); 
      this.uri = uri;
      this.urls = urls;
      this.username = username;
      this.password = password;
      this.tlsCertPolicy = tlsCertPolicy;
      this.hostname = hostname;
      this.tlsAlpnProtocols = tlsAlpnProtocols;
      this.tlsEllipticCurves = tlsEllipticCurves;
    }
    
    public String toString() {
      return this.urls + " [" + this.username + ":" + this.password + "] [" + this.tlsCertPolicy + "] [" + this.hostname + "] [" + this.tlsAlpnProtocols + "] [" + this.tlsEllipticCurves + "]";
    }
    
    public boolean equals(@Nullable Object obj) {
      if (obj == null)
        return false; 
      if (obj == this)
        return true; 
      if (!(obj instanceof IceServer))
        return false; 
      IceServer other = (IceServer)obj;
      return (this.uri.equals(other.uri) && this.urls.equals(other.urls) && this.username.equals(other.username) && this.password
        .equals(other.password) && this.tlsCertPolicy.equals(other.tlsCertPolicy) && this.hostname
        .equals(other.hostname) && this.tlsAlpnProtocols.equals(other.tlsAlpnProtocols) && this.tlsEllipticCurves
        .equals(other.tlsEllipticCurves));
    }
    
    public int hashCode() {
      Object[] values = { this.uri, this.urls, this.username, this.password, this.tlsCertPolicy, this.hostname, this.tlsAlpnProtocols, this.tlsEllipticCurves };
      return Arrays.hashCode(values);
    }
    
    public static Builder builder(String uri) {
      return new Builder(Collections.singletonList(uri));
    }
    
    public static Builder builder(List<String> urls) {
      return new Builder(urls);
    }
    
    public static class Builder {
      @Nullable
      private final List<String> urls;
      
      private String username = "";
      
      private String password = "";
      
      private PeerConnection.TlsCertPolicy tlsCertPolicy = PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE;
      
      private String hostname = "";
      
      private List<String> tlsAlpnProtocols;
      
      private List<String> tlsEllipticCurves;
      
      private Builder(List<String> urls) {
        if (urls == null || urls.isEmpty())
          throw new IllegalArgumentException("urls == null || urls.isEmpty(): " + urls); 
        this.urls = urls;
      }
      
      public Builder setUsername(String username) {
        this.username = username;
        return this;
      }
      
      public Builder setPassword(String password) {
        this.password = password;
        return this;
      }
      
      public Builder setTlsCertPolicy(PeerConnection.TlsCertPolicy tlsCertPolicy) {
        this.tlsCertPolicy = tlsCertPolicy;
        return this;
      }
      
      public Builder setHostname(String hostname) {
        this.hostname = hostname;
        return this;
      }
      
      public Builder setTlsAlpnProtocols(List<String> tlsAlpnProtocols) {
        this.tlsAlpnProtocols = tlsAlpnProtocols;
        return this;
      }
      
      public Builder setTlsEllipticCurves(List<String> tlsEllipticCurves) {
        this.tlsEllipticCurves = tlsEllipticCurves;
        return this;
      }
      
      public PeerConnection.IceServer createIceServer() {
        return new PeerConnection.IceServer(this.urls.get(0), this.urls, this.username, this.password, this.tlsCertPolicy, this.hostname, this.tlsAlpnProtocols, this.tlsEllipticCurves);
      }
    }
    
    @Nullable
    @CalledByNative("IceServer")
    List<String> getUrls() {
      return this.urls;
    }
    
    @Nullable
    @CalledByNative("IceServer")
    String getUsername() {
      return this.username;
    }
    
    @Nullable
    @CalledByNative("IceServer")
    String getPassword() {
      return this.password;
    }
    
    @CalledByNative("IceServer")
    PeerConnection.TlsCertPolicy getTlsCertPolicy() {
      return this.tlsCertPolicy;
    }
    
    @Nullable
    @CalledByNative("IceServer")
    String getHostname() {
      return this.hostname;
    }
    
    @CalledByNative("IceServer")
    List<String> getTlsAlpnProtocols() {
      return this.tlsAlpnProtocols;
    }
    
    @CalledByNative("IceServer")
    List<String> getTlsEllipticCurves() {
      return this.tlsEllipticCurves;
    }
  }
  
  public enum IceTransportsType {
    NONE, RELAY, NOHOST, ALL;
  }
  
  public enum BundlePolicy {
    BALANCED, MAXBUNDLE, MAXCOMPAT;
  }
  
  public enum RtcpMuxPolicy {
    NEGOTIATE, REQUIRE;
  }
  
  public enum TcpCandidatePolicy {
    ENABLED, DISABLED;
  }
  
  public enum CandidateNetworkPolicy {
    ALL, LOW_COST;
  }
  
  public enum AdapterType {
    UNKNOWN(0),
    ETHERNET(1 << 0),
    WIFI(1 << 1),
    CELLULAR(1 << 2),
    VPN(1 << 3),
    LOOPBACK(1 << 4),
    ADAPTER_TYPE_ANY(1 << 5),
    CELLULAR_2G(1 << 6),
    CELLULAR_3G(1 << 7),
    CELLULAR_4G(1 << 8),
    CELLULAR_5G(1 << 9);

    public final Integer bitMask;
    
    private static final Map<Integer, AdapterType> BY_BITMASK = new HashMap<>();
    
    AdapterType(Integer bitMask) {
      this.bitMask = bitMask;
    }
    
    static {
      for (AdapterType t : values())
        BY_BITMASK.put(t.bitMask, t); 
    }
    
    @Nullable
    @CalledByNative("AdapterType")
    static AdapterType fromNativeIndex(int nativeIndex) {
      return BY_BITMASK.get(Integer.valueOf(nativeIndex));
    }
  }
  
  public enum KeyType {
    RSA, ECDSA;
  }
  
  public enum ContinualGatheringPolicy {
    GATHER_ONCE, GATHER_CONTINUALLY;
  }
  
  public enum PortPrunePolicy {
    NO_PRUNE, PRUNE_BASED_ON_PRIORITY, KEEP_FIRST_READY;
  }
  
  public enum SdpSemantics {
    PLAN_B, UNIFIED_PLAN;
  }
  
  public static class RTCConfiguration {
    public PeerConnection.IceTransportsType iceTransportsType;
    
    public List<PeerConnection.IceServer> iceServers;
    
    public PeerConnection.BundlePolicy bundlePolicy;
    
    @Nullable
    public RtcCertificatePem certificate;
    
    public PeerConnection.RtcpMuxPolicy rtcpMuxPolicy;
    
    public PeerConnection.TcpCandidatePolicy tcpCandidatePolicy;
    
    public PeerConnection.CandidateNetworkPolicy candidateNetworkPolicy;
    
    public int audioJitterBufferMaxPackets;
    
    public boolean audioJitterBufferFastAccelerate;
    
    public int iceConnectionReceivingTimeout;
    
    public int iceBackupCandidatePairPingInterval;
    
    public PeerConnection.KeyType keyType;
    
    public PeerConnection.ContinualGatheringPolicy continualGatheringPolicy;
    
    public int iceCandidatePoolSize;
    
    @Deprecated
    public boolean pruneTurnPorts;
    
    public PeerConnection.PortPrunePolicy turnPortPrunePolicy;
    
    public boolean presumeWritableWhenFullyRelayed;
    
    public boolean surfaceIceCandidatesOnIceTransportTypeChanged;
    
    @Nullable
    public Integer iceCheckIntervalStrongConnectivityMs;
    
    @Nullable
    public Integer iceCheckIntervalWeakConnectivityMs;
    
    @Nullable
    public Integer iceCheckMinInterval;
    
    @Nullable
    public Integer iceUnwritableTimeMs;
    
    @Nullable
    public Integer iceUnwritableMinChecks;
    
    @Nullable
    public Integer stunCandidateKeepaliveIntervalMs;
    
    public boolean disableIPv6OnWifi;
    
    public int maxIPv6Networks;
    
    public boolean disableIpv6;
    
    public boolean enableDscp;
    
    public boolean enableCpuOveruseDetection;
    
    public boolean enableRtpDataChannel;
    
    public boolean suspendBelowMinBitrate;
    
    @Nullable
    public Integer screencastMinBitrate;
    
    @Nullable
    public Boolean combinedAudioVideoBwe;
    
    @Nullable
    public Boolean enableDtlsSrtp;
    
    public PeerConnection.AdapterType networkPreference;
    
    public PeerConnection.SdpSemantics sdpSemantics;
    
    @Nullable
    public TurnCustomizer turnCustomizer;
    
    public boolean activeResetSrtpParams;
    
    @Nullable
    public Boolean allowCodecSwitching;
    
    public boolean useMediaTransport;
    
    public boolean useMediaTransportForDataChannels;
    
    @Nullable
    public CryptoOptions cryptoOptions;
    
    @Nullable
    public String turnLoggingId;
    
    public RTCConfiguration(List<PeerConnection.IceServer> iceServers) {
      this.iceTransportsType = PeerConnection.IceTransportsType.ALL;
      this.bundlePolicy = PeerConnection.BundlePolicy.BALANCED;
      this.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
      this.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
      this.candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL;
      this.iceServers = iceServers;
      this.audioJitterBufferMaxPackets = 50;
      this.audioJitterBufferFastAccelerate = false;
      this.iceConnectionReceivingTimeout = -1;
      this.iceBackupCandidatePairPingInterval = -1;
      this.keyType = PeerConnection.KeyType.ECDSA;
      this.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE;
      this.iceCandidatePoolSize = 0;
      this.pruneTurnPorts = false;
      this.turnPortPrunePolicy = PeerConnection.PortPrunePolicy.NO_PRUNE;
      this.presumeWritableWhenFullyRelayed = false;
      this.surfaceIceCandidatesOnIceTransportTypeChanged = false;
      this.iceCheckIntervalStrongConnectivityMs = null;
      this.iceCheckIntervalWeakConnectivityMs = null;
      this.iceCheckMinInterval = null;
      this.iceUnwritableTimeMs = null;
      this.iceUnwritableMinChecks = null;
      this.stunCandidateKeepaliveIntervalMs = null;
      this.disableIPv6OnWifi = false;
      this.maxIPv6Networks = 5;
      this.disableIpv6 = false;
      this.enableDscp = false;
      this.enableCpuOveruseDetection = true;
      this.enableRtpDataChannel = false;
      this.suspendBelowMinBitrate = false;
      this.screencastMinBitrate = null;
      this.combinedAudioVideoBwe = null;
      this.enableDtlsSrtp = null;
      this.networkPreference = PeerConnection.AdapterType.UNKNOWN;
      this.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B;
      this.activeResetSrtpParams = false;
      this.useMediaTransport = false;
      this.useMediaTransportForDataChannels = false;
      this.cryptoOptions = null;
      this.turnLoggingId = null;
      this.allowCodecSwitching = null;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.IceTransportsType getIceTransportsType() {
      return this.iceTransportsType;
    }
    
    @CalledByNative("RTCConfiguration")
    List<PeerConnection.IceServer> getIceServers() {
      return this.iceServers;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.BundlePolicy getBundlePolicy() {
      return this.bundlePolicy;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.PortPrunePolicy getTurnPortPrunePolicy() {
      return this.turnPortPrunePolicy;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    RtcCertificatePem getCertificate() {
      return this.certificate;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.RtcpMuxPolicy getRtcpMuxPolicy() {
      return this.rtcpMuxPolicy;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.TcpCandidatePolicy getTcpCandidatePolicy() {
      return this.tcpCandidatePolicy;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.CandidateNetworkPolicy getCandidateNetworkPolicy() {
      return this.candidateNetworkPolicy;
    }
    
    @CalledByNative("RTCConfiguration")
    int getAudioJitterBufferMaxPackets() {
      return this.audioJitterBufferMaxPackets;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getAudioJitterBufferFastAccelerate() {
      return this.audioJitterBufferFastAccelerate;
    }
    
    @CalledByNative("RTCConfiguration")
    int getIceConnectionReceivingTimeout() {
      return this.iceConnectionReceivingTimeout;
    }
    
    @CalledByNative("RTCConfiguration")
    int getIceBackupCandidatePairPingInterval() {
      return this.iceBackupCandidatePairPingInterval;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.KeyType getKeyType() {
      return this.keyType;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.ContinualGatheringPolicy getContinualGatheringPolicy() {
      return this.continualGatheringPolicy;
    }
    
    @CalledByNative("RTCConfiguration")
    int getIceCandidatePoolSize() {
      return this.iceCandidatePoolSize;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getPruneTurnPorts() {
      return this.pruneTurnPorts;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getPresumeWritableWhenFullyRelayed() {
      return this.presumeWritableWhenFullyRelayed;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getSurfaceIceCandidatesOnIceTransportTypeChanged() {
      return this.surfaceIceCandidatesOnIceTransportTypeChanged;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getIceCheckIntervalStrongConnectivity() {
      return this.iceCheckIntervalStrongConnectivityMs;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getIceCheckIntervalWeakConnectivity() {
      return this.iceCheckIntervalWeakConnectivityMs;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getIceCheckMinInterval() {
      return this.iceCheckMinInterval;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getIceUnwritableTimeout() {
      return this.iceUnwritableTimeMs;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getIceUnwritableMinChecks() {
      return this.iceUnwritableMinChecks;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getStunCandidateKeepaliveInterval() {
      return this.stunCandidateKeepaliveIntervalMs;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getDisableIPv6OnWifi() {
      return this.disableIPv6OnWifi;
    }
    
    @CalledByNative("RTCConfiguration")
    int getMaxIPv6Networks() {
      return this.maxIPv6Networks;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    TurnCustomizer getTurnCustomizer() {
      return this.turnCustomizer;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getDisableIpv6() {
      return this.disableIpv6;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getEnableDscp() {
      return this.enableDscp;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getEnableCpuOveruseDetection() {
      return this.enableCpuOveruseDetection;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getEnableRtpDataChannel() {
      return this.enableRtpDataChannel;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getSuspendBelowMinBitrate() {
      return this.suspendBelowMinBitrate;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Integer getScreencastMinBitrate() {
      return this.screencastMinBitrate;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Boolean getCombinedAudioVideoBwe() {
      return this.combinedAudioVideoBwe;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Boolean getEnableDtlsSrtp() {
      return this.enableDtlsSrtp;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.AdapterType getNetworkPreference() {
      return this.networkPreference;
    }
    
    @CalledByNative("RTCConfiguration")
    PeerConnection.SdpSemantics getSdpSemantics() {
      return this.sdpSemantics;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getActiveResetSrtpParams() {
      return this.activeResetSrtpParams;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    Boolean getAllowCodecSwitching() {
      return this.allowCodecSwitching;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getUseMediaTransport() {
      return this.useMediaTransport;
    }
    
    @CalledByNative("RTCConfiguration")
    boolean getUseMediaTransportForDataChannels() {
      return this.useMediaTransportForDataChannels;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    CryptoOptions getCryptoOptions() {
      return this.cryptoOptions;
    }
    
    @Nullable
    @CalledByNative("RTCConfiguration")
    String getTurnLoggingId() {
      return this.turnLoggingId;
    }
  }
  
  private final List<MediaStream> localStreams = new ArrayList<>();
  
  private final long nativePeerConnection;
  
  private List<RtpSender> senders = new ArrayList<>();
  
  private List<RtpReceiver> receivers = new ArrayList<>();
  
  private List<RtpTransceiver> transceivers = new ArrayList<>();
  
  public PeerConnection(NativePeerConnectionFactory factory) {
    this(factory.createNativePeerConnection());
  }
  
  PeerConnection(long nativePeerConnection) {
    this.nativePeerConnection = nativePeerConnection;
  }
  
  public SessionDescription getLocalDescription() {
    return nativeGetLocalDescription();
  }
  
  public SessionDescription getRemoteDescription() {
    return nativeGetRemoteDescription();
  }
  
  public RtcCertificatePem getCertificate() {
    return nativeGetCertificate();
  }
  
  public DataChannel createDataChannel(String label, DataChannel.Init init) {
    return nativeCreateDataChannel(label, init);
  }
  
  public void createOffer(SdpObserver observer, MediaConstraints constraints) {
    nativeCreateOffer(observer, constraints);
  }
  
  public void createAnswer(SdpObserver observer, MediaConstraints constraints) {
    nativeCreateAnswer(observer, constraints);
  }
  
  public void setLocalDescription(SdpObserver observer, SessionDescription sdp) {
    nativeSetLocalDescription(observer, sdp);
  }
  
  public void setRemoteDescription(SdpObserver observer, SessionDescription sdp) {
    nativeSetRemoteDescription(observer, sdp);
  }
  
  public void setAudioPlayout(boolean playout) {
    nativeSetAudioPlayout(playout);
  }
  
  public void setAudioRecording(boolean recording) {
    nativeSetAudioRecording(recording);
  }
  
  public boolean setConfiguration(RTCConfiguration config) {
    return nativeSetConfiguration(config);
  }
  
  public boolean addIceCandidate(IceCandidate candidate) {
    return nativeAddIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
  }
  
  public boolean removeIceCandidates(IceCandidate[] candidates) {
    return nativeRemoveIceCandidates(candidates);
  }
  
  public boolean addStream(MediaStream stream) {
    boolean ret = nativeAddLocalStream(stream.getNativeMediaStream());
    if (!ret)
      return false; 
    this.localStreams.add(stream);
    return true;
  }
  
  public void removeStream(MediaStream stream) {
    nativeRemoveLocalStream(stream.getNativeMediaStream());
    this.localStreams.remove(stream);
  }
  
  public RtpSender createSender(String kind, String stream_id) {
    RtpSender newSender = nativeCreateSender(kind, stream_id);
    if (newSender != null)
      this.senders.add(newSender); 
    return newSender;
  }
  
  public List<RtpSender> getSenders() {
    for (RtpSender sender : this.senders)
      sender.dispose(); 
    this.senders = nativeGetSenders();
    return Collections.unmodifiableList(this.senders);
  }
  
  public List<RtpReceiver> getReceivers() {
    for (RtpReceiver receiver : this.receivers)
      receiver.dispose(); 
    this.receivers = nativeGetReceivers();
    return Collections.unmodifiableList(this.receivers);
  }
  
  public List<RtpTransceiver> getTransceivers() {
    for (RtpTransceiver transceiver : this.transceivers)
      transceiver.dispose(); 
    this.transceivers = nativeGetTransceivers();
    return Collections.unmodifiableList(this.transceivers);
  }
  
  public RtpSender addTrack(MediaStreamTrack track) {
    return addTrack(track, Collections.emptyList());
  }
  
  public RtpSender addTrack(MediaStreamTrack track, List<String> streamIds) {
    if (track == null || streamIds == null)
      throw new NullPointerException("No MediaStreamTrack specified in addTrack."); 
    RtpSender newSender = nativeAddTrack(track.getNativeMediaStreamTrack(), streamIds);
    if (newSender == null)
      throw new IllegalStateException("C++ addTrack failed."); 
    this.senders.add(newSender);
    return newSender;
  }
  
  public boolean removeTrack(RtpSender sender) {
    if (sender == null)
      throw new NullPointerException("No RtpSender specified for removeTrack."); 
    return nativeRemoveTrack(sender.getNativeRtpSender());
  }
  
  public RtpTransceiver addTransceiver(MediaStreamTrack track) {
    return addTransceiver(track, new RtpTransceiver.RtpTransceiverInit());
  }
  
  public RtpTransceiver addTransceiver(MediaStreamTrack track, @Nullable RtpTransceiver.RtpTransceiverInit init) {
    if (track == null)
      throw new NullPointerException("No MediaStreamTrack specified for addTransceiver."); 
    if (init == null)
      init = new RtpTransceiver.RtpTransceiverInit(); 
    RtpTransceiver newTransceiver = nativeAddTransceiverWithTrack(track.getNativeMediaStreamTrack(), init);
    if (newTransceiver == null)
      throw new IllegalStateException("C++ addTransceiver failed."); 
    this.transceivers.add(newTransceiver);
    return newTransceiver;
  }
  
  public RtpTransceiver addTransceiver(MediaStreamTrack.MediaType mediaType) {
    return addTransceiver(mediaType, new RtpTransceiver.RtpTransceiverInit());
  }
  
  public RtpTransceiver addTransceiver(MediaStreamTrack.MediaType mediaType, @Nullable RtpTransceiver.RtpTransceiverInit init) {
    if (mediaType == null)
      throw new NullPointerException("No MediaType specified for addTransceiver."); 
    if (init == null)
      init = new RtpTransceiver.RtpTransceiverInit(); 
    RtpTransceiver newTransceiver = nativeAddTransceiverOfType(mediaType, init);
    if (newTransceiver == null)
      throw new IllegalStateException("C++ addTransceiver failed."); 
    this.transceivers.add(newTransceiver);
    return newTransceiver;
  }
  
  @Deprecated
  public boolean getStats(StatsObserver observer, @Nullable MediaStreamTrack track) {
    return nativeOldGetStats(observer, (track == null) ? 0L : track.getNativeMediaStreamTrack());
  }
  
  public void getStats(RTCStatsCollectorCallback callback) {
    nativeNewGetStats(callback);
  }
  
  public boolean setBitrate(Integer min, Integer current, Integer max) {
    return nativeSetBitrate(min, current, max);
  }
  
  public boolean startRtcEventLog(int file_descriptor, int max_size_bytes) {
    return nativeStartRtcEventLog(file_descriptor, max_size_bytes);
  }
  
  public void stopRtcEventLog() {
    nativeStopRtcEventLog();
  }
  
  public SignalingState signalingState() {
    return nativeSignalingState();
  }
  
  public IceConnectionState iceConnectionState() {
    return nativeIceConnectionState();
  }
  
  public PeerConnectionState connectionState() {
    return nativeConnectionState();
  }
  
  public IceGatheringState iceGatheringState() {
    return nativeIceGatheringState();
  }
  
  public void close() {
    nativeClose();
  }
  
  public void dispose() {
    close();
    for (MediaStream stream : this.localStreams) {
      nativeRemoveLocalStream(stream.getNativeMediaStream());
      stream.dispose();
    } 
    this.localStreams.clear();
    for (RtpSender sender : this.senders)
      sender.dispose(); 
    this.senders.clear();
    for (RtpReceiver receiver : this.receivers)
      receiver.dispose(); 
    for (RtpTransceiver transceiver : this.transceivers)
      transceiver.dispose(); 
    this.transceivers.clear();
    this.receivers.clear();
    nativeFreeOwnedPeerConnection(this.nativePeerConnection);
  }
  
  public long getNativePeerConnection() {
    return nativeGetNativePeerConnection();
  }
  
  @CalledByNative
  long getNativeOwnedPeerConnection() {
    return this.nativePeerConnection;
  }
  
  public static long createNativePeerConnectionObserver(Observer observer) {
    return nativeCreatePeerConnectionObserver(observer);
  }
  
  private native long nativeGetNativePeerConnection();
  
  private native SessionDescription nativeGetLocalDescription();
  
  private native SessionDescription nativeGetRemoteDescription();
  
  private native RtcCertificatePem nativeGetCertificate();
  
  private native DataChannel nativeCreateDataChannel(String paramString, DataChannel.Init paramInit);
  
  private native void nativeCreateOffer(SdpObserver paramSdpObserver, MediaConstraints paramMediaConstraints);
  
  private native void nativeCreateAnswer(SdpObserver paramSdpObserver, MediaConstraints paramMediaConstraints);
  
  private native void nativeSetLocalDescription(SdpObserver paramSdpObserver, SessionDescription paramSessionDescription);
  
  private native void nativeSetRemoteDescription(SdpObserver paramSdpObserver, SessionDescription paramSessionDescription);
  
  private native void nativeSetAudioPlayout(boolean paramBoolean);
  
  private native void nativeSetAudioRecording(boolean paramBoolean);
  
  private native boolean nativeSetBitrate(Integer paramInteger1, Integer paramInteger2, Integer paramInteger3);
  
  private native SignalingState nativeSignalingState();
  
  private native IceConnectionState nativeIceConnectionState();
  
  private native PeerConnectionState nativeConnectionState();
  
  private native IceGatheringState nativeIceGatheringState();
  
  private native void nativeClose();
  
  private static native long nativeCreatePeerConnectionObserver(Observer paramObserver);
  
  private static native void nativeFreeOwnedPeerConnection(long paramLong);
  
  private native boolean nativeSetConfiguration(RTCConfiguration paramRTCConfiguration);
  
  private native boolean nativeAddIceCandidate(String paramString1, int paramInt, String paramString2);
  
  private native boolean nativeRemoveIceCandidates(IceCandidate[] paramArrayOfIceCandidate);
  
  private native boolean nativeAddLocalStream(long paramLong);
  
  private native void nativeRemoveLocalStream(long paramLong);
  
  private native boolean nativeOldGetStats(StatsObserver paramStatsObserver, long paramLong);
  
  private native void nativeNewGetStats(RTCStatsCollectorCallback paramRTCStatsCollectorCallback);
  
  private native RtpSender nativeCreateSender(String paramString1, String paramString2);
  
  private native List<RtpSender> nativeGetSenders();
  
  private native List<RtpReceiver> nativeGetReceivers();
  
  private native List<RtpTransceiver> nativeGetTransceivers();
  
  private native RtpSender nativeAddTrack(long paramLong, List<String> paramList);
  
  private native boolean nativeRemoveTrack(long paramLong);
  
  private native RtpTransceiver nativeAddTransceiverWithTrack(long paramLong, RtpTransceiver.RtpTransceiverInit paramRtpTransceiverInit);
  
  private native RtpTransceiver nativeAddTransceiverOfType(MediaStreamTrack.MediaType paramMediaType, RtpTransceiver.RtpTransceiverInit paramRtpTransceiverInit);
  
  private native boolean nativeStartRtcEventLog(int paramInt1, int paramInt2);
  
  private native void nativeStopRtcEventLog();
}
