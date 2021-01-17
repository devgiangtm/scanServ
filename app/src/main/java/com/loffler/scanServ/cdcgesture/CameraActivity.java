package com.loffler.scanServ.cdcgesture;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loffler.scanServ.Constants;
import com.loffler.scanServ.R;
import com.loffler.scanServ.cdcsetting.SharedPreferencesController;
import com.loffler.scanServ.customview.MyStepperAdapter;
import com.loffler.scanServ.dashboard.DashboardActivity;
import com.loffler.scanServ.service.sql.dao.DaoResult;
import com.loffler.scanServ.service.sql.dao.OutputDao;
import com.loffler.scanServ.service.sql.dao.OutputDaoImpl;
import com.loffler.scanServ.utils.AppLauncherImpl;
import com.loffler.scanServ.welcomescreen.WelcomeDetectorActivity;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.stepstone.stepper.StepperLayout;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.loffler.scanServ.Constants.CURRENT_SCAN_ID;
import static com.loffler.scanServ.Constants.swSQLWriteLogs;

public abstract class CameraActivity extends AppCompatActivity
        implements ImageReader.OnImageAvailableListener,
        Camera.PreviewCallback {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private int currentQuestionIdx = 0;
    private LinearLayout questionLayout;
    protected ImageView ivIconConfirm;
    protected RelativeLayout layoutPopUp;
    protected LinearLayout layoutInstruction;
    protected LinearLayout layoutBottom;
    protected CircularProgressBar circularProgressBar;
    private TextView tvQuestion;
    private TextView tvCountDownTimer;
    protected List<Question> questionList;
    private MipsData mipsData;

    private static final long START_TIME_IN_MILLIS = 30000;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;
    private static final long START_TIME_INSTRUCTION_IN_MILLIS = 30000;
    private long mTimeInstrucstionLeftInMillis = START_TIME_INSTRUCTION_IN_MILLIS;
    private CountDownTimer mCountDownTimer;
    CountDownTimer mCountDownInstructionTimer;
    private StepperLayout mStepperLayout;
    protected ImageView approve;
    protected RelativeLayout layoutBackground;
    protected LinearLayout layoutConfirm;
    protected TextView tvWarning;
    private MediaPlayer mp;
    private OutputDao outputDao;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.stepper_layout);
        mipsData = (MipsData) getIntent().getExtras().get("mimpsdata");
        prefs = getApplicationContext().getSharedPreferences(Constants.PreferenceName, MODE_PRIVATE);
        outputDao = new OutputDaoImpl(prefs);
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
        questionLayout = findViewById(R.id.question_layout);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvCountDownTimer = findViewById(R.id.countDownTimer);
        circularProgressBar = findViewById(R.id.circularProgressBar);
        ivIconConfirm = findViewById(R.id.ivIcon);
        layoutPopUp = findViewById(R.id.rlPopupConfirm);
        layoutBottom = findViewById(R.id.bottomLayout);
        layoutInstruction = findViewById(R.id.layoutInstruction);
        mStepperLayout = (StepperLayout) findViewById(R.id.stepperLayout);
        layoutBackground = findViewById(R.id.layoutBackground);
        layoutConfirm = findViewById(R.id.layoutConfirm);
        tvWarning = findViewById(R.id.tvWarning);
        approve = findViewById(R.id.approve);
//        ViewGroup.LayoutParams params = bottomSheetLayout.getLayoutParams();
//        params.height = getResources().getDisplayMetrics().heightPixels / 3;
//        bottomSheetLayout.setLayoutParams(params);
        Type listType = new TypeToken<List<Question>>() {
        }.getType();
        questionList = new Gson().fromJson(loadJSONFromAsset(), listType);
        mStepperLayout.setAdapter(new MyStepperAdapter(getSupportFragmentManager(), this, questionList.size()));
        mCountDownInstructionTimer = new CountDownTimer(mTimeInstrucstionLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeInstrucstionLeftInMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                showQuestion(false);
            }
        }.start();


    }


    protected void showPopupConfirm(boolean isApprove, String message) {
        layoutConfirm.setVisibility(View.VISIBLE);
        AnimatedVectorDrawableCompat avdCompat;
        AnimatedVectorDrawable avd;
        if (isApprove) {
            layoutBackground.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.approve_bg));
            approve.setImageResource(R.drawable.avd_anim_approve);
            tvWarning.setText(message);
        } else {
            layoutBackground.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.denied_bg));
            approve.setImageResource(R.drawable.avd_anim_denied);
            tvWarning.setText(message);
        }
        Drawable drawable = approve.getDrawable();
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mCountDownTimer.cancel();
                finish();
//                if (prefs.getBoolean(Constants.WELCOME_ENABLE, false)) {
//                    startActivity(new Intent(getBaseContext(), WelcomeDetectorActivity.class));
//                } else if (prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)) {
//                    startActivity(new Intent(getBaseContext(), DashboardActivity.class));
//                }
                if (prefs.getBoolean(Constants.WELCOME_ENABLE, false) || prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)) {
                    new AppLauncherImpl(getApplicationContext()).launchScanServ();
                } else {
                    new AppLauncherImpl(getApplicationContext()).launchMips();
                }
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mCountDownTimer.cancel();
//        if (prefs.getBoolean(Constants.WELCOME_ENABLE, false)) {
//            startActivity(new Intent(getBaseContext(), WelcomeDetectorActivity.class));
//        } else if (prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)) {
//            startActivity(new Intent(getBaseContext(), DashboardActivity.class));
//        }
        if (prefs.getBoolean(Constants.WELCOME_ENABLE, false) || prefs.getBoolean(Constants.DashboardSettingsEnableFeatureKey, false)) {
            new AppLauncherImpl(getApplicationContext()).launchScanServ();
        } else {
            new AppLauncherImpl(getApplicationContext()).launchMips();
        }
    }

    protected void showQuestion(boolean stopTimer) {
        if (stopTimer) {
            mCountDownInstructionTimer.cancel();
        }
        layoutInstruction.setVisibility(View.GONE);
        mStepperLayout.setVisibility(View.VISIBLE);
        layoutBottom.setVisibility(View.VISIBLE);
        questionLayout.setVisibility(View.VISIBLE);
        tvQuestion.setText(questionList.get(currentQuestionIdx).getQuestion());
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        startQuestionTimer();
        Log.d("giangtm2", "Start Question ");
    }


    private void startQuestionTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                if (mTimeLeftInMillis > 5000 && mTimeLeftInMillis < 6000) {
                    Toast.makeText(getBaseContext(), "You must answer the Question!!!", Toast.LENGTH_LONG).show();
                }
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                showPopupConfirm(false, "No gesture detected.  Restarting Workflow.");
            }
        }.start();
    }

    private void resetTimer() {
        mCountDownTimer.cancel();
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        startQuestionTimer();
        updateCountDownText();
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountDownTimer.setText(timeLeftFormatted);
    }


    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("question.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    protected void backQuestion() {
        if (currentQuestionIdx > 0) {
            currentQuestionIdx--;
            tvQuestion.setText(questionList.get(currentQuestionIdx).getQuestion());
            resetTimer();
            mStepperLayout.onBackClicked();
        } else {
            Toast.makeText(CameraActivity.this, "No more to back ", Toast.LENGTH_SHORT).show();
        }
    }

    protected void forwardQuestion() {
        if (questionList.get(currentQuestionIdx).getAnswer() == -1) {
            Toast.makeText(this, "You must answer current Question", Toast.LENGTH_SHORT).show();
        } else {
            currentQuestionIdx++;
            tvQuestion.setText(questionList.get(currentQuestionIdx).getQuestion());
            resetTimer();
            mStepperLayout.proceed();
        }
    }

    protected void nextQuestion(int result) {
        Log.d("giangtm2", "End Question ");
        if (currentQuestionIdx >= questionList.size() - 1) {
            questionList.get(currentQuestionIdx).setAnswer(result);
            boolean finalResult = true;

            for (Question question : questionList) {
                if (question.getAnswer() == 1) {
                    finalResult = false;
                    break;
                }
            }

            boolean finalResultTemp = finalResult;
            if (prefs.getBoolean(Constants.SQLConnected, false) && prefs.getBoolean(swSQLWriteLogs, false)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String mac = mipsData.getMac();
                        String name = mipsData.getName();
                        String userId = mipsData.getUserId();
                        String type = String.valueOf(mipsData.getType());
                        String mask = String.valueOf(mipsData.getMask());
                        Double temperature = parseTemperature(mipsData);
                        Date checkTime = parseCheckTime(mipsData);
                        OutputDao.Record.Update record = new OutputDao.Record.Update(mac, name, checkTime, temperature, userId, type, mask, null, null, null, null, null);
                        record.setQuestion1(String.valueOf(questionList.get(0).getAnswer() == 1 ? "True" : "False"));
                        record.setQuestion2(String.valueOf(questionList.get(1).getAnswer() == 1 ? "True" : "False"));
                        record.setQuestion3(String.valueOf(questionList.get(2).getAnswer() == 1 ? "True" : "False"));
                        record.setQuestion4(String.valueOf(questionList.get(3).getAnswer() == 1 ? "True" : "False"));
                        record.setResult(finalResultTemp ? "Pass" : "Fail");
                        if(SharedPreferencesController.with(getApplicationContext()).getInt(CURRENT_SCAN_ID) != -1){
                            DaoResult<Boolean> update = outputDao.update(record, SharedPreferencesController.with(getApplicationContext()).getInt(CURRENT_SCAN_ID));
                            SharedPreferencesController.with(getApplicationContext()).saveInt(CURRENT_SCAN_ID,-1);
                        }
                        System.out.println("hihi");
                    }
                }).start();
            }
            if (finalResult) {
                showPopupConfirm(true, "Access Approved");
                try {
                    //todo: print
//                    PrintUtil.print(getBaseContext(), mipsData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                showPopupConfirm(false, "Not Approved");
            }


        } else {
            questionList.get(currentQuestionIdx).setAnswer(result);
            currentQuestionIdx++;
            tvQuestion.setText(questionList.get(currentQuestionIdx).getQuestion());
            resetTimer();
            mStepperLayout.proceed();
            Log.d("giangtm2", "Start Question ");

        }
    }

    private Double parseTemperature(MipsData mipsData) {
        double temperature = Double.parseDouble(mipsData.getTemperature());
        return convertToFahrenheit(temperature);
    }

    private double convertToFahrenheit(double temperature) {
        return (temperature * 9 / 5) + 32;
    }

    private Date parseCheckTime(MipsData mipsData) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mipsData.getCheckTime());
        return cal.getTime();
    }


    protected void playSound() {
        mp = MediaPlayer.create(getBaseContext(), R.raw.beep1);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.reset();
                mp.release();
                mp = null;
            }
        });
        mp.start();
    }


    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }


    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }
                useCamera2API = true;
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();
        Fragment fragment;
        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        new CameraConnectionFragment.ConnectionCallback() {
                            @Override
                            public void onPreviewSizeChosen(final Size size, final int rotation) {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                CameraActivity.this.onPreviewSizeChosen(size, rotation);
                            }
                        },
                        this,
                        getLayoutId(),
                        getDesiredPreviewFrameSize());

        camera2Fragment.setCamera(cameraId);
        fragment = camera2Fragment;

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();
}
