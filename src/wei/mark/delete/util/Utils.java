package wei.mark.delete.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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

public class Utils {

	private static final int WIDTH = 150, HEIGHT = 150;

	public static Bitmap createThumbnail(Bitmap imageBitmap,
			int thumbnailWidth, int thumbnailHeight) {
		if (imageBitmap == null)
			return null;

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

	public static void delete(final Context context, final Uri uri,
			final DeleteCallback callback) {
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
					.setMessage(getPath(context, uri))
					.setPositiveButton("Delete", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							callback.done(deleteWithoutPrompt(context, uri));
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
			callback.done(deleteWithoutPrompt(context, uri));
		}
	}

	private static boolean deleteWithoutPrompt(Context context, Uri uri) {
		String path = getPath(context, uri);
		if (path != null) {
			File file = new File(path);
			if (file.canWrite()) {
				if (file.delete()) {
					ContentResolver cr = context.getContentResolver();
					int deleted = 0;
					int id;
					try {
						id = getId(context, uri);
					} catch (ShareToDeleteException e) {
						Utils.fail("Deleted "
								+ deleted
								+ " from storage but not from Content Provider.");
						return false;
					}
					if ((deleted = cr.delete(
							MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							BaseColumns._ID + "=" + id, null)) == 1) {
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
		} else {
			Utils.fail("File not found: " + path);
		}

		return false;
	}

	public static int getId(Context context, Uri uri)
			throws ShareToDeleteException {
		Log.d("Share To Delete", "Delete requested. Uri: " + uri);
		String[] projection = { MediaStore.Images.Media._ID };
		Cursor cursor = context.getContentResolver().query(uri, projection,
				null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

		if (cursor.moveToFirst()) {
			return cursor.getInt(column_index);
		}

		throw new ShareToDeleteException("Image does not exist.");
	}

	// http://stackoverflow.com/questions/5548645/get-thumbnail-uri-path-of-the-image-stored-in-sd-card-android
	public static String getPath(Context context, Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = context.getContentResolver().query(uri, projection,
				null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		if (cursor.moveToFirst()) {
			return cursor.getString(column_index);
		}
		return null;
	}

	public interface DeleteCallback {
		void done(boolean success);
	}

	public static class ShareToDeleteException extends Exception {
		private static final long serialVersionUID = -3175598643034955186L;

		public ShareToDeleteException(String message) {
			super(message);
		}
	}
}