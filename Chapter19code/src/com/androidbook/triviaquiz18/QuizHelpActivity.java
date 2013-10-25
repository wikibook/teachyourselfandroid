package com.androidbook.triviaquiz18;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class QuizHelpActivity extends QuizActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        // 원시 파일을 문자열로 읽어들인 후 TextView에 채움
        InputStream iFile = getResources().openRawResource(R.raw.quizhelp);
        try {
            TextView helpText = (TextView) findViewById(R.id.TextView_HelpText);
            String strFile = inputStreamToString(iFile);
            helpText.setText(strFile);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "InputStreamToString failure", e);
        }

    }

    /**
     * 입력 스트림을 문자열로 변환
     * 
     * @param is
     *            읽을 {@code InputStream} 객체
     * @return 입력에 대한 문자열을 나타내는 {@code String} 객체
     * @throws IOException
     *             읽기 실패 시 예외를 던짐
     */
    public String inputStreamToString(InputStream is) throws IOException {
        StringBuffer sBuffer = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String strLine = null;

        while ((strLine = br.readLine()) != null) {
            sBuffer.append(strLine + "\n");
        }

        br.close();
        is.close();

        return sBuffer.toString();
    }
}