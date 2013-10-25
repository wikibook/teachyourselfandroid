package com.androidbook.triviaquiz11;

import android.app.Activity;

public class QuizActivity extends Activity {
    // 게임 환경설정 값
    public static final String GAME_PREFERENCES = "GamePrefs";
    public static final String GAME_PREFERENCES_NICKNAME = "Nickname"; // String
    public static final String GAME_PREFERENCES_EMAIL = "Email"; // String
    public static final String GAME_PREFERENCES_PASSWORD = "Password"; // String
    public static final String GAME_PREFERENCES_DOB = "DOB"; // Long
    public static final String GAME_PREFERENCES_GENDER = "Gender";  // Integer, 배열 순서: 남자 (1), 여자 (2), 비공개 (0)

    public static final String DEBUG_TAG = "Trivia Quiz Log";
}
