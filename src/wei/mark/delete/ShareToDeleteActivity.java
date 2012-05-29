package wei.mark.delete;

import wei.mark.delete.util.Utils;
import wei.mark.delete.util.Utils.DeleteCallback;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

					Utils.delete(this, uri, new DeleteCallback() {

						@Override
						public void done(boolean success) {
							finish();
							Toast.makeText(ShareToDeleteActivity.this,
									"Deleted successfully.", Toast.LENGTH_SHORT)
									.show();
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
			// mostly happens when we just want to refresh the Camera
			finish();
		}
	}
}