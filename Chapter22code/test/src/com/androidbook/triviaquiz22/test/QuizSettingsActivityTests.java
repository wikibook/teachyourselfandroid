package com.androidbook.triviaquiz22.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.EditText;
import com.androidbook.triviaquiz22.QuizActivity;
import com.androidbook.triviaquiz22.QuizSettingsActivity;
import com.androidbook.triviaquiz22.R;

public class QuizSettingsActivityTests extends ActivityInstrumentationTestCase2<QuizSettingsActivity> {
    private static final String DEBUG_TAG = "QuizSettingsActivityTests";

    private EditText nickname;

    public QuizSettingsActivityTests() {
        super("com.androidbook.triviaquiz22", QuizSettingsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final QuizSettingsActivity settingsActivity = getActivity();
        nickname = (EditText) settingsActivity.findViewById(R.id.EditText_Nickname);
    }

    public void testNicknameFieldConsistency() {
        SharedPreferences settings = getActivity().getSharedPreferences(QuizActivity.GAME_PREFERENCES,
                Context.MODE_PRIVATE);
        String fromPrefs = settings.getString(QuizActivity.GAME_PREFERENCES_NICKNAME, "");
        String fromField = nickname.getText().toString();
        assertTrue("Field should equal prefs value", fromPrefs.equals(fromField));
    }

    private static final String TESTNICK_KEY_PRESSES = "T E S T N I C K ENTER";

    // ...
    public void testUpdateNickname() {
        Log.w(DEBUG_TAG, "Warning: " + "If nickname was previously 'testnick' this test is invalid.");
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                nickname.setText("");
                nickname.requestFocus();
            }
        });
        sendKeys(TESTNICK_KEY_PRESSES);
        SharedPreferences settings = getActivity().getSharedPreferences(QuizActivity.GAME_PREFERENCES,
                Context.MODE_PRIVATE);
        String fromPrefs = settings.getString(QuizActivity.GAME_PREFERENCES_NICKNAME, "");
        assertTrue("Prefs should be testnick", fromPrefs.equalsIgnoreCase("testnick"));
    }
}
