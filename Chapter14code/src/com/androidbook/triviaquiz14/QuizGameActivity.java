package com.androidbook.triviaquiz14;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class QuizGameActivity extends QuizActivity {
    /** Called when the activity is first created. */

    SharedPreferences mGameSettings;
    Hashtable<Integer, Question> mQuestions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        // 예 버튼 처리
        Button yesButton = (Button) findViewById(R.id.Button_Yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleAnswerAndShowNextQuestion(true);
            }
        });

        // 아니오 버튼 처리
        Button noButton = (Button) findViewById(R.id.Button_No);
        noButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleAnswerAndShowNextQuestion(false);
            }
        });

        // 공유 환경설정 가져오기
        mGameSettings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);

        // 질문 문항 초기화
        mQuestions = new Hashtable<Integer, Question>(QUESTION_BATCH_SIZE);

        // 질문 불러오기
        int startingQuestionNumber = mGameSettings.getInt(GAME_PREFERENCES_CURRENT_QUESTION, 0);

        // 퀴즈를 처음 시작할 경우 공유 환경설정을 초기화
        if (startingQuestionNumber == 0) {
            Editor editor = mGameSettings.edit();
            editor.putInt(GAME_PREFERENCES_CURRENT_QUESTION, 1);
            editor.commit();
            startingQuestionNumber = 1;
        }

        try {
            loadQuestionBatch(startingQuestionNumber);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Loading initial question batch failed", e);
        }

        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        // 텍스트 전환기(Text Switcher) 구성
        TextSwitcher questionTextSwitcher = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
        questionTextSwitcher.setInAnimation(in);
        questionTextSwitcher.setOutAnimation(out);
        questionTextSwitcher.setFactory(new MyTextSwitcherFactory());

        // 이미지 전환기(Image Switcher) 구성
        ImageSwitcher questionImageSwitcher = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
        questionImageSwitcher.setInAnimation(in);
        questionImageSwitcher.setOutAnimation(out);
        questionImageSwitcher.setFactory(new MyImageSwitcherFactory());

        // 질문을 적절히 불러오면 화면에 표시
        if (mQuestions.containsKey(startingQuestionNumber) == true) {
            // 텍스트 전환기의 텍스트를 설정
            questionTextSwitcher.setCurrentText(getQuestionText(startingQuestionNumber));

            // 이미지 전환기의 이미지를 설정
            Drawable image = getQuestionImageDrawable(startingQuestionNumber);
            questionImageSwitcher.setImageDrawable(image);
        } else {
            // 현 시점에서 새로운 질문이 없음을 사용자에게 알림
            handleNoQuestions();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.gameoptions, menu);
        menu.findItem(R.id.help_menu_item).setIntent(new Intent(this, QuizHelpActivity.class));
        menu.findItem(R.id.settings_menu_item).setIntent(new Intent(this, QuizSettingsActivity.class));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        startActivity(item.getIntent());
        return true;
    }

    /**
     * 질문 이미지에 사용할 전환기 팩터리.
     * 애니메이션을 적용할 다음 {@code ImageView} 객체를 생성
     * 
     */
    private class MyImageSwitcherFactory implements ViewSwitcher.ViewFactory {
        public View makeView() {
            ImageView imageView = new ImageView(QuizGameActivity.this);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            return imageView;
        }
    }

    /**
     * 질문 텍스트에 사용할 전환기 팩터리.
     * 애니메이션을 적용할 다음 {@code TextView} 객체를 생성
     * 
     */
    private class MyTextSwitcherFactory implements ViewSwitcher.ViewFactory {
        public View makeView() {
            TextView textView = new TextView(QuizGameActivity.this);
            textView.setGravity(Gravity.CENTER);
            Resources res = getResources();
            float dimension = res.getDimension(R.dimen.game_question_size);
            int titleColor = res.getColor(R.color.title_color);
            int shadowColor = res.getColor(R.color.title_glow);
            textView.setTextSize(dimension);
            textView.setTextColor(titleColor);
            textView.setShadowLayer(10, 5, 5, shadowColor);
            return textView;
        }
    }

    /**
     * 
     * 사용자가 답한 내용을 기록하고 다음 질문을 불러오는 도우미 메서드
     * 
     * @param bAnswer
     *            사용자가 답한 내용
     */
    private void handleAnswerAndShowNextQuestion(boolean bAnswer) {
        int curScore = mGameSettings.getInt(GAME_PREFERENCES_SCORE, 0);
        int nextQuestionNumber = mGameSettings.getInt(GAME_PREFERENCES_CURRENT_QUESTION, 1) + 1;

        Editor editor = mGameSettings.edit();
        editor.putInt(GAME_PREFERENCES_CURRENT_QUESTION, nextQuestionNumber);

        // "예" 답변의 개수만 기록
        if (bAnswer == true) {
            editor.putInt(GAME_PREFERENCES_SCORE, curScore + 1);
        }
        editor.commit();

        if (mQuestions.containsKey(nextQuestionNumber) == false) {
            // 다음 문항 집합을 불러옴
            try {
                loadQuestionBatch(nextQuestionNumber);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Loading updated question batch failed", e);
            }
        }

        if (mQuestions.containsKey(nextQuestionNumber) == true) {
            // 질문 텍스트 갱신
            TextSwitcher questionTextSwitcher = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
            questionTextSwitcher.setText(getQuestionText(nextQuestionNumber));

            // 질문 이미지 갱신
            ImageSwitcher questionImageSwitcher = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
            Drawable image = getQuestionImageDrawable(nextQuestionNumber);
            questionImageSwitcher.setImageDrawable(image);
        } else {
            handleNoQuestions();
        }

    }

    /**
     * 질문이 더는 없을 경우 질문 화면을 구성하는 도우미 메서드.
     * 새로운 질문이 없거나 IO 실패, 또는 파서 오류와 같은 다양한 오류 상황에 호출될 수 있음.
     */
    private void handleNoQuestions() {
        TextSwitcher questionTextSwitcher = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
        questionTextSwitcher.setText(getResources().getText(R.string.no_questions));
        ImageSwitcher questionImageSwitcher = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
        questionImageSwitcher.setImageResource(R.drawable.noquestion);

        // 예 버튼 비활성화
        Button yesButton = (Button) findViewById(R.id.Button_Yes);
        yesButton.setEnabled(false);

        // 아니오 버튼 비활성화
        Button noButton = (Button) findViewById(R.id.Button_No);
        noButton.setEnabled(false);
    }

    /**
     * 특정 질문 번호에 대한 텍스트를 나타내는 {@code String}을 반환
     * 
     * @param questionNumber
     *            텍스트를 가져올 질문 번호
     * @return 질문에 대한 텍스트. {@code questionNumber}를 찾지 못하면 null을 반환
     */
    private String getQuestionText(Integer questionNumber) {
        String text = null;
        Question curQuestion = (Question) mQuestions.get(questionNumber);
        if (curQuestion != null) {
            text = curQuestion.mText;
        }
        return text;
    }

    /**
     * 특정 질문에 대한 이미지 URL을 나타내는 {@code String}을 반환
     * 
     * @param questionNumber
     *            URL을 가져올 질문 번호
     * @return URL에 대한 {@code String}. URL이 없을 경우 null을 반환
     */
    private String getQuestionImageUrl(Integer questionNumber) {
        String url = null;
        Question curQuestion = (Question) mQuestions.get(questionNumber);
        if (curQuestion != null) {
            url = curQuestion.mImageUrl;
        }
        return url;
    }

    /**
     * 특정 질문에 대한 {@code Drawable} 객체를 가져옴
     * 
     * @param questionNumber
     *            {@code Drawable}을 가져올 질문 번호
     * @return 특정 질문에 대한 {@code Drawable}을 반환하거나 이미지를 불러오는 데 실패하거나 질문이 존재하지 않을 경우 대체 이미지를 반환
     */
    private Drawable getQuestionImageDrawable(int questionNumber) {
        Drawable image;
        URL imageUrl;

        try {
            // 원격 URL로부터 스트림을 디코딩해서 Drawable을 생성
            imageUrl = new URL(getQuestionImageUrl(questionNumber));
            Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openStream());
            image = new BitmapDrawable(bitmap);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Decoding Bitmap stream failed.");
            image = getResources().getDrawable(R.drawable.noquestion);
        }
        return image;
    }

    /**
     * {@see mQuestions} 클래스의 멤버 변수로 XML을 불러옴
     * 
     * @param startQuestionNumber
     *            TODO: 현재는 사용하지 않음
     * @throws XmlPullParserException
     *             XML 파싱 오류 시 던짐
     * @throws IOException
     *             XML 불러올 때 오류가 발생하면 던짐
     */
    private void loadQuestionBatch(int startQuestionNumber) throws XmlPullParserException, IOException {
        // 기존 문항을 제거
        mQuestions.clear();

        // TODO: 서버와 통신하여 startQuestionNumber부터 시작하는 질문 데이터를 받아옴
        XmlResourceParser questionBatch;

        // 임시 질문 시작
        if (startQuestionNumber < 16) {
            questionBatch = getResources().getXml(R.xml.samplequestions);
        } else {
            questionBatch = getResources().getXml(R.xml.samplequestions2);
        }
        // 임시 질문 끝

        // XML 파싱
        int eventType = -1;

        // XML에서 점수 기록을 찾음
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {

                // 태그의 이름을 획득(예, questions나 question)
                String strName = questionBatch.getName();

                if (strName.equals(XML_TAG_QUESTION)) {

                    String questionNumber = questionBatch.getAttributeValue(null, XML_TAG_QUESTION_ATTRIBUTE_NUMBER);
                    Integer questionNum = new Integer(questionNumber);
                    String questionText = questionBatch.getAttributeValue(null, XML_TAG_QUESTION_ATTRIBUTE_TEXT);
                    String questionImageUrl = questionBatch.getAttributeValue(null, XML_TAG_QUESTION_ATTRIBUTE_IMAGEURL);

                    // 데이터를 해시테이블에 저장
                    mQuestions.put(questionNum, new Question(questionNum, questionText, questionImageUrl));
                }
            }
            eventType = questionBatch.next();
        }
    }

    /**
     * 퀴즈 문항 하나에 대한 데이터를 관리하는 객체
     * 
     */
    private class Question {
        @SuppressWarnings("unused")
        int mNumber;
        String mText;
        String mImageUrl;

        /**
         * 
         * 새로운 질문 객체를 생성
         * 
         * @param questionNum
         *            이 문항의 번호
         * @param questionText
         *            이 문항의 텍스트
         * @param questionImageUrl
         *            이 문항과 함께 보여줄 유효한 이미지 Url
         */
        public Question(int questionNum, String questionText, String questionImageUrl) {
            mNumber = questionNum;
            mText = questionText;
            mImageUrl = questionImageUrl;
        }
    }
}
