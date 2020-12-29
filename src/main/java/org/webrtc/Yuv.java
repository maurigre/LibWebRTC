package org.webrtc;


public final class Yuv {
    public static final Yuv INSTANCE = new Yuv();

    static {
        System.loadLibrary("yuv");
    }

    /* package */ native void abgrToNv21(byte[] abgr, byte[] nv21, int width, int height);
    /* package */ native void nv21ToAbgr(byte[] nv21, byte[] abgr, int width, int height);
    /* package */ native void nv21Rotate(byte[] src, byte[] dest, int width, int height, int degrees);
}