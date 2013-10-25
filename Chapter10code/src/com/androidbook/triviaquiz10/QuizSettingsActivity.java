package com.androidbook.triviaquiz10;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class QuizSettingsActivity extends QuizActivity {
    SharedPreferences mGameSettings;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        // 공유 환경설정을 가져옴
        mGameSettings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
        
		// 별명 입력 컨트롤 초기화
        initNicknameEntry();
        
		// 이메일 입력 컨트롤 초기화
        initEmailEntry();
        
		// 비밀번호 선택 컨트롤 초기화
        initPasswordChooser();
        
		// 날짜 선택 컨트롤 초기화
        initDatePicker();
        
		// 성별 스피너 초기화
        initGenderSpinner();
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "SHARED PREFERENCES");
        Log.d(DEBUG_TAG, "Nickname is: " + mGameSettings.getString(GAME_PREFERENCES_NICKNAME, "Not set"));
        Log.d(DEBUG_TAG, "Email is: " + mGameSettings.getString(GAME_PREFERENCES_EMAIL, "Not set"));
        Log.d(DEBUG_TAG, "Gender (M=1, F=2, U=0) is: " + mGameSettings.getInt(GAME_PREFERENCES_GENDER, 0));
        // 아직까진 비밀번호를 저장하지 않는다
        Log.d(DEBUG_TAG, "Password is: " + mGameSettings.getString(GAME_PREFERENCES_PASSWORD, "Not set"));
        // 아직까진 생년월일을 저장하지 않는다
        Log.d(DEBUG_TAG, "DOB is: "
                + DateFormat.format("MMMM dd, yyyy", mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));
        super.onDestroy();
    }

    /**
     * 별명 입력 컨트롤 초기화
     */
    private void initNicknameEntry() {
        // 별명 저장
        final EditText nicknameText = (EditText) findViewById(R.id.EditText_Nickname);
        if (mGameSettings.contains(GAME_PREFERENCES_NICKNAME)) {
            nicknameText.setText(mGameSettings.getString(GAME_PREFERENCES_NICKNAME, ""));
        }
        nicknameText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String strNickname = nicknameText.getText().toString();
                    Editor editor = mGameSettings.edit();
                    editor.putString(GAME_PREFERENCES_NICKNAME, strNickname);
                    editor.commit();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 이메일 입력 컨트롤 초기화
     */
    private void initEmailEntry() {
        // 이메일 저장
        final EditText emailText = (EditText) findViewById(R.id.EditText_Email);
        if (mGameSettings.contains(GAME_PREFERENCES_EMAIL)) {
            emailText.setText(mGameSettings.getString(GAME_PREFERENCES_EMAIL, ""));
        }
        emailText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Editor editor = mGameSettings.edit();
                    editor.putString(GAME_PREFERENCES_EMAIL, emailText.getText().toString());
                    editor.commit();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 비밀번호 선택 컨트롤 초기화
     */
    private void initPasswordChooser() {
        // 비밀번호 정보 설정
        TextView passwordInfo = (TextView) findViewById(R.id.TextView_Password_Info);
        if (mGameSettings.contains(GAME_PREFERENCES_PASSWORD)) {
            passwordInfo.setText(R.string.settings_pwd_set);
        } else {
            passwordInfo.setText(R.string.settings_pwd_not_set);
        }
        // 비밀번호 설정 대화상자 처리
        Button setPassword = (Button) findViewById(R.id.Button_Password);
        setPassword.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(QuizSettingsActivity.this, "TODO: 비밀번호 대화상자를 띄운다", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 날짜 선택 컨트롤 초기화
     */
    private void initDatePicker() {
        // 날짜 정보 설정
        TextView dobInfo = (TextView) findViewById(R.id.TextView_DOB_Info);
        if (mGameSettings.contains(GAME_PREFERENCES_DOB)) {
            dobInfo.setText(DateFormat.format("yyyy년 MM월 dd일", mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));
        } else {
            dobInfo.setText(R.string.settings_dob_not_set);
        }
        // 날짜 선택 대화상자 처리
        Button pickDate = (Button) findViewById(R.id.Button_DOB);
        pickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(QuizSettingsActivity.this, "TODO: DatePickerDialog를 띄운다", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 스피너 초기화
     */
    private void initGenderSpinner() {
        // Spinner 컨트롤에 성별을 채움
        final Spinner spinner = (Spinner) findViewById(R.id.Spinner_Gender);
        ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this, R.array.genders,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (mGameSettings.contains(GAME_PREFERENCES_GENDER)) {
            spinner.setSelection(mGameSettings.getInt(GAME_PREFERENCES_GENDER, 0));
        }
        // 스피너 선택 처리
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View itemSelected, int selectedItemPosition,
                    long selectedId) {
                Editor editor = mGameSettings.edit();
                editor.putInt(GAME_PREFERENCES_GENDER, selectedItemPosition);
                editor.commit();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
