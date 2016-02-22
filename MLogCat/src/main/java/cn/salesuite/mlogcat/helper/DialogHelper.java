package cn.salesuite.mlogcat.helper;

import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import cn.salesuite.mlogcat.R;
import cn.salesuite.mlogcat.data.FilterQueryWithLevel;
import cn.salesuite.mlogcat.data.SortedFilterArrayAdapter;
import cn.salesuite.mlogcat.utils.ArrayUtil;
import cn.salesuite.mlogcat.utils.Callback;
import cn.salesuite.saf.utils.AsyncTaskExecutor;
import cn.salesuite.saf.utils.JodaUtils;


public class DialogHelper {
	
	public static void startRecordingWithProgressDialog(final String filename, 
			final String filterQuery, final String logLevel, final Runnable onPostExecute, final Context context) {
		
		final ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setTitle(context.getString(R.string.dialog_please_wait));
		progressDialog.setMessage(context.getString(R.string.dialog_initializing_recorder));
		progressDialog.setCancelable(false);
		
		AsyncTaskExecutor.executeAsyncTask(new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progressDialog.show();
			}

			@Override
			protected Void doInBackground(Void... params) {
				ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(
						context, filename, filterQuery, logLevel);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				if (onPostExecute != null) {
					onPostExecute.run();
				}
			}
		});
		
	}
	
	public static boolean isInvalidFilename(CharSequence filename) {
		
		String filenameAsString = null;
		
		return TextUtils.isEmpty(filename)
				|| (filenameAsString = filename.toString()).contains("/")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains(" ")
				|| !filenameAsString.endsWith(".txt");
				
	}
		
	public static void showFilterDialogForRecording(final Context context, final String queryFilterText, 
			final String logLevelText, final List<String> filterQuerySuggestions, 
			final Callback<FilterQueryWithLevel> callback) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View filterView = inflater.inflate(R.layout.logcat_filter_query_for_recording, null, false);
		
		// add suggestions to autocompletetextview
		final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) filterView.findViewById(android.R.id.edit);
		autoCompleteTextView.setText(queryFilterText);            

		SortedFilterArrayAdapter<String> suggestionAdapter = new SortedFilterArrayAdapter<String>(
				context, R.layout.logcat_simple_dropdown_small, filterQuerySuggestions);
		autoCompleteTextView.setAdapter(suggestionAdapter);
		
		// set values on spinner to be the log levels
		final Spinner spinner = (Spinner) filterView.findViewById(R.id.spinner);
		
		// put the word "default" after whatever the default log level is
		CharSequence[] logLevels = context.getResources().getStringArray(R.array.log_levels);
		String defaultLogLevel = Character.toString(PreferenceHelper.getDefaultLogLevelPreference(context));
		int index = ArrayUtil.indexOf(context.getResources().getStringArray(R.array.log_levels_values), defaultLogLevel);
		logLevels[index] = logLevels[index].toString() + " " + context.getString(R.string.default_in_parens);
		
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
		            context, android.R.layout.simple_spinner_item, logLevels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		// in case the user has changed it, choose the pre-selected log level
		spinner.setSelection(ArrayUtil.indexOf(context.getResources().getStringArray(R.array.log_levels_values), 
				logLevelText));
		
		// create alertdialog for the "Filter..." button
		new AlertDialog.Builder(context)
			.setCancelable(true)
			.setTitle(R.string.title_filter)
			.setView(filterView)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					// get the true log level value, as opposed to the display string
					int logLevelIdx = spinner.getSelectedItemPosition();
					String[] logLevelValues = context.getResources().getStringArray(R.array.log_levels_values);
					String logLevelValue = logLevelValues[logLevelIdx];
					
					String filterQuery = autoCompleteTextView.getText().toString();
					
					callback.onCallback(new FilterQueryWithLevel(filterQuery, logLevelValue));
				}
			}).show();
		
	}

	public static void stopRecordingLog(Context context) {
		
		ServiceHelper.stopBackgroundServiceIfRunning(context);
		
	}
	public static EditText createEditTextForFilenameSuggestingDialog(final Context context) {
		
		final EditText editText = new EditText(context);
		editText.setSingleLine();
		editText.setSingleLine(true);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
					// dismiss soft keyboard
					InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
					return true;
				}
				
				
				return false;
			}
		});
		
		// create an initial filename to suggest to the user
		String filename = createLogFilename();
		editText.setText(filename);
		
		// highlight everything but the .txt at the end
		editText.setSelection(0, filename.length() - 4);
		
		return editText;
	}
	
	
	
	public static void showFilenameSuggestingDialog(Context context, EditText editText, 
			OnClickListener onOkClickListener, OnClickListener onNeutralListener, 
			OnClickListener onCancelListener, int titleResId) {
		
		Builder builder = new Builder(context);
		
		builder.setTitle(titleResId)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, onCancelListener)
			.setPositiveButton(android.R.string.ok, onOkClickListener)
			.setMessage(R.string.enter_filename)
			.setView(editText);
		
		if (onNeutralListener != null) {

			builder.setNeutralButton(R.string.text_filter_ellipsis, onNeutralListener);
		}
		
		builder.show();
		
	}

	private static String createLogFilename() {
		String year = JodaUtils.getCurrentYear();
		String month = JodaUtils.getCurrentMonth();
		String day = JodaUtils.getCurrentDay();
		String hour = JodaUtils.getCurrentHour();
		String minute = JodaUtils.getCurrentMinute();
		String second = JodaUtils.getCurrentSecond();
		
		StringBuilder sb = new StringBuilder();
		sb.append(year).append("-").append(month).append("-")
				.append(day).append("-").append(hour).append("-")
				.append(minute).append("-").append(second);
		sb.append(".txt");
		
		return sb.toString();
	}
	
}
