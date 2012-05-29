package wei.mark.delete;

import java.util.Timer;
import java.util.TimerTask;

import wei.mark.delete.util.Utils;
import wei.mark.delete.util.Utils.DeleteCallback;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

public class ContentObserverService extends Service {
	private static final int DELETE_NOTIFICATION_ID = 1;

	private static final int SHOW_POPUP_ID = 1;

	WindowManager mWindowManager;
	ViewGroup mView;

	ActivityManager mActivityManager;
	String mCameraApp;
	Timer mTimer;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SHOW_POPUP_ID:
					Uri uri = (Uri) msg.obj;

					showViews(uri);
					break;
			}
		};
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		createViews();
		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

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

						if (cursor.moveToFirst()) {
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

							Uri uri = Uri
									.withAppendedPath(
											MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
											Integer.toString(id));

							String alert = prefs.getString("alert", "none");
							if ("overlay".equals(alert)) {
								// show overlay

								Message msg = Message.obtain();
								msg.setTarget(mHandler);
								msg.what = SHOW_POPUP_ID;
								msg.obj = uri;
								msg.sendToTarget();
							} else if ("notification".equals(alert)) {
								// notification

								int icon = android.R.drawable.ic_menu_delete;
								long when = System.currentTimeMillis();
								Context c = getApplicationContext();
								String contentTitle = "Found New Image";
								String contentText = "Click to delete the image.";
								String tickerText = String.format("%s: %s",
										contentTitle, contentText);
								Intent notificationIntent = getDeleteIntent(uri);
								PendingIntent contentIntent = PendingIntent
										.getService(
												ContentObserverService.this, 0,
												notificationIntent,
												// flag updates any existing
												// notification
												PendingIntent.FLAG_UPDATE_CURRENT);

								Notification notification = new Notification(
										icon, tickerText, when);
								notification.setLatestEventInfo(c,
										contentTitle, contentText,
										contentIntent);
								notification.flags = notification.flags
										| Notification.FLAG_AUTO_CANCEL;

								NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								mNotificationManager.notify(
										DELETE_NOTIFICATION_ID, notification);

							}
						}
					}
				});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		hideViews();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (intent != null) {
			String action = intent.getStringExtra("action");
			Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if ("delete".equals(action)) {
				delete(uri);
			}
		}

		// save count. Only react to added images
		PreferenceManager
				.getDefaultSharedPreferences(ContentObserverService.this)
				.edit()
				.putInt("count",
						getContentResolver().query(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								null, null, null, "date_added DESC").getCount())
				.commit();

		return START_STICKY;
	}

	private Intent getDeleteIntent(Uri uri) {
		return new Intent(this, ContentObserverService.class).putExtra(
				Intent.EXTRA_STREAM, uri).putExtra("action", "delete");
	}

	private void createViews() {
		int margins = 4;

		ImageView image = new ImageView(ContentObserverService.this);
		image.setImageResource(android.R.drawable.ic_menu_delete);
		image.setBackgroundColor(Color.argb((int) (255 / 1.5), 0, 0, 0));

		mView = new FrameLayout(ContentObserverService.this) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					Uri uri = (Uri) getTag();
					delete(uri);
				}
				return true;
			}
		};
		mView.setBackgroundColor(Color.argb((int) (255 / 2.5), 255, 255, 255));

		FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		imageParams.setMargins(margins, margins, margins, margins);
		mView.addView(image, imageParams);
	}

	private void showViews(Uri uri) {
		int gravity = Gravity.BOTTOM | Gravity.RIGHT;
		int x = 8;
		int y = 8;
		int delay = 1000;

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		params.gravity = gravity;
		params.x = x;
		params.y = y;

		mView.setTag(uri);

		mWindowManager.addView(mView, params);

		// set up timer to detect when user exists Camera app
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		mCameraApp = mActivityManager.getRunningTasks(1).get(0).topActivity
				.getClassName();
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (!mCameraApp.equals(mActivityManager.getRunningTasks(1).get(
						0).topActivity.getClassName())) {
					hideViews();
				}
			}
		}, delay, delay);
	}

	private void hideViews() {
		try {
			mWindowManager.removeView(mView);
		} catch (Exception ex) {
			// avoid crashing when we hideViews() from notification
		}
		
		// cancel timer
		mTimer.cancel();
	}

	private void delete(Uri uri) {
		hideViews();

		boolean prompt = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("prompt", false);
		if (prompt) {
			// prompt needs activity. Not recommended in Preferences
			startActivity(new Intent(this, ShareToDeleteActivity.class)
					.setAction(Intent.ACTION_SEND)
					.putExtra(Intent.EXTRA_STREAM, uri)
					.putExtra("action", "delete")
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} else {
			// no prompt. fast delete
			Utils.delete(this, uri, new DeleteCallback() {

				@Override
				public void done(boolean success) {
					if (success) {
						Toast.makeText(ContentObserverService.this,
								"Deleted successfully.", Toast.LENGTH_SHORT)
								.show();
					}

					boolean refresh = PreferenceManager
							.getDefaultSharedPreferences(
									ContentObserverService.this).getBoolean(
									"refresh", false);
					if (refresh) {
						// workaround for Camera app that doesn't auto refresh
						startActivity(new Intent(ContentObserverService.this,
								ShareToDeleteActivity.class)
								.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
					}
				}
			});
		}
	}
}
