package wei.mark.delete;

import java.io.File;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class ShareToDeleteActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();

		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action)) {
			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				try {
					// Get resource path from intent callee
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

					String path = getPath(uri);

					File file = new File(path);
					if (file.canWrite()) {
						if (file.delete()) {
							ContentResolver cr = getContentResolver();
							int deleted = 0;
							if ((deleted = cr
									.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
											BaseColumns._ID + "=" + getId(uri),
											null)) == 1) {
								Toast.makeText(this, "Deleted: " + path,
										Toast.LENGTH_SHORT).show();
							} else {
								fail("Deleted " + deleted
										+ " rows from Content Provider.");
							}
						} else {
							fail("Failed to delete: " + path);
						}
					} else {
						fail("Cannot write to path: " + path);
					}
				} catch (Exception e) {
					fail(e.toString());
				}

			} else if (extras.containsKey(Intent.EXTRA_TEXT)) {
				fail("Intent contains EXTRA_TEXT");
			}
		} else {
			// launch Settings
		}

		finish();
	}

	private void fail(String message) {
		Log.e(this.getClass().getName(), message);
		Toast.makeText(this, "Failed: " + message, Toast.LENGTH_SHORT).show();
	}

	// http://stackoverflow.com/questions/5548645/get-thumbnail-uri-path-of-the-image-stored-in-sd-card-android
	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	public int getId(Uri uri) {
		String[] projection = { MediaStore.Images.Media._ID };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

		cursor.moveToFirst();
		return cursor.getInt(column_index);
	}
}