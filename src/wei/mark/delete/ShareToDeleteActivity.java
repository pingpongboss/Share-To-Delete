package wei.mark.delete;

import java.io.File;

import wei.mark.delete.util.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class ShareToDeleteActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();

		boolean finished = true;

		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action)) {

			if (extras.containsKey(Intent.EXTRA_STREAM)) {
				try {
					// Get resource path from intent callee
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

					final String path = getPath(uri);
					final int id = getId(uri);

					boolean prompt = PreferenceManager
							.getDefaultSharedPreferences(this).getBoolean(
									"prompt", false);
					if (prompt) {
						finished = false;

						ImageView image = new ImageView(this);
						Bitmap bitmap = Utils.createThumbnail(
								MediaStore.Images.Media.getBitmap(
										getContentResolver(), uri), 150, 150);
						image.setImageBitmap(bitmap);

						AlertDialog.Builder alert = new AlertDialog.Builder(
								this);
						alert.setTitle("Do you want to delete this file?")
								.setMessage(path)
								.setPositiveButton("Yes",
										new OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												delete(path, id);
												finish();
											}
										})
								.setNegativeButton("Cancel",
										new OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												fail("User cancelled the action.");
												finish();
											}
										}).setView(image)
								.setIcon(android.R.drawable.ic_menu_delete)
								.show();
					} else {
						delete(path, id);
					}
				} catch (Exception e) {
					fail(e.getMessage(), e.getStackTrace());
				}

			} else if (extras.containsKey(Intent.EXTRA_TEXT)) {
				fail("Intent contains EXTRA_TEXT");
			}
		} else {
			startActivity(new Intent(this, ShareToDeletePreferences.class));
		}

		if (finished)
			finish();
	}

	private void fail(String message, StackTraceElement... messages) {
		if (messages.length > 0) {
			for (StackTraceElement m : messages) {
				Log.e(this.getClass().getName(), m.toString());
			}
			Toast.makeText(this, "Failed: " + message.toString(),
					Toast.LENGTH_SHORT).show();
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

	public int getId(Uri uri) {
		String[] projection = { MediaStore.Images.Media._ID };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

		cursor.moveToFirst();
		return cursor.getInt(column_index);
	}

	public void delete(String path, int id) {
		File file = new File(path);
		if (file.canWrite()) {
			if (file.delete()) {
				ContentResolver cr = getContentResolver();
				int deleted = 0;
				if ((deleted = cr.delete(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						BaseColumns._ID + "=" + id, null)) == 1) {
					Toast.makeText(this, "Deleted: " + path, Toast.LENGTH_SHORT)
							.show();
				} else {
					fail("Deleted " + deleted + " rows from Content Provider.");
				}
			} else {
				fail("Failed to delete: " + path);
			}
		} else {
			fail("Cannot write to path: " + path);
		}
	}
}