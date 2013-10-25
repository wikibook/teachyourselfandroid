package com.androidbook.triviaquiz12;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

public class QuizScoresActivity extends QuizActivity {
    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scores);

        // 탭 초기화
        TabHost host = (TabHost) findViewById(R.id.TabHost1);
        host.setup();

        // 전체 점수 탭
        TabSpec allScoresTab = host.newTabSpec("allTab");
        allScoresTab.setIndicator(getResources().getString(R.string.all_scores), getResources().getDrawable(
                android.R.drawable.star_on));
        allScoresTab.setContent(R.id.ScrollViewAllScores);
        host.addTab(allScoresTab);

        // 친구 점수 탭
        TabSpec friendScoresTab = host.newTabSpec("friendsTab");
        friendScoresTab.setIndicator(getResources().getString(R.string.friends_scores), getResources().getDrawable(
                android.R.drawable.star_on));
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

        XmlResourceParser mockAllScores = getResources().getXml(R.xml.allscores);
        XmlResourceParser mockFriendScores = getResources().getXml(R.xml.friendscores);
        try {
            processScores(allScoresTable, mockAllScores);
            processScores(friendScoresTable, mockFriendScores);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to load scores", e);
        }
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
     * XML에 포함된 점수 정보를 찾아 {@code TableLayout}에 채움
     * 
     * @param scoreTable
     *            채울 {@code TableLayout}
     * @param scores
     *            점수가 담긴 표준 {@code XmlResourceParser}
     * @throws XmlPullParserException
     *             XML 오류에 대해 던짐
     * @throws IOException
     *             XML을 읽는 도중 발생한 IO 오류에 대해 던짐
     */
    private void processScores(final TableLayout scoreTable, XmlResourceParser scores) throws XmlPullParserException,
            IOException {
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
                    insertScoreRow(scoreTable, scoreValue, scoreRank, scoreUserName);
                }
            }
            eventType = scores.next();
        }

        // 점수가 없는 경우 처리
        if (bFoundScores == false) {
            final TableRow newRow = new TableRow(this);
            TextView noResults = new TextView(this);
            noResults.setText(getResources().getString(R.string.no_scores));
            newRow.addView(noResults);
            scoreTable.addView(newRow);
        }
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
        final TableRow newRow = new TableRow(this);

        int textColor = getResources().getColor(R.color.title_color);
        float textSize = getResources().getDimension(R.dimen.help_text_size);

        addTextToRowWithValues(newRow, scoreUserName, textColor, textSize);
        addTextToRowWithValues(newRow, scoreValue, textColor, textSize);
        addTextToRowWithValues(newRow, scoreRank, textColor, textSize);
        scoreTable.addView(newRow);
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

}
