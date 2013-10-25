package com.androidbook.triviaquiz17;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class QuizSettingsActivity extends QuizActivity {

    AccountTask accountUpload;
    ImageUploadTask imageUpload;
    FriendRequestTask friendRequest;
    SharedPreferences mGameSettings;
    GPSCoords mFavPlaceCoords;

    static final int DATE_DIALOG_ID = 0;
    static final int PASSWORD_DIALOG_ID = 1;
    static final int PLACE_DIALOG_ID = 2;
    static final int FRIEND_EMAIL_DIALOG_ID = 3;

    static final int TAKE_AVATAR_CAMERA_REQUEST = 1;
    static final int TAKE_AVATAR_GALLERY_REQUEST = 2;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);

        setContentView(R.layout.settings);

        // 공유 환경설정을 가져옴
        mGameSettings = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);

        // 아바타 버튼 초기화
        initAvatar();

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
        
		// 즐겨 찾는 위치 선택기 초기화
        initFavoritePlacePicker();
        
		// 친구 이메일 입력 컨트롤 초기화
        initFriendEmailEntry();

		// 아직 serverId가 없으면 하나 가져올 필요가 있음
        Integer serverId = -1;
        serverId = mGameSettings.getInt(GAME_PREFERENCES_PLAYER_ID, -1);
        if (serverId == -1) {
            updateServerData();
        }
    }

    @Override
    protected void onPause() {
        if (accountUpload != null) {
            accountUpload.cancel(true);
        }

        if (imageUpload != null) {
            imageUpload.cancel(true);
        }

        if (friendRequest != null) {
            friendRequest.cancel(true);
        }

        super.onPause();
    }

    /**
     * 새 이미지나 변경된 이미지를 서버로 업로드
     */
    private void uploadAvatarImage() {
		// 다른 대기중인 갱신 내역과 충돌하지 않게 함
        if (imageUpload == null || imageUpload.getStatus() == AsyncTask.Status.FINISHED || imageUpload.isCancelled()) {
            imageUpload = new ImageUploadTask();
            imageUpload.execute();
        } else {
            Log.w(DEBUG_TAG, "Warning: upload task already going");
        }
    }

    /**
     * 최신 환경설정 데이터로 서버를 갱신(이미지 제외).
     * 
     */
    private void updateServerData() {
		// 다른 대기중인 갱신 내역과 충돌하지 않게 함
        if (accountUpload == null || accountUpload.getStatus() == AsyncTask.Status.FINISHED || accountUpload.isCancelled()) {
            accountUpload = new AccountTask();
            accountUpload.execute();
        } else {
            Log.w(DEBUG_TAG, "Warning: update task already going");
        }
    }

    /**
     * 최신 환경설정 데이터로 서버를 갱신(이미지 제외).
     * 
     */
    private void doFriendRequest(String friendEmail) {
		// 다른 대기중인 갱신 내역과 충돌하지 않게 함
        if (friendRequest == null || friendRequest.getStatus() == AsyncTask.Status.FINISHED || friendRequest.isCancelled()) {
            friendRequest = new FriendRequestTask();
            friendRequest.execute(friendEmail);
        } else {
            Log.w(DEBUG_TAG, "Warning: friendRequestTask already going");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
        case TAKE_AVATAR_CAMERA_REQUEST:

            if (resultCode == Activity.RESULT_CANCELED) {
                // 아바타 카메라 모드가 취소됨
            } else if (resultCode == Activity.RESULT_OK) {

                // 사진 촬영 후 기본적으로 제공되는 크기를 줄인 카메라 이미지를 사용
                Bitmap cameraPic = (Bitmap) data.getExtras().get("data");
                if (cameraPic != null) {
                    try {
                        saveAvatar(cameraPic);
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "saveAvatar() with camera image failed.", e);
                    }
                }
            }
            break;
        case TAKE_AVATAR_GALLERY_REQUEST:

            if (resultCode == Activity.RESULT_CANCELED) {
                // 아바타 갤러리 요청 모드가 취소됨
            } else if (resultCode == Activity.RESULT_OK) {

                // 어느 사진이 선택됐는지 파악
                Uri photoUri = data.getData();
                if (photoUri != null) {
                    try {
                        int maxLength = 75;
						// 원본 크기 이미지는 크기가 클 확률이 높음. 아바타로 쓰기에 적절한 크기로 이미지의 크기를 조정
                        Bitmap galleryPic = Media.getBitmap(getContentResolver(), photoUri);
                        Bitmap scaledGalleryPic = createScaledBitmapKeepingAspectRatio(galleryPic, maxLength);
                        saveAvatar(scaledGalleryPic);
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "saveAvatar() with gallery picker failed.", e);
                    }
                }
            }
            break;
        }
    }

    /**
     * 측면 비율은 유지한 채로 비트맵의 크기를 조정
     * 
     * @param bitmap
     *            크기를 조정할 비트맵
     * @param maxSide
     *            각 측면의 최대 길이
     * @return 크기가 조정된 새 Bitmap
     */
    private Bitmap createScaledBitmapKeepingAspectRatio(Bitmap bitmap, int maxSide) {
        int orgHeight = bitmap.getHeight();
        int orgWidth = bitmap.getWidth();

		// 모든 측면의 크기가 75px를 넘지 않도록 조정
        int scaledWidth = (orgWidth >= orgHeight) ? maxSide : (int) ((float) maxSide * ((float) orgWidth / (float) orgHeight));
        int scaledHeight = (orgHeight >= orgWidth) ? maxSide : (int) ((float) maxSide * ((float) orgHeight / (float) orgWidth));

		// 크기가 조정된 비트맵 생성
        Bitmap scaledGalleryPic = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        return scaledGalleryPic;
    }

    /**
     * Bitmap을 avatar.jpg로 저장
     * 
     * @param avatar
     *            파일 시스템에 저장할 Bitmap
     */
    private void saveAvatar(Bitmap avatar) {
        String strAvatarFilename = "avatar.jpg";
        try {
            avatar.compress(CompressFormat.JPEG, 100, openFileOutput(strAvatarFilename, MODE_PRIVATE));
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Avatar compression and save failed.", e);
        }

        Uri imageUriToSaveCameraImageTo = Uri.fromFile(new File(QuizSettingsActivity.this.getFilesDir(), strAvatarFilename));

        Editor editor = mGameSettings.edit();
        editor.putString(GAME_PREFERENCES_AVATAR, imageUriToSaveCameraImageTo.getPath());
        editor.commit();

		// 이미지를 서버로 업로드
        uploadAvatarImage();

        // 환경설정 화면 갱신
        ImageButton avatarButton = (ImageButton) findViewById(R.id.ImageButton_Avatar);
        String strAvatarUri = mGameSettings.getString(GAME_PREFERENCES_AVATAR, "android.resource://com.androidbook.triviaquiz17/drawable/avatar");
        Uri imageUri = Uri.parse(strAvatarUri);
        avatarButton.setImageURI(null); // 이전 이미지 Uri를 캐싱하려는 ImageButton을 갱신하는 방법. null을 전달하면 사실상 ImageButton이 초기화됨
        avatarButton.setImageURI(imageUri);
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "SHARED PREFERENCES");
        Log.d(DEBUG_TAG, "Nickname is: " + mGameSettings.getString(GAME_PREFERENCES_NICKNAME, "Not set"));
        Log.d(DEBUG_TAG, "Email is: " + mGameSettings.getString(GAME_PREFERENCES_EMAIL, "Not set"));
        Log.d(DEBUG_TAG, "Gender (M=1, F=2, U=0) is: " + mGameSettings.getInt(GAME_PREFERENCES_GENDER, 0));
        Log.d(DEBUG_TAG, "Password is: " + mGameSettings.getString(GAME_PREFERENCES_PASSWORD, "Not set"));
        Log.d(DEBUG_TAG, "DOB is: " + DateFormat.format("MMMM dd, yyyy", mGameSettings.getLong(GAME_PREFERENCES_DOB, 0)));
        Log.d(DEBUG_TAG, "Avatar is: " + mGameSettings.getString(GAME_PREFERENCES_AVATAR, "Not set"));
        Log.d(DEBUG_TAG, "Fav Place Name is: " + mGameSettings.getString(GAME_PREFERENCES_FAV_PLACE_NAME, "Not set"));
        Log.d(DEBUG_TAG, "Fav Place GPS Lat is: " + mGameSettings.getFloat(GAME_PREFERENCES_FAV_PLACE_LAT, 0));
        Log.d(DEBUG_TAG, "Fav Place GPS Lon is: " + mGameSettings.getFloat(GAME_PREFERENCES_FAV_PLACE_LONG, 0));

        super.onDestroy();
    }

    /**
	 * 아바타 초기화
     */
    private void initAvatar() {
		// 아바타 버튼 컨트롤 가져오기
        ImageButton avatarButton = (ImageButton) findViewById(R.id.ImageButton_Avatar);

        if (mGameSettings.contains(GAME_PREFERENCES_AVATAR)) {
            String strAvatarUri = mGameSettings.getString(GAME_PREFERENCES_AVATAR, "android.resource://com.androidbook.triviaquiz17/drawable/avatar");
            Uri imageUri = Uri.parse(strAvatarUri);
            avatarButton.setImageURI(imageUri);
        } else {
            avatarButton.setImageResource(R.drawable.avatar);
        }

        avatarButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String strAvatarPrompt = "아바타로 저장할 사진을 찍으세요!";
                Intent pictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(Intent.createChooser(pictureIntent, strAvatarPrompt), TAKE_AVATAR_CAMERA_REQUEST);
            }
        });

        avatarButton.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                String strAvatarPrompt = "아바타로 쓸 사진을 선택하세요!";
                Intent pickPhoto = new Intent(Intent.ACTION_PICK);
                pickPhoto.setType("image/*");
                startActivityForResult(Intent.createChooser(pickPhoto, strAvatarPrompt), TAKE_AVATAR_GALLERY_REQUEST);
                return true;
            }
        });
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

                    // ... 서버 데이터
                    updateServerData();
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
                    // ... 서버 데이터
                    updateServerData();
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
     * 친구 이메일 주소 입력 컨트롤 초기화
     */
    private void initFriendEmailEntry() {
		// 친구 이메일 입력 대화상자를 불러올 버튼 핸들러 설정
        Button addFriend = (Button) findViewById(R.id.Button_Friend_Email);
        addFriend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(FRIEND_EMAIL_DIALOG_ID);
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
    boolean spinnerInitCall = true;

    private void initGenderSpinner() {
        // Spinner 컨트롤에 성별을 채움
        final Spinner spinner = (Spinner) findViewById(R.id.Spinner_Gender);
        ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this, R.array.genders, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (mGameSettings.contains(GAME_PREFERENCES_GENDER)) {
            spinner.setSelection(mGameSettings.getInt(GAME_PREFERENCES_GENDER, 0));
        }

        // 스피너 선택 처리
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View itemSelected, int selectedItemPosition, long selectedId) {
                if (!spinnerInitCall) {
                    Editor editor = mGameSettings.edit();
                    editor.putInt(GAME_PREFERENCES_GENDER, selectedItemPosition);
                    editor.commit();
                    // ... 서버 데이터
                    updateServerData();
                } else {
                    spinnerInitCall = false;
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    /**
     * 즐겨 찾는 위치 선택기 초기화
     */
    private void initFavoritePlacePicker() {
        // 위치 정보 설정
        TextView placeInfo = (TextView) findViewById(R.id.TextView_FavoritePlace_Info);

        if (mGameSettings.contains(GAME_PREFERENCES_FAV_PLACE_NAME)) {
            placeInfo.setText(mGameSettings.getString(GAME_PREFERENCES_FAV_PLACE_NAME, ""));
        } else {
            placeInfo.setText(R.string.settings_favoriteplace_not_set);
        }

        // 위치 선택 대화상자 처리
        Button pickPlace = (Button) findViewById(R.id.Button_FavoritePlace);
        pickPlace.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(PLACE_DIALOG_ID);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (id) {
        case PLACE_DIALOG_ID:

            final View dialogLayout = layoutInflater.inflate(R.layout.fav_place_dialog, (ViewGroup) findViewById(R.id.root));

            final TextView placeCoordinates = (TextView) dialogLayout.findViewById(R.id.TextView_FavPlaceCoords_Info);
            final EditText placeName = (EditText) dialogLayout.findViewById(R.id.EditText_FavPlaceName);
            placeName.setOnKeyListener(new View.OnKeyListener() {

                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                        String strPlaceName = placeName.getText().toString();
                        if ((strPlaceName != null) && (strPlaceName.length() > 0)) {
                            // 문자열을 GPS 좌표로 해석
                            resolveLocation(strPlaceName);

                            Editor editor = mGameSettings.edit();
                            editor.putString(GAME_PREFERENCES_FAV_PLACE_NAME, placeName.getText().toString());
                            editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LONG, mFavPlaceCoords.mLon);
                            editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT, mFavPlaceCoords.mLat);
                            editor.commit();

                            // ... 서버 데이터
                            updateServerData();

                            placeCoordinates.setText(formatCoordinates(mFavPlaceCoords.mLat, mFavPlaceCoords.mLon));
                            return true;
                        }
                    }
                    return false;
                }
            });

            final Button mapButton = (Button) dialogLayout.findViewById(R.id.Button_MapIt);
            mapButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

					// 문자열을 GPS 좌표로 해석
                    String placeToFind = placeName.getText().toString();
                    resolveLocation(placeToFind);

                    Editor editor = mGameSettings.edit();
                    editor.putString(GAME_PREFERENCES_FAV_PLACE_NAME, placeToFind);
                    editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LONG, mFavPlaceCoords.mLon);
                    editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT, mFavPlaceCoords.mLat);
                    editor.commit();

                    // ... 서버 데이터
                    updateServerData();

                    placeCoordinates.setText(formatCoordinates(mFavPlaceCoords.mLat, mFavPlaceCoords.mLon));

					// gps 좌표를 가지고 지도를 띄움
                    String geoURI = String.format("geo:%f,%f?z=10", mFavPlaceCoords.mLat, mFavPlaceCoords.mLon);
                    Uri geo = Uri.parse(geoURI);
                    Intent geoMap = new Intent(Intent.ACTION_VIEW, geo);
                    startActivity(geoMap);
                }
            });

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setView(dialogLayout);

            // 이제 AlertDialog를 구성
            dialogBuilder.setTitle(R.string.settings_button_favoriteplace);

            dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Dialog가 다시 사용될 수 없도록 강제로 소거하고 제거
                    QuizSettingsActivity.this.removeDialog(PLACE_DIALOG_ID);
                }
            });

            dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    TextView placeInfo = (TextView) findViewById(R.id.TextView_FavoritePlace_Info);
                    String strPlaceName = placeName.getText().toString();

                    if ((strPlaceName != null) && (strPlaceName.length() > 0)) {
                        Editor editor = mGameSettings.edit();
                        editor.putString(GAME_PREFERENCES_FAV_PLACE_NAME, strPlaceName);
                        editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LONG, mFavPlaceCoords.mLon);
                        editor.putFloat(GAME_PREFERENCES_FAV_PLACE_LAT, mFavPlaceCoords.mLat);
                        editor.commit();

                        // ... 서버 데이터
                        updateServerData();

                        placeInfo.setText(strPlaceName);
                    }

                    // Dialog가 다시 사용될 수 없도록 강제로 소거하고 제거
                    QuizSettingsActivity.this.removeDialog(PLACE_DIALOG_ID);
                }
            });

            // AlertDialog를 생성해서 반환
            AlertDialog placeDialog = dialogBuilder.create();
            return placeDialog;
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

                    // ... 서버 데이터
                    updateServerData();
                }
            }, year, monthOfYear, dayOfMonth);
            return dateDialog;
        case FRIEND_EMAIL_DIALOG_ID:

            final View friendDialogLayout = layoutInflater.inflate(R.layout.friend_entry, (ViewGroup) findViewById(R.id.root));

            AlertDialog.Builder friendDialogBuilder = new AlertDialog.Builder(this);
            friendDialogBuilder.setView(friendDialogLayout);
            final TextView emailText = (TextView) friendDialogLayout.findViewById(R.id.EditText_Friend_Email);

            friendDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    String friendEmail = emailText.getText().toString();
                    if (friendEmail != null && friendEmail.length() > 0) {
                        doFriendRequest(friendEmail);
                    }
                }
            });
            return friendDialogBuilder.create();
        case PASSWORD_DIALOG_ID:

            final View layout = layoutInflater.inflate(R.layout.password_dialog, (ViewGroup) findViewById(R.id.root));

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

            // 이제 AlertDialog를 생성해서 반환
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

                        // ... 서버 데이터
                        updateServerData();

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
        case PLACE_DIALOG_ID:

            // 여기서 DatePickerDialog 관련 초기화를 처리
            AlertDialog placeDialog = (AlertDialog) dialog;

            String strFavPlaceName;

            // 즐겨 찾는 위치가 환경설정에 존재하는지 확인
            if (mGameSettings.contains(GAME_PREFERENCES_FAV_PLACE_NAME)) {

				// 환경설정에서 즐겨 찾는 곳을 가져옴
                strFavPlaceName = mGameSettings.getString(GAME_PREFERENCES_FAV_PLACE_NAME, "");
                mFavPlaceCoords = new GPSCoords(mGameSettings.getFloat(GAME_PREFERENCES_FAV_PLACE_LAT, 0), mGameSettings.getFloat(GAME_PREFERENCES_FAV_PLACE_LONG, 0));

            } else {

				// 설정된 즐겨 찾는 위치가 없음. 좌표를 현재 위치로 설정
                strFavPlaceName = getResources().getString(R.string.settings_favplace_currentlocation); // 이 위치("현재 위치")의 이름은 지정하지 않았지만 지도의 시작 지점으로 사용
				// 원한다면 사용자는 위치의 이름을 줄 수 있음
                calculateCurrentCoordinates();

            }

			// 위치명 텍스트와 좌표를 저장된 값으로 설정하거나 현재 위치에 대한 GPS 좌표로 설정
            final EditText placeName = (EditText) placeDialog.findViewById(R.id.EditText_FavPlaceName);
            placeName.setText(strFavPlaceName);

            final TextView placeCoordinates = (TextView) placeDialog.findViewById(R.id.TextView_FavPlaceCoords_Info);
            placeCoordinates.setText(formatCoordinates(mFavPlaceCoords.mLat, mFavPlaceCoords.mLon));

            return;
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

    /**
     * 화면에 표시하기 위해 좌표의 형식을 지정하는 도우미 메서드
     * 
     * @param lat
     * @param lon
     * @return 형식화된 문자열
     */
    private String formatCoordinates(float lat, float lon) {
        StringBuilder strCoords = new StringBuilder();
        strCoords.append(lat).append(",").append(lon);
        return strCoords.toString();
    }

    /**
     * 
	 * 위치명을 확인할 수 없으면 현재 좌표를 토대로 위치를 파악
     * 
     * @param strLocation
     *            파악하고자 하는 위치나 위치명
     */
    private void resolveLocation(String strLocation) {
        boolean bResolvedAddress = false;

        if (strLocation.equalsIgnoreCase(getResources().getString(R.string.settings_favplace_currentlocation)) == false) {
            bResolvedAddress = lookupLocationByName(strLocation);
        }

        if (bResolvedAddress == false) {
			// 문자열 위치명을 파악할 수 없으면(또는 "current location" 문자열과 일치하면) 현재 위치에 이름을 직접 지정했다고 가정함
            calculateCurrentCoordinates();
        }
    }

    /**
	 * 기기의 마지막 위치를 획득 시도함. 보통 이 위치는 위치 제공자가 설정한 마지막 값을 가리킴.
     */
    private void calculateCurrentCoordinates() {
        float lat = 0, lon = 0;

        try {
            LocationManager locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location recentLoc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            lat = (float) recentLoc.getLatitude();
            lon = (float) recentLoc.getLongitude();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Location failed", e);
        }

        mFavPlaceCoords = new GPSCoords(lat, lon);
    }

    /**
     * 
	 * 위치 설명을 획득한 후 mFavPlaceCoords에 좌표를 저장
     * 
     * @param strLocation
     *            찾고자 하는 위치나 위치명
     * @return 주소나 위치가 인식되면 true를, 그 밖의 경우에는 false를 반환
     */
    private boolean lookupLocationByName(String strLocation) {
        final Geocoder coder = new Geocoder(getApplicationContext());
        boolean bResolvedAddress = false;

        try {
            List<Address> geocodeResults = coder.getFromLocationName(strLocation, 1);
            Iterator<Address> locations = geocodeResults.iterator();

            while (locations.hasNext()) {
                Address loc = locations.next();
                mFavPlaceCoords = new GPSCoords((float) loc.getLatitude(), (float) loc.getLongitude());
                bResolvedAddress = true;
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to geocode location", e);
        }
        return bResolvedAddress;
    }

    private class GPSCoords {
        float mLat, mLon;

        GPSCoords(float lat, float lon) {
            mLat = lat;
            mLon = lon;
        }
    }

    private class FriendRequestTask extends AsyncTask<String, Object, Boolean> {
        @Override
        protected void onPostExecute(Boolean result) {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(false);
        }

        @Override
        protected void onPreExecute() {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean succeeded = false;
            try {
                String friendEmail = params[0];

                SharedPreferences prefs = getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
                Integer playerId = prefs.getInt(GAME_PREFERENCES_PLAYER_ID, -1);

                Vector<NameValuePair> vars = new Vector<NameValuePair>();
                vars.add(new BasicNameValuePair("command", "add"));
                vars.add(new BasicNameValuePair("playerId", playerId.toString()));
                vars.add(new BasicNameValuePair("friend", friendEmail));

                HttpClient client = new DefaultHttpClient();

                // HttpClient에 HTTP POST와 폼 변수를 사용하는 예제
                HttpPost request = new HttpPost(TRIVIA_SERVER_FRIEND_EDIT);
                request.setEntity(new UrlEncodedFormEntity(vars));

                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(request, responseHandler);

                Log.d(DEBUG_TAG, "Add friend result: " + responseBody);
                if (responseBody != null) {
                    succeeded = true;
                }

            } catch (MalformedURLException e) {
                Log.e(DEBUG_TAG, "Failed to add friend", e);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Failed to add friend", e);
            }

            return succeeded;
        }

    }

    private class ImageUploadTask extends AsyncTask<Object, String, Boolean> {
        private static final String DEBUG_TAG = "ImageUploadTask";
        ProgressDialog pleaseWaitDialog;

        @Override
        protected void onCancelled() {
            Log.i(DEBUG_TAG, "onCancelled");
            pleaseWaitDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(DEBUG_TAG, "onPostExecute");
            pleaseWaitDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            pleaseWaitDialog = ProgressDialog.show(QuizSettingsActivity.this, "별난체험 퀴즈", "아바타 이미지를 업로드하는 중...", true, true);
            pleaseWaitDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface dialog) {
                    ImageUploadTask.this.cancel(true);
                }
            });
        }

        @Override
        protected Boolean doInBackground(Object... params) {
			// HttpClient와 HttpMime을 사용해서 웹 브라우저와 동일한 방식으로 
			// HTTP POST를 통해 파일을 업로드하는 예제(멀티파트 MIME 인코딩 사용)
            String avatar = mGameSettings.getString(GAME_PREFERENCES_AVATAR, "");
            Integer playerId = mGameSettings.getInt(GAME_PREFERENCES_PLAYER_ID, -1);

            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            File file = new File(avatar);
            FileBody encFile = new FileBody(file);

            entity.addPart("avatar", encFile);
            try {
                entity.addPart("updateId", new StringBody(playerId.toString()));
            } catch (UnsupportedEncodingException e) {
                Log.e(DEBUG_TAG, "Failed to add form field.", e);
            }

            HttpPost request = new HttpPost(TRIVIA_SERVER_ACCOUNT_EDIT);
            request.setEntity(entity);

            HttpClient client = new DefaultHttpClient();

            try {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(request, responseHandler);

                if (responseBody != null && responseBody.length() > 0) {
                    Log.w(DEBUG_TAG, "Unexpected response from avatar upload: " + responseBody);
                }

            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

    }

    private class AccountTask extends AsyncTask<Object, String, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(false);
        }

        @Override
        protected void onPreExecute() {
            QuizSettingsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Boolean succeeded = false;

			// HttpClient에 HTTP GET과 폼 변수를 사용하는 예제

            Integer playerId = mGameSettings.getInt(GAME_PREFERENCES_PLAYER_ID, -1);
            String nickname = mGameSettings.getString(GAME_PREFERENCES_NICKNAME, "");
            String email = mGameSettings.getString(GAME_PREFERENCES_EMAIL, "");
            String password = mGameSettings.getString(GAME_PREFERENCES_PASSWORD, "");
            Integer score = mGameSettings.getInt(GAME_PREFERENCES_SCORE, -1);
            Integer gender = mGameSettings.getInt(GAME_PREFERENCES_GENDER, -1);
            Long birthdate = mGameSettings.getLong(GAME_PREFERENCES_DOB, 0);
            String favePlaceName = mGameSettings.getString(GAME_PREFERENCES_FAV_PLACE_NAME, "");

            Vector<NameValuePair> vars = new Vector<NameValuePair>();

            if (playerId == -1) {
				// 아직 playerId가 없으므로 uniqueId를 전달
				// 고유 식별자를 얻는 좋은 방법 중 하나는 기기의 ID를 이용하는 것이며, 이 정보는 TelephonyManager 데이터에 저장돼 있음
				// 기기의 ID를 가져오려면 READ_PHONE_STATE 권한을 이용해야 함
                TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String uniqueId = telManager.getDeviceId();

				// 고유하지만 식별 불가능한 값을 만들기 위해 값에 해시 알고리즘을 적용
                String mdUniqueId;
                try {
                    MessageDigest sha = MessageDigest.getInstance("SHA");

                    byte[] enc = sha.digest(uniqueId.getBytes());
                    StringBuilder sb = new StringBuilder();

                    for (byte enc1 : enc) {
                        sb.append(Integer.toHexString(enc1 & 0xFF));
                    }
                    mdUniqueId = sb.toString();
                } catch (NoSuchAlgorithmException e) {
                    Log.w(DEBUG_TAG, "Failed to get SHA, using hashcode()");
                    mdUniqueId = String.valueOf(uniqueId.hashCode());
                }
                vars.add(new BasicNameValuePair("uniqueId", mdUniqueId));

            } else {
				// playerId를 사용해서 데이터를 갱신
                vars.add(new BasicNameValuePair("updateId", playerId.toString()));
				// 이어서 최신 점수를 삽입
                vars.add(new BasicNameValuePair("score", score.toString()));
            }
            vars.add(new BasicNameValuePair("nickname", nickname));
            vars.add(new BasicNameValuePair("email", email));
            vars.add(new BasicNameValuePair("password", password));
            vars.add(new BasicNameValuePair("gender", gender.toString()));
            vars.add(new BasicNameValuePair("faveplace", favePlaceName));
            vars.add(new BasicNameValuePair("dob", birthdate.toString()));

            String url = TRIVIA_SERVER_ACCOUNT_EDIT + "?" + URLEncodedUtils.format(vars, null);

            HttpGet request = new HttpGet(url);

            try {

                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                HttpClient client = new DefaultHttpClient();
                String responseBody = client.execute(request, responseHandler);

                if (responseBody != null && responseBody.length() > 0) {
                    Integer resultId = Integer.parseInt(responseBody);
                    Editor editor = mGameSettings.edit();
                    editor.putInt(GAME_PREFERENCES_PLAYER_ID, resultId);
                    editor.commit();
                    succeeded = true;
                }

            } catch (ClientProtocolException e) {
                Log.e(DEBUG_TAG, "Failed to get playerId (protocol): ", e);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Failed to get playerId (io): ", e);
            }
            return succeeded;
        }
    }
}
