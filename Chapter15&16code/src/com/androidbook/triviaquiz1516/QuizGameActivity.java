package com.androidbook.triviaquiz1516;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
    private TextSwitcher mQuestionText;
    private ImageSwitcher mQuestionImage;
    QuizTask downloader;

    @Override
    protected void onPause() {
        if (downloader != null && downloader.getStatus() != AsyncTask.Status.FINISHED) {
            downloader.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        // 공유 환경설정 가져오기
        mGameSettings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);

        // 문항 집합 초기화
        mQuestions = new Hashtable<Integer, Question>(QUESTION_BATCH_SIZE);

        // 질문을 이어서 진행
        int startingQuestionNumber = mGameSettings.getInt(GAME_PREFERENCES_CURRENT_QUESTION, 0);

        // 퀴즈를 처음 시작할 경우 공유 환경설정을 초기화
        if (startingQuestionNumber == 0) {
            startingQuestionNumber = 1;
            Editor editor = mGameSettings.edit();
            editor.putInt(GAME_PREFERENCES_CURRENT_QUESTION, startingQuestionNumber);
            editor.commit();
        }

        // 백그라운드에서 질문을 불러오기 시작함
        downloader = new QuizTask();
        downloader.execute(TRIVIA_SERVER_QUESTIONS, startingQuestionNumber);

        // 그 사이 UI를 구성
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

        // 텍스트 전환기 구성
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        mQuestionText = (TextSwitcher) findViewById(R.id.TextSwitcher_QuestionText);
        mQuestionText.setInAnimation(in);
        mQuestionText.setOutAnimation(out);
        mQuestionText.setFactory(new MyTextSwitcherFactory());

        mQuestionImage = (ImageSwitcher) findViewById(R.id.ImageSwitcher_QuestionImage);
        mQuestionImage.setInAnimation(in);
        mQuestionImage.setOutAnimation(out);
        mQuestionImage.setFactory(new MyImageSwitcherFactory());
    }

    /**
     * 질문을 모두 불러오면 호출됨
     * 
     * @param startingQuestionNumber
     *            사용 가능한 첫 번째 질문 번호
     */
    private void displayCurrentQuestion(int startingQuestionNumber) {
		// 질문을 적절히 불러오면 화면에 표시
        if (mQuestions.containsKey(startingQuestionNumber) == true) {
			// 텍스트 전환기의 텍스트를 설정
            mQuestionText.setCurrentText(getQuestionText(startingQuestionNumber));

			// 이미지 전환기의 이미지를 설정
            Drawable image = getQuestionImageDrawable(startingQuestionNumber);
            mQuestionImage.setImageDrawable(image);
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
     * 질문 이미지에 사용할 전환기 팩터리. 애니메이션을 적용할 다음 {@code ImageView} 객체를 생성
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
     * 질문 텍스트에 사용할 전환기 팩터리. 애니메이션을 적용할 다음 {@code TextView} 객체를 생성
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

            downloader = new QuizTask();
            downloader.execute(TRIVIA_SERVER_QUESTIONS, nextQuestionNumber);

			// 현재 질문 표시는 이 작업이 완료될 때까지 지연됨
        } else {

            displayCurrentQuestion(nextQuestionNumber);
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

    private class QuizTask extends AsyncTask<Object, String, Boolean> {
        private static final String DEBUG_TAG = "QuizGameActivity$QuizTask";

        int startingNumber;
        ProgressDialog pleaseWaitDialog;

        @Override
        protected void onCancelled() {
            Log.i(DEBUG_TAG, "onCancelled");
            handleNoQuestions();
            pleaseWaitDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(DEBUG_TAG, "Download task complete.");
            if (result) {
                displayCurrentQuestion(startingNumber);
            } else {
                handleNoQuestions();
            }

            pleaseWaitDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            pleaseWaitDialog = ProgressDialog.show(QuizGameActivity.this, "별난체험 퀴즈", "별난체험 질문을 내려 받는 중", true, true);
            pleaseWaitDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    QuizTask.this.cancel(true);
                }
            });
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            try {
				// 매개변수를 반드시 올바른 순서와 타입대로 집어 넣어야 함. 그렇지 않으면 ClassCastException이 발생
                startingNumber = (Integer) params[1];
                String pathToQuestions = params[0] + "?max=" + QUESTION_BATCH_SIZE + "&start=" + startingNumber;

				// 등록된 계정에 대해서는 점수를 갱신함
				// 지연 시간을 줄이고 네트워크 효율을 높이고자 동일한 요청에서 이러한 작업을 처리
                SharedPreferences settings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);

                Integer playerId = settings.getInt(GAME_PREFERENCES_PLAYER_ID, -1);
                if (playerId != -1) {
                    Log.d(DEBUG_TAG, "Updating score");
                    Integer score = settings.getInt(GAME_PREFERENCES_SCORE, -1);
                    if (score != -1) {
                        pathToQuestions += "&updateScore=yes&updateId=" + playerId + "&score=" + score;
                    }
                }

                Log.d(DEBUG_TAG, "path: " + pathToQuestions + " -- Num: " + startingNumber);

                result = loadQuestionBatch(startingNumber, pathToQuestions);

            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Unexpected failure in XML downloading and parsing", e);
            }

            return result;
        }

        /**
		 * XML 질문을 파싱해서 {@see mQuestions}에 삽입. 질문들은 XmlPullParser(questionBatch)에 미리 로딩돼 있음
         * 
         * @param questionBatch
         *            질문이 담긴 XmlPullParser
         * @throws XmlPullParserException
         *             XML 파싱 오류가 발생하면 던짐
         * @throws IOException
         *             IO 예외가 발생하면 던짐
         */
        private void parseXMLQuestionBatch(XmlPullParser questionBatch) throws XmlPullParserException, IOException {
            int eventType = -1;

            // XML에서 점수 기록을 찾음
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {

                    // 태그의 이름을 획득(예 questions나 question)
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
         * {@see mQuestions} 클래스의 멤버 변수로 XML을 불러옴
         * 
         * @param startQuestionNumber
         *            불러올 첫 번째 문항
         */
        private boolean loadQuestionBatch(int startQuestionNumber, String xmlSource) {
            boolean result = false;
			// 기존 문항을 제거
            mQuestions.clear();

            // 서버와 통신하여 startQuestionNumber부터 시작하는 질문 데이터를 받아옴

            XmlPullParser questionBatch;
            try {
                URL xmlUrl = new URL(xmlSource);
                questionBatch = XmlPullParserFactory.newInstance().newPullParser();
                questionBatch.setInput(xmlUrl.openStream(), null);
            } catch (XmlPullParserException e1) {
                questionBatch = null;
                Log.e(DEBUG_TAG, "Failed to initialize pull parser", e1);
            } catch (IOException e) {
                questionBatch = null;
                Log.e(DEBUG_TAG, "IO Failure during pull parser initialization", e);
            }

            // XML을 파싱
            if (questionBatch != null) {
                try {
                    parseXMLQuestionBatch(questionBatch);
                    result = true;
                } catch (XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "Pull Parser failure", e);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "IO Exception parsing XML", e);
                }
            }

            return result;

        }

    }
}
