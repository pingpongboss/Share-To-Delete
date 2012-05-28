package wei.mark.delete;

import wei.mark.delete.util.Utils;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.provider.MediaStore;

public class ContentObserverService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		getContentResolver().registerContentObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
				new ContentObserver(null) {
					@Override
					public void onChange(boolean selfChange) {
						super.onChange(selfChange);

						Cursor cursor = getContentResolver().query(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								null, null, null, "date_added DESC");

						int pathColumnIndex = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
						int idColumnIndex = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
						cursor.moveToFirst();

						String path = cursor.getString(pathColumnIndex);
						int id = cursor.getInt(idColumnIndex);

						Bitmap bitmap = Utils.createThumbnail(
								BitmapFactory.decodeFile(path), 48, 48);
					}
				});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Utils.fail("onDestroy");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
}
