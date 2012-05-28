package wei.mark.delete.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import wei.mark.delete.ShareToDeleteActivity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class Utils {

	private static final int WIDTH = 150, HEIGHT = 150;

	public static Bitmap createThumbnail(Bitmap imageBitmap,
			int thumbnailWidth, int thumbnailHeight) {
		Float width = new Float(imageBitmap.getWidth());
		Float height = new Float(imageBitmap.getHeight());
		Float ratio = width / height;
		return Bitmap.createScaledBitmap(imageBitmap,
				(int) (thumbnailHeight * ratio), thumbnailWidth, false);
	}

	public static Bitmap createThumbnail(Bitmap imageBitmap) {
		return createThumbnail(imageBitmap, WIDTH, HEIGHT);
	}

	public static void fail(String message, StackTraceElement... messages) {
		Log.e("Share To Delete", message);
		if (messages.length > 0) {
			for (StackTraceElement m : messages) {
				Log.e("Share To Delete", m.toString());
			}
		}
	}

	public static void delete(final ShareToDeleteActivity context,
			final String path, final Uri uri, final DeleteCallback callback) {

		boolean prompt = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("prompt", false);
		if (prompt) {

			ImageView image = new ImageView(context);
			Bitmap bitmap;
			try {
				bitmap = Utils.createThumbnail(MediaStore.Images.Media
						.getBitmap(context.getContentResolver(), uri));
				image.setImageBitmap(bitmap);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			AlertDialog.Builder alert = new AlertDialog.Builder(context);
			alert.setTitle("What do you want to do?")
					.setMessage(path)
					.setPositiveButton("Delete", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							callback.done(deleteWithoutPrompt(context, path,
									uri, getId(context, uri)));
							return;
						}
					}).setNegativeButton("Keep", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Utils.fail("User cancelled the action.");
							callback.done(false);
							return;
						}
					}).setView(image)
					.setIcon(android.R.drawable.ic_menu_delete).show();
		} else {
			// no prompt
			callback.done(deleteWithoutPrompt(context, path, uri,
					getId(context, uri)));
		}
	}

	private static boolean deleteWithoutPrompt(ShareToDeleteActivity context,
			String path, Uri uri, int mediaId) {
		File file = new File(path);
		if (file.canWrite()) {
			if (file.delete()) {
				ContentResolver cr = context.getContentResolver();
				int deleted = 0;
				if ((deleted = cr.delete(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						BaseColumns._ID + "=" + mediaId, null)) == 1) {
					Toast.makeText(context, "Deleted: " + path,
							Toast.LENGTH_SHORT).show();
					return true;
				} else {
					Utils.fail("Deleted " + deleted
							+ " rows from Content Provider.");
				}
			} else {
				Utils.fail("Failed to delete: " + path);
			}
		} else {
			Utils.fail("Cannot write to path: " + path);
		}

		return false;
	}

	public static int getId(Context context, Uri uri) {
		String[] projection = { MediaStore.Images.Media._ID };
		Cursor cursor = context.getContentResolver().query(uri, projection,
				null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

		cursor.moveToFirst();
		return cursor.getInt(column_index);
	}

	public interface DeleteCallback {
		void done(boolean success);
	}
}