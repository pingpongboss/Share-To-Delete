package wei.mark.delete.util;

import android.graphics.Bitmap;

public class Utils {

	public static Bitmap createThumbnail(Bitmap imageBitmap, int thumbWidth,
			int thumbHeight) {
		Float width = new Float(imageBitmap.getWidth());
		Float height = new Float(imageBitmap.getHeight());
		Float ratio = width / height;
		return Bitmap.createScaledBitmap(imageBitmap,
				(int) (thumbHeight * ratio), thumbWidth, false);
	}
}