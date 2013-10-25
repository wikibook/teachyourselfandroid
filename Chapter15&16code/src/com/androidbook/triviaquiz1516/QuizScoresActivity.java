package com.androidbook.triviaquiz1516;

import java.io.IOException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

public class QuizScoresActivity extends QuizActivity {

    int mProgressCounter = 0;
    ScoreDownloaderTask allScoresDownloader;
    ScoreDownloaderTask friendScoresDownloader;

    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.scores);

        // 탭 초기화
        TabHost host = (TabHost) findViewById(R.id.TabHost1);
        host.setup();

        // 전체 점수 탭
        TabSpec allScoresTab = host.newTabSpec("allTab");
        allScoresTab.setIndicator(getResources().getString(R.string.all_scores), getResources().getDrawable(android.R.drawable.star_on));
        allScoresTab.setContent(R.id.ScrollViewAllScores);
        host.addTab(allScoresTab);

        // 친구 점수 탭
        TabSpec friendScoresTab = host.newTabSpec("friendsTab");
        friendScoresTab.setIndicator(getResources().getString(R.string.friends_scores), getResources().getDrawable(android.R.drawable.star_on));
        friendScoresTab.setContent(R.id.ScrollViewFriendScores);
        host.addTab(friendScoresTab);

        // 기본 탭 설정
        host.setCurrentTabByTag("allTab");

        // TableLayout 참조 획득
        TableLayout allScoresTable = (TableLayout) findViewById(R.id.TableLayout_AllScores);
        TableLayout friendScoresTable = (TableLayout) findViewById(R.id.TableLayout_FriendScores);

        // 각 TableLayout에 열 이름을 포함한 노랑색 머릿말 행을 추가
        initializeHeaderRow(allScoresTable);
        initializeHeaderRow(friendScoresTable);

        allScoresDownloader = new ScoreDownloaderTask();
        allScoresDownloader.execute(TRIVIA_SERVER_SCORES, allScoresTable);

        SharedPreferences prefs = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
        Integer playerId = prefs.getInt(GAME_PREFERENCES_PLAYER_ID, -1);

        if (playerId != -1) {
            friendScoresDownloader = new ScoreDownloaderTask();
            friendScoresDownloader.execute(TRIVIA_SERVER_SCORES + "?playerId=" + playerId, friendScoresTable);
        }
    }

    @Override
    protected void onPause() {
        if (allScoresDownloader != null && allScoresDownloader.getStatus() != AsyncTask.Status.FINISHED) {
            allScoresDownloader.cancel(true);
        }
        if (friendScoresDownloader != null && friendScoresDownloader.getStatus() != AsyncTask.Status.FINISHED) {
            friendScoresDownloader.cancel(true);
        }
        super.onPause();
    }

    /**
     * 
     * {@code TableLayout}에 {@code TableRow} 머리말을 추가(서식 적용)
     * 
     * @param scoreTable
     *            머리말 행이 추가될 {@code TableLayout}
     */
    private void initializeHeaderRow(TableLayout scoreTable) {
        // 테이블 머리말 행을 생성
        TableRow headerRow = new TableRow(this);

        int textColor = getResources().getColor(R.color.logo_color);
        float textSize = getResources().getDimension(R.dimen.help_text_size);

        addTextToRowWithValues(headerRow, getResources().getString(R.string.username), textColor, textSize);
        addTextToRowWithValues(headerRow, getResources().getString(R.string.score), textColor, textSize);
        addTextToRowWithValues(headerRow, getResources().getString(R.string.rank), textColor, textSize);
        scoreTable.addView(headerRow);
    }

    /**
     * {@code insertScoreRow()} 도우미 메서드 -- {@code TextView} 데이터로 구성된 세 열로 {@code TableRow}를 채움 (서식 적용)
     * 
     * @param tableRow
     *            텍스트가 추가되는 {@code TableRow}
     * @param text
     *            추가할 텍스트
     * @param textColor
     *            텍스트 색상
     * @param textSize
     *            텍스트 크기
     */
    private void addTextToRowWithValues(final TableRow tableRow, String text, int textColor, float textSize) {
        TextView textView = new TextView(this);
        textView.setTextSize(textSize);
        textView.setTextColor(textColor);
        textView.setText(text);
        tableRow.addView(textView);
    }

    private class ScoreDownloaderTask extends AsyncTask<Object, String, Boolean> {
        private static final String DEBUG_TAG = "ScoreDownloaderTask";
        TableLayout table;

        @Override
        protected void onCancelled() {
            Log.i(DEBUG_TAG, "onCancelled");
            mProgressCounter--;
            if (mProgressCounter <= 0) {
                mProgressCounter = 0;
                QuizScoresActivity.this.setProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(DEBUG_TAG, "onPostExecute");
            mProgressCounter--;
            if (mProgressCounter <= 0) {
                mProgressCounter = 0;
                QuizScoresActivity.this.setProgressBarIndeterminateVisibility(false);
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressCounter++;
            QuizScoresActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onProgressUpdate(String... values) {

            if (values.length == 3) {
                String scoreValue = values[0];
                String scoreRank = values[1];
                String scoreUserName = values[2];
                insertScoreRow(table, scoreValue, scoreRank, scoreUserName);
            } else {
                final TableRow newRow = new TableRow(QuizScoresActivity.this);
                TextView noResults = new TextView(QuizScoresActivity.this);
                noResults.setText(getResources().getString(R.string.no_scores));
                newRow.addView(noResults);
                table.addView(newRow);
            }

        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            String pathToScores = (String) params[0];
            table = (TableLayout) params[1];

            XmlPullParser scores;
            try {
                URL xmlUrl = new URL(pathToScores);
                scores = XmlPullParserFactory.newInstance().newPullParser();
                scores.setInput(xmlUrl.openStream(), null);
            } catch (XmlPullParserException e) {
                scores = null;
            } catch (IOException e) {
                scores = null;
            }

            if (scores != null) {
                try {
                    processScores(scores);
                } catch (XmlPullParserException e) {
                    Log.e(DEBUG_TAG, "Pull Parser failure", e);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "IO Exception parsing XML", e);
                }
            }

            return result;
        }

        /**
         * 
         * {@code processScores()} 도우미 메서드 -- {@code TableLayout}에 새 점수 {@code TableRow}를 삽입
         * 
         * @param scoreTable
         *            점수를 추가할 {@code TableLayout}
         * @param scoreValue
         *            점수 값
         * @param scoreRank
         *            점수 순위
         * @param scoreUserName
         *            점수를 획득한 사용자
         */
        private void insertScoreRow(final TableLayout scoreTable, String scoreValue, String scoreRank, String scoreUserName) {
            final TableRow newRow = new TableRow(QuizScoresActivity.this);

            int textColor = getResources().getColor(R.color.title_color);
            float textSize = getResources().getDimension(R.dimen.help_text_size);

            addTextToRowWithValues(newRow, scoreUserName, textColor, textSize);
            addTextToRowWithValues(newRow, scoreValue, textColor, textSize);
            addTextToRowWithValues(newRow, scoreRank, textColor, textSize);
            scoreTable.addView(newRow);
        }

        /**
         * XML에 포함된 점수 정보를 찾아 {@code TableLayout}에 채움
         * 
		 * @param scores
         *            점수가 담긴 표준 {@code XmlResourceParser}
         * @throws XmlPullParserException
         *             XML 오류에 대해 던짐
         * @throws IOException
         *             XML을 읽는 도중 발생한 IO 오류에 대해 던짐
         */
        private void processScores(XmlPullParser scores) throws XmlPullParserException, IOException {
            int eventType = -1;
            boolean bFoundScores = false;

            // XML에서 점수 레코드를 찾음
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {

                    // 태그의 이름을 획득(예, scores나 score)
                    String strName = scores.getName();

                    if (strName.equals("score")) {
                        bFoundScores = true;
                        String scoreValue = scores.getAttributeValue(null, "score");
                        String scoreRank = scores.getAttributeValue(null, "rank");
                        String scoreUserName = scores.getAttributeValue(null, "username");
                        publishProgress(scoreValue, scoreRank, scoreUserName);
                    }
                }
                eventType = scores.next();
            }

            // 점수가 없는 경우 처리
            if (bFoundScores == false) {
                publishProgress();
            }
        }

    }

}
