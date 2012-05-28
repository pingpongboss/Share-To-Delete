package wei.mark.delete;

import wei.mark.delete.util.Utils;
import wei.mark.delete.util.Utils.DeleteCallback;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

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
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
					final String path = getPath(uri);

					Utils.delete(this, path, uri, new DeleteCallback() {

						@Override
						public void done(boolean success) {
							finish();
						}
					});
				} catch (Exception e) {
					Utils.fail(e.getMessage(), e.getStackTrace());
					finish();
				}

			} else if (extras.containsKey(Intent.EXTRA_TEXT)) {
				Utils.fail("Intent contains EXTRA_TEXT");
				finish();
			}
		} else {
			// started from the launcher
			startActivity(new Intent(this, ShareToDeletePreferences.class));
			finish();
		}
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
}