package com.androidbook.triviaquiz22;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class QuizWidgetProvider extends AppWidgetProvider {

    // private static final int IO_BUFFER_SIZE = 4 * 1024;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// 다음을 Service에 집어 넣어 백그라운드에서 실행되게 함
		// Provider가 남아있을지도 모르므로 스레드를 쓸 수 없음
		// (매니페스트에 서비스 항목을 추가하는 것을 잊지 말 것)

        Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
        context.startService(serviceIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
		// 주의사항: appWidgetids은 무시해도 괜찮지만 앱 위젯이 하나 이상 실행되고 있을 경우
		// 이 앱 위젯 인스턴스에 대한 갱신이 중단될 수도 있음. 이 위젯은 다중 인스턴스 위젯으로
		// 설계되지 않음.
        Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
        context.stopService(serviceIntent);

        super.onDeleted(context, appWidgetIds);
    }

    public static class WidgetUpdateService extends Service {
        Thread widgetUpdateThread = null;

        private static final String DEBUG_TAG = "WidgetUpdateService";

        @Override
        public int onStartCommand(Intent intent, int flags, final int startId) {
            widgetUpdateThread = new Thread() {
                public void run() {
                    Context context = WidgetUpdateService.this;
                    WidgetData widgetData = new WidgetData("Unknown", "NA", "");

                    getWidgetData(widgetData);

					// RemoteView를 준비
                    String packageName = context.getPackageName();
                    Log.d(DEBUG_TAG, "packageName: " + packageName);
                    RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget);
                    remoteView.setTextViewText(R.id.widget_nickname, widgetData.nickname);
                    remoteView.setTextViewText(R.id.widget_score, "Score: " + widgetData.score);
                    if (widgetData.avatarUrl.length() > 0) {
                        // remoteView.setImageViewUri(R.id.widget_image, Uri.parse(avatarUrl));
                        URL image;
                        try {
                            image = new URL(widgetData.avatarUrl);
                            Log.d(DEBUG_TAG, "avatarUrl: " + widgetData.avatarUrl);

                            // decodeStream을 직접적으로 사용하지 않는 이유에 대해서는
							// http://bit.ly/bAtW6W와 http://bit.ly/a3Qkw4를 참고
                            // (짧게 말해서, 특정 상황에서는 동작하지 않기 때문)
                            // 아래에 작성한 꼼수는 <시작하세요! 안드로이드 프로그래밍>에서도 사용한 바 있음

                            Bitmap bitmap = BitmapFactory.decodeStream(image.openStream());
                            /*
                             * BufferedInputStream in;
                             * BufferedOutputStream out;
                             * 
                             * in = new BufferedInputStream(image.openStream(), IO_BUFFER_SIZE);
                             * final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                             * out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
                             * copy(in, out);
                             * // 이 파일 하단에 구현 방법이 나와 있음; 사용하려면 주석을 해제
                             * out.flush();
                             * 
                             * final byte[] data = dataStream.toByteArray();
                             * Log.d(DEBUG_TAG, "Length: "+ data.length);
                             * Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                             */
                            if (bitmap == null) {
                                Log.w(DEBUG_TAG, "Failed to decode image");

                                remoteView.setImageViewResource(R.id.widget_image, R.drawable.avatar);
                            } else {
                                remoteView.setImageViewBitmap(R.id.widget_image, bitmap);
                            }
                        } catch (MalformedURLException e) {
                            Log.e(DEBUG_TAG, "Bad url in image", e);
                        } catch (IOException e) {
                            Log.e(DEBUG_TAG, "IO failure for image", e);
                        }

                    } else {
                        remoteView.setImageViewResource(R.id.widget_image, R.drawable.avatar);
                    }

                    try {

                        // 클릭 처리 추가
                        Intent launchAppIntent = new Intent(context, QuizMenuActivity.class);
                        PendingIntent launchAppPendingIntent = PendingIntent.getActivity(context, 0, launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        remoteView.setOnClickPendingIntent(R.id.widget_view, launchAppPendingIntent);

						// QuizWidgetProvider에 대한 안드로이드 컴포넌트 이름을 획득
                        ComponentName quizWidget = new ComponentName(context, QuizWidgetProvider.class);

						// AppWidgetManager 인스턴스를 획득
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

						// 위젯 갱신
                        appWidgetManager.updateAppWidget(quizWidget, remoteView);

                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Failed to update widget", e);
                    }

                    if (!WidgetUpdateService.this.stopSelfResult(startId)) {
                        Log.e(DEBUG_TAG, "Failed to stop service");
                    }
                }

                /**
				 * Widget에서 표시할 데이터를 다운로드
                 * 
                 * @param widgetData
                 */
                private void getWidgetData(WidgetData widgetData) {
                    SharedPreferences prefs = getSharedPreferences(QuizActivity.GAME_PREFERENCES, Context.MODE_PRIVATE);
                    Integer playerId = prefs.getInt(QuizActivity.GAME_PREFERENCES_PLAYER_ID, -1);

                    try {
                        URL userInfo = new URL(QuizActivity.TRIVIA_SERVER_BASE + "getplayer?playerId=" + playerId);
                        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                        parser.setInput(userInfo.openStream(), null);

                        int eventType = -1;
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                String strName = parser.getName();

                                if (strName.equals("nickname")) {
                                    widgetData.nickname = parser.nextText();
                                } else if (strName.equals("score")) {
                                    widgetData.score = parser.nextText();
                                } else if (strName.equals("avatarUrl")) {
                                    widgetData.avatarUrl = parser.nextText();
                                }
                            }
                            eventType = parser.next();
                        }
                    } catch (MalformedURLException e) {
                        Log.e(DEBUG_TAG, "Bad URL", e);
                    } catch (XmlPullParserException e) {
                        Log.e(DEBUG_TAG, "Parser exception", e);
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "IO Exception", e);
                    }
                }
            };

			// 백그라운드 스레드 시작
            widgetUpdateThread.start();

			// 처리가 중단됐다면 추가 데이터를 다시 받기 위해 원래 인텐트를 가지고 재시작한다
            return START_REDELIVER_INTENT;
        }

        @Override
        public void onDestroy() {
            widgetUpdateThread.interrupt();
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) {
			// 연동되지 않음
            return null;
        }

        /**
		 * 입력 스트림의 내용을 출력 스트림으로 복사({@link #IO_BUFFER_SIZE}에 지정된 크기를 지닌 임시 바이트 배열 버퍼을 이용)
         * 
         * @param in
         *            내용을 복사해올 입력 스트림
         * @param out
         *            내용을 복사할 출력 스트림
         * @throws IOException
         *             복사 도중 오류가 발생하면 예외를 던짐
         */
        /*
         * private static void copy(InputStream in, OutputStream out) throws IOException { byte[] b = new byte[IO_BUFFER_SIZE]; int read; while ((read = in.read(b)) != -1) { out.write(b, 0, read); } }
         */

        public static class WidgetData {
            String nickname;
            String score;
            String avatarUrl;

            public WidgetData(String nickname, String score, String avatarUrl) {
                super();
                this.nickname = nickname;
                this.score = score;
                this.avatarUrl = avatarUrl;
            }

            public String getNickname() {
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public String getScore() {
                return score;
            }

            public void setScore(String score) {
                this.score = score;
            }

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
            }
        }
    }

}
