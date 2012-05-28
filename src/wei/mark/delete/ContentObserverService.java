package wei.mark.delete;

import wei.mark.delete.util.Utils;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class ContentObserverService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// save count. Only react to added images
		PreferenceManager
				.getDefaultSharedPreferences(ContentObserverService.this)
				.edit()
				.putInt("count",
						getContentResolver().query(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								null, null, null, "date_added DESC").getCount())
				.commit();

		getContentResolver().registerContentObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
				new ContentObserver(null) {
					@Override
					public void onChange(boolean selfChange) {
						super.onChange(selfChange);

						Cursor cursor = getContentResolver().query(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								null, null, null, "date_added DESC");
						int count = cursor.getCount();

						int pathColumnIndex = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
						int idColumnIndex = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
						cursor.moveToFirst();

						String path = cursor.getString(pathColumnIndex);
						int id = cursor.getInt(idColumnIndex);

						SharedPreferences prefs = PreferenceManager
								.getDefaultSharedPreferences(ContentObserverService.this);
						int prevCount = prefs.getInt("count", 0);
						prefs.edit().putInt("count", count).commit();

						Log.d("ContentObserverService", "Image count from "
								+ prevCount + " to " + count);

						// don't do anything if no images were added
						if (prevCount >= count) {
							return;
						}

						Bitmap bitmap = Utils.createThumbnail(
								BitmapFactory.decodeFile(path), 48, 48);
						Uri uri = Uri.withAppendedPath(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								Integer.toString(id));

						// tmp
						Intent deleteIntent = new Intent(Intent.ACTION_SEND)
								.setClass(ContentObserverService.this,
										ShareToDeleteActivity.class)
								.putExtra(Intent.EXTRA_STREAM, uri)
								.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(deleteIntent);
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
