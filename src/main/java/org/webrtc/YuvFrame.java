package org.webrtc;


// Some work based on http://stackoverflow.com/a/12702836 by rics (http://stackoverflow.com/users/21047/rics)

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;

// TODO: project should include org.webrtc:google-webrtc from Google
import org.webrtc.VideoFrame;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

// TODO: project should include io.github.zncmn.libyuv:core:0.0.7 from Maven


public class YuvFrame
{
	private static final String TAG = "YuvFrame";
	public int width;
	public int height;
	public byte[] nv21Buffer;
	public int rotationDegree;
	public long timestamp;

	private final Object planeLock = new Object();

	public static final int PROCESSING_NONE = 0x00;
	public static final int PROCESSING_CROP_TO_SQUARE = 0x01;


	// Converts from NV21 format to ARGB format
	//private static final YuvConvert yuvConverter = new YuvConvert();


	/**
	 * Creates a YuvFrame from the provided VideoFrame. Does no processing, and uses the current time as a timestamp.
	 * @param videoFrame Source VideoFrame.
	 */
	@SuppressWarnings("unused")
	public YuvFrame( final VideoFrame videoFrame )
	{
		fromVideoFrame( videoFrame, PROCESSING_NONE, System.nanoTime() );
	}


	/**
	 * Creates a YuvFrame from the provided VideoFrame. Does any processing indicated, and uses the current time as a timestamp.
	 * @param videoFrame Source VideoFrame.
	 * @param processingFlags Processing flags, YuvFrame.PROCESSING_NONE for no processing.
	 */
	@SuppressWarnings("unused")
	public YuvFrame( final VideoFrame videoFrame, final int processingFlags )
	{
		fromVideoFrame( videoFrame, processingFlags, System.nanoTime() );
	}


	/**
	 * Creates a YuvFrame from the provided VideoFrame. Does any processing indicated, and uses the given timestamp.
	 * @param videoFrame Source VideoFrame.
	 * @param processingFlags Processing flags, YuvFrame.PROCESSING_NONE for no processing.
	 * @param timestamp The timestamp to give the frame.
	 */
	public YuvFrame( final VideoFrame videoFrame, final int processingFlags, final long timestamp )
	{
		fromVideoFrame( videoFrame, processingFlags, timestamp );
	}


	/**
	 * Replaces the data in this YuvFrame with the data from the provided frame. Will create new byte arrays to hold pixel data if necessary,
	 * or will reuse existing arrays if they're already the correct size.
	 * @param videoFrame Source VideoFrame.
	 * @param processingFlags Processing flags, YuvFrame.PROCESSING_NONE for no processing.
	 * @param timestamp The timestamp to give the frame.
	 */
	public void fromVideoFrame( final VideoFrame videoFrame, final int processingFlags, final long timestamp )
	{
		if ( videoFrame == null )
		{
			return;
		}

		synchronized ( planeLock )
		{
			try
			{
				// Save timestamp
				this.timestamp = timestamp;

				// Copy rotation information
				rotationDegree = videoFrame.getRotation();  // Just save rotation info for now, doing actual rotation can wait until per-pixel processing.

				// Copy the pixel data, processing as requested.
				if ( PROCESSING_CROP_TO_SQUARE == ( processingFlags & PROCESSING_CROP_TO_SQUARE ) )
				{
					copyPlanesCropped( videoFrame.getBuffer() );
				}
				else
				{
					copyPlanes( videoFrame.getBuffer() );
				}
			}
			catch ( Throwable t )
			{
				dispose();
			}
		}
	}


	public void dispose()
	{
		nv21Buffer = null;
	}


	public boolean hasData()
	{
		return nv21Buffer != null;
	}


	/**
	 * Copy the Y, V, and U planes from the source I420Buffer.
	 * Sets width and height.
	 * @param videoFrameBuffer Source frame buffer.
	 */
	private void copyPlanes( final VideoFrame.Buffer videoFrameBuffer )
	{
		VideoFrame.I420Buffer i420Buffer = null;

		if ( videoFrameBuffer != null )
		{
			i420Buffer = videoFrameBuffer.toI420();
		}

		if ( i420Buffer == null )
		{
			return;
		}

		synchronized ( planeLock )
		{
			// Set the width and height of the frame.
			width = i420Buffer.getWidth();
			height = i420Buffer.getHeight();

			// Calculate sizes needed to convert to NV21 buffer format
			final int size = width * height;
			final int chromaStride = width;
			final int chromaWidth = ( width + 1 ) / 2;
			final int chromaHeight = ( height + 1 ) / 2;
			final int nv21Size = size + chromaStride * chromaHeight;

			if ( nv21Buffer == null || nv21Buffer.length != nv21Size )
			{
				nv21Buffer = new byte[nv21Size];
			}

			final ByteBuffer yPlane = i420Buffer.getDataY();
			final ByteBuffer uPlane = i420Buffer.getDataU();
			final ByteBuffer vPlane = i420Buffer.getDataV();
			final int yStride = i420Buffer.getStrideY();
			final int uStride = i420Buffer.getStrideU();
			final int vStride = i420Buffer.getStrideV();

			// Populate a buffer in NV21 format because that's what the converter wants
			for ( int y = 0; y < height; y++ )
			{
				for ( int x = 0; x < width; x++ )
				{
					nv21Buffer[y * width + x] = yPlane.get( y * yStride + x );
				}
			}

			for ( int y = 0; y < chromaHeight; y++ )
			{
				for ( int x = 0; x < chromaWidth; x++ )
				{
					// Swapping U and V values here because it makes the image the right color

					// Store V
					nv21Buffer[size + y * chromaStride + 2 * x + 1] = uPlane.get( y * uStride + x );

					// Store U
					nv21Buffer[size + y * chromaStride + 2 * x] = vPlane.get( y * vStride + x );
				}
			}
		}
	}


	/**
	 * Copy the Y, V, and U planes from the source I420Buffer, cropping them to square.
	 * Sets width and height.
	 * @param videoFrameBuffer Source frame buffer.
	 */
	private void copyPlanesCropped( final VideoFrame.Buffer videoFrameBuffer )
	{
		if ( videoFrameBuffer == null )
		{
			return;
		}

		synchronized ( planeLock )
		{
			// Verify that the dimensions of the I420Frame are appropriate for cropping
			// If improper dimensions are found, default back to copying the entire frame.
			final int width = videoFrameBuffer.getWidth();
			final int height = videoFrameBuffer.getHeight();

			if ( width > height )
			{
				final VideoFrame.Buffer croppedVideoFrameBuffer = videoFrameBuffer.cropAndScale( ( width - height ) / 2, 0, height, height, height, height );

				copyPlanes( croppedVideoFrameBuffer );

				croppedVideoFrameBuffer.release();
			}
			else
			{
				final VideoFrame.Buffer croppedVideoFrameBuffer = videoFrameBuffer.cropAndScale( 0, ( height - width ) / 2, width, width, width, width );

				copyPlanes( croppedVideoFrameBuffer );

				croppedVideoFrameBuffer.release();
			}
		}
	}


	/**
	 * Converts this YUV frame to an ARGB_8888 Bitmap. Applies stored rotation.
	 * @return A new Bitmap containing the converted frame.
	 */
	public Bitmap getBitmap()
	{

		Log.i(TAG, "getBitmap:nv21Buffer=" + nv21Buffer);
		Log.i(TAG, "getBitmap:width=" + width);
		Log.i(TAG, "getBitmap:height=" + height);
		if ( nv21Buffer == null )
		{
			return null;
		}

		// Calculate the size of the frame
		final int size = width * height;

		// Allocate an array to hold the ARGB pixel data
		final byte[] argbBytes = new byte[size * 4];

		// Use the converter (based on WebRTC source) to change to ARGB format
		toARGB( nv21Buffer, argbBytes, width, height, YuvFormat.NV21 );


		 //Bitmap bitmap = BitmapFactory.decodeByteArray(nv21Buffer, 0, argbBytes.length);
		//String myBase64Image = encodeToBase64(bitmap, Bitmap.CompressFormat.JPEG, 100);

		// Construct a Bitmap based on the new pixel data
		Bitmap bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
		//Bitmap bitmap = Bitmap.createBitmap(BitmapFactory.decodeByteArray(nv21Buffer, 0, nv21Buffer.length));
		//bitmap.reconfigure(width, height, Bitmap.Config.ARGB_8888 );
		bitmap.copyPixelsFromBuffer( ByteBuffer.wrap( argbBytes ) );

		//return BitmapFactory.decodeByteArray(nv21Buffer, 0, nv21Buffer.length);

		// If necessary, generate a rotated version of the Bitmap
		if ( rotationDegree == 90 || rotationDegree == -270 )
		{
			final Matrix m = new Matrix();
			m.postRotate( 90 );

			return Bitmap.createBitmap( bitmap, 0, 0, width, height, m, true );
		}
		else if ( rotationDegree == 180 || rotationDegree == -180 )
		{
			final Matrix m = new Matrix();
			m.postRotate( 180 );

			return Bitmap.createBitmap( bitmap, 0, 0, width, height, m, true );
		}
		else if ( rotationDegree == 270 || rotationDegree == -90 )
		{
			final Matrix m = new Matrix();
			m.postRotate( 270 );

			return Bitmap.createBitmap( bitmap, 0, 0, width, height, m, true );
		}
		else
		{
			// Don't rotate, just return the Bitmap
			return bitmap;
		}
	}

	public static String encodeBitmapToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
	{
		ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
		image.compress(compressFormat, quality, byteArrayOS);
		return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
	}

	public static Bitmap decodeBase64ToBitmap(String base64Bitmap)
	{
		byte[] decodedBytes = Base64.decode(base64Bitmap, 0);
		return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
	}

	public String getBitmapBase64() {
		Bitmap bitmap = getBitmap();
		Log.i(TAG, "getBitmapBase64:bitmap=" + bitmap);
		if (bitmap == null) return null;

		String myBase64Image = encodeBitmapToBase64(bitmap, Bitmap.CompressFormat.JPEG, 100);
		Log.d("Log", "onPictureTaken - wrote bytes: " + myBase64Image);
		return myBase64Image;
	}

	public void toARGB(final byte[] yuv, final byte[] argb, final int width, final int height, final YuvFormat format) {
		if (format == YuvFormat.NV21) {
			Yuv.INSTANCE.nv21ToAbgr(yuv, argb, width, height);
		} else {
			throw new UnsupportedOperationException("not supported format. " + format);
		}
	}
	public static Bitmap convertBitmap(byte[] data, Camera camera) {
		Camera.Size previewSize = camera.getParameters().getPreviewSize();
		YuvImage yuvimage = new YuvImage(
				data,
				camera.getParameters().getPreviewFormat(),
				previewSize.width,
				previewSize.height,
				null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
		byte[] rawImage = baos.toByteArray();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
		Matrix m = new Matrix();
		// 这里我的手机需要旋转一下图像方向才正确，如果在你们的手机上不正确，自己调节，
		// 正式项目中不能这么写，需要计算方向，计算YuvImage方向太麻烦，我这里没有做。
		m.postRotate(90);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
	}

}
