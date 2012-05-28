package wei.mark.delete;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class ShareToDeletePreferences extends PreferenceActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		try {
			Preference version = findPreference("version");
			version.setSummary("Version "
					+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			version.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent browserIntent = new Intent(
							Intent.ACTION_VIEW,
							Uri.parse("http://pingpongboss.github.com/Share-To-Delete/"));
					startActivity(browserIntent);
					return true;
				}
			});
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		startService(new Intent(this, ContentObserverService.class));
	}

}
