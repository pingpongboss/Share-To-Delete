package wei.mark.delete;

import wei.mark.delete.util.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class ContentObserverService extends Service {
	private static final int DELETE_NOTIFICATION_ID = 1;

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

						int idColumnIndex = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
						cursor.moveToFirst();

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

						Uri uri = Uri.withAppendedPath(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								Integer.toString(id));

						String alert = prefs.getString("alert", "none");
						if ("popup".equals(alert)) {
							// popup

							Intent deleteIntent = getDeleteIntent(uri, true);
							startActivity(deleteIntent);
						} else if ("notification".equals(alert)) {
							// notification

							int icon = android.R.drawable.ic_menu_delete;
							long when = System.currentTimeMillis();
							Context c = getApplicationContext();
							String contentTitle = "Found New Image";
							String contentText = "Click to delete the image.";
							String tickerText = String.format("%s: %s",
									contentTitle, contentText);
							Intent notificationIntent = getDeleteIntent(uri,
									false);
							PendingIntent contentIntent = PendingIntent
									.getActivity(ContentObserverService.this,
											0, notificationIntent,
											// flag updates any existing
											// notification
											PendingIntent.FLAG_UPDATE_CURRENT);

							Notification notification = new Notification(icon,
									tickerText, when);
							notification.setLatestEventInfo(c, contentTitle,
									contentText, contentIntent);
							notification.flags = notification.flags
									| Notification.FLAG_AUTO_CANCEL;

							NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							mNotificationManager.notify(DELETE_NOTIFICATION_ID,
									notification);
						}
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

	private Intent getDeleteIntent(Uri uri, boolean prompt) {
		return new Intent(Intent.ACTION_SEND)
				.setClass(this, ShareToDeleteActivity.class)
				.putExtra(Intent.EXTRA_STREAM, uri).putExtra("prompt", prompt)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	}
}
