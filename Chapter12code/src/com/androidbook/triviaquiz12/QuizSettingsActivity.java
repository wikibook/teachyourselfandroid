package com.androidbook.triviaquiz12;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class QuizSettingsActivity extends QuizActivity {

    SharedPreferences mGameSettings;

    static final int DATE_DIALOG_ID = 0;
    static final int PASSWORD_DIALOG_ID = 1;

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
        Log.d(DEBUG_TAG, "Password is: " + mGameSettings.getString(GAME_PREFERENCES_PASSWORD, "Not set"));
        Log.d(DEBUG_TAG, "DOB is: " + DateFormat.format("MMMM dd, yyyy", mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));
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
                showDialog(PASSWORD_DIALOG_ID);
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
                showDialog(DATE_DIALOG_ID);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG_ID:

            final TextView dob = (TextView) findViewById(R.id.TextView_DOB_Info);

            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int monthOfYear = cal.get(Calendar.MONTH) + 1;
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dateDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                    Time dateOfBirth = new Time();
                    dateOfBirth.set(dayOfMonth, monthOfYear, year);
                    long dtDob = dateOfBirth.toMillis(true);
                    dob.setText(DateFormat.format("yyyy년 MM월 dd일", dtDob));

                    Editor editor = mGameSettings.edit();
                    editor.putLong(GAME_PREFERENCES_DOB, dtDob);
                    editor.commit();
                }
            }, year, monthOfYear, dayOfMonth);
            return dateDialog;

        case PASSWORD_DIALOG_ID:

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.password_dialog, (ViewGroup) findViewById(R.id.root));

            final EditText p1 = (EditText) layout.findViewById(R.id.EditText_Pwd1);
            final EditText p2 = (EditText) layout.findViewById(R.id.EditText_Pwd2);
            final TextView error = (TextView) layout.findViewById(R.id.TextView_PwdProblem);

            p2.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {

                    String strPass1 = p1.getText().toString();
                    String strPass2 = p2.getText().toString();

                    if (strPass1.equals(strPass2)) {
                        error.setText(R.string.settings_pwd_equal);
                    } else {
                        error.setText(R.string.settings_pwd_not_equal);
                    }
                }

                // ... 다른 재정의한 메서드에서는 아무것도 하지 않음
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout);

            // 이제 AlertDialog를 구성
            builder.setTitle(R.string.settings_button_pwd);

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Dialog가 다시 사용될 수 없도록 강제로 소거하고 제거
                    QuizSettingsActivity.this.removeDialog(PASSWORD_DIALOG_ID);
                }
            });

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    TextView passwordInfo = (TextView) findViewById(R.id.TextView_Password_Info);

                    String strPassword1 = p1.getText().toString();
                    String strPassword2 = p2.getText().toString();

                    if (strPassword1.equals(strPassword2)) {
                        Editor editor = mGameSettings.edit();
                        editor.putString(GAME_PREFERENCES_PASSWORD, strPassword1);
                        editor.commit();

                        passwordInfo.setText(R.string.settings_pwd_set);
                    } else {
                        Log.d(DEBUG_TAG, "Passwords do not match. Not saving. Keeping old password (if set).");
                    }
                    // Dialog가 다시 사용될 수 없도록 강제로 소거하고 제거
                    QuizSettingsActivity.this.removeDialog(PASSWORD_DIALOG_ID);
                }
            });

            // AlertDialog를 생성해서 반환
            AlertDialog passwordDialog = builder.create();
            return passwordDialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
        case DATE_DIALOG_ID:

            // 여기서 DatePickerDialog 관련 초기화를 처리
            DatePickerDialog dateDialog = (DatePickerDialog) dialog;
            int iDay,
            iMonth,
            iYear;

            // 생년월일 환경설정 확인
            if (mGameSettings.contains(GAME_PREFERENCES_DOB)) {
                // 환경설정에서 생년월일 설정을 가져옴
                long msBirthDate = mGameSettings.getLong(GAME_PREFERENCES_DOB, 0);
                Time dateOfBirth = new Time();
                dateOfBirth.set(msBirthDate);

                iDay = dateOfBirth.monthDay;
                iMonth = dateOfBirth.month;
                iYear = dateOfBirth.year;
            } else {
                Calendar cal = Calendar.getInstance();

                // 오늘 날짜 필드
                iDay = cal.get(Calendar.DAY_OF_MONTH);
                iMonth = cal.get(Calendar.MONTH);
                iYear = cal.get(Calendar.YEAR);
            }
            // DatePicker의 날짜를 생년월일이나 현재 날짜로 설정
            dateDialog.updateDate(iYear, iMonth, iDay);
            return;

        case PASSWORD_DIALOG_ID:
            // 여기서 Password Dialog 관련 초기화를 처리
            // 기존 비밀번호 대화상자를 보여주고 싶지 않으므로 새로운 대화상자를 설정하고
            // 여기서는 아무것도 할 필요가 없음
            // 비밀번호 대화상자는 쓰고 난 후 "재사용"하지 않을 것이므로
            // removeDialog()를 이용해서 명시적으로 액티비티 대화상자 풀에서 제거하고
            // 필요할 때 다시 생성한다.
            return;
        }
    }

}
