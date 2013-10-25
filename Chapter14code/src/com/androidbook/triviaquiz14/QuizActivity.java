package com.androidbook.triviaquiz14;

import android.app.Activity;

public class QuizActivity extends Activity {

    // 게임 환경설정 값
    public static final String GAME_PREFERENCES = "GamePrefs";
    public static final String GAME_PREFERENCES_NICKNAME = "Nickname"; // String
    public static final String GAME_PREFERENCES_EMAIL = "Email"; // String
    public static final String GAME_PREFERENCES_PASSWORD = "Password"; // String
    public static final String GAME_PREFERENCES_DOB = "DOB"; // Long
    public static final String GAME_PREFERENCES_GENDER = "Gender"; // Integer, 배열 순서: 남자 (1), 여자 (2), 비공개 (0)
    public static final String GAME_PREFERENCES_SCORE = "Score"; // Integer
    public static final String GAME_PREFERENCES_CURRENT_QUESTION = "CurQuestion"; // Integer
    public static final String GAME_PREFERENCES_AVATAR = "Avatar"; // String URL to image

    public static final String GAME_PREFERENCES_FAV_PLACE_NAME = "FavPlaceName"; // String
    public static final String GAME_PREFERENCES_FAV_PLACE_LONG = "FavPlaceLong"; // float
    public static final String GAME_PREFERENCES_FAV_PLACE_LAT = "FavPlaceLat"; // float

    // XML 태그명
    public static final String XML_TAG_QUESTION_BLOCK = "questions";
    public static final String XML_TAG_QUESTION = "question";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_NUMBER = "number";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_TEXT = "text";
    public static final String XML_TAG_QUESTION_ATTRIBUTE_IMAGEURL = "imageUrl";
    public static final int QUESTION_BATCH_SIZE = 15;

    public static final String DEBUG_TAG = "Trivia Quiz Log";
}
