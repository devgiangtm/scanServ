
package com.loffler.scanServ.cdcgesture;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.loffler.scanServ.cdcgesture.detection.Detector;
import com.loffler.scanServ.cdcgesture.detection.MultiBoxTracker;
import com.loffler.scanServ.cdcgesture.detection.TFLiteObjectDetectionAPIModel;
import com.loffler.scanServ.Constants;
import com.loffler.scanServ.R;
import com.loffler.scanServ.customview.OverlayView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "ssd_q_int8_300_i.tflite";
    private static final String TF_OD_API_LABELS_FILE = "label.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.85f;
    private static final float MINIMUM_CONFIDENCE_DRAW = 0.65f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final boolean DRAW_BOX = true;
    private static final float TEXT_SIZE_DIP = 10;
    private static final float COUNT_CONFIRM = 3;
    private static final float POPUP_THRESHOLD = 1;
    private static final float HIDE_POPUP_THRESHOLD = 0;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Detector detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;
    private int hidePopupThreshold = 3;
    private int showPopupThreshold = 3;


    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    private HashMap<String, Integer> hmResult = new HashMap<>();
    private String lastLabel = "";

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            this,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = 90;
//        sensorOrientation = getScreenOrientation();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
//    canvas.rotate(90);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(new Runnable() {
            @Override
            public void run() {
                //TODO background code
                final long startTime = SystemClock.uptimeMillis();
                final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                switch (MODE) {
                    case TF_OD_API:
                        minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        break;
                }

                final List<Detector.Recognition> mappedRecognitions =
                        new ArrayList<Detector.Recognition>();

                for (final Detector.Recognition result : results) {
                    //Draw Box
                    final RectF location = result.getLocation();
                    if (location != null && result.getConfidence() >= minimumConfidence) {
                        if (layoutInstruction.getVisibility() == View.GONE) {
                            if (layoutConfirm.getVisibility() == View.GONE) {
                                if (lastLabel.equals(result.getTitle())) {
                                    showPopupThreshold++;
                                } else {
                                    showPopupThreshold = 0;
                                }
                                if (showPopupThreshold >= POPUP_THRESHOLD) {
                                    hidePopupThreshold = 0;
                                    if (hmResult.get(result.getTitle()) != null) {

                                        Integer value = hmResult.get(result.getTitle());
                                        value++;
                                        hmResult.put(result.getTitle(), value);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                layoutPopUp.setVisibility(View.VISIBLE);
                                                float progress = circularProgressBar.getProgress();
//                                                if (progress == 100) {
//                                                    switch (result.getTitle()) {
//                                                        case Constant.LEFT:
//                                                            Log.d("giangtm3", "run: backQuestion");
//                                                            backQuestion();
//                                                            break;
//                                                        case Constant.RIGHT:
//                                                            Log.d("giangtm3", "run: forwardQuestion");
//                                                            forwardQuestion();
//                                                            break;
//                                                        case Constant.UP:
//                                                            Log.d("giangtm3", "run: nextQuestion 1");
//                                                            nextQuestion(1);
//                                                            break;
//                                                        case Constant.DOWN:
//                                                            Log.d("giangtm3", "run: nextQuestion 2");
//                                                            nextQuestion(0);
//                                                            break;
//                                                    }
//                                                    circularProgressBar.setProgress(0);
//                                                    hmResult.clear();
//                                                    layoutPopUp.setVisibility(View.GONE);
//                                                    showPopupThreshold = 0;
//                                                } else {
//                                                    progress += (float) (100 / COUNT_CONFIRM);
//                                                    circularProgressBar.setProgressWithAnimation(progress, 200L);
//                                                }

                                                progress += (100 / COUNT_CONFIRM);
                                                circularProgressBar.setProgressWithAnimation(progress, 200L);
                                                if (progress == 100) {
                                                    playSound();
                                                    switch (result.getTitle()) {
                                                        case Constants.LEFT:
                                                            backQuestion();
                                                            break;
                                                        case Constants.RIGHT:
                                                            forwardQuestion();
                                                            break;
                                                        case Constants.UP:
                                                            nextQuestion(1);
                                                            break;
                                                        case Constants.DOWN:
                                                            nextQuestion(0);
                                                            break;
                                                    }
                                                    hmResult.clear();

                                                }
                                                Log.d("giangtm4", "progress: "+ progress);
                                            }
                                        });
                                    } else {
                                        hmResult.clear();
                                        hmResult.put(result.getTitle(), 1);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                layoutPopUp.setVisibility(View.VISIBLE);
                                                switch (result.getTitle()) {
                                                    case Constants.LEFT:
                                                        ivIconConfirm.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_point_left));
                                                        break;
                                                    case Constants.RIGHT:
                                                        ivIconConfirm.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_point_right));
                                                        break;
                                                    case Constants.UP:
                                                        ivIconConfirm.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_like));
                                                        break;
                                                    default:
                                                        ivIconConfirm.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_dislike));
                                                }
                                                circularProgressBar.setProgress(0);
                                                showPopupThreshold = 0;

                                            }
                                        });
                                    }
                                }
                            }

                        } else {
                            if (result.getTitle().equals(Constants.UP)) {
                                showPopupThreshold = -3;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showQuestion(true);
                                    }
                                });
                            }
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layoutPopUp.setVisibility(View.GONE);
                                circularProgressBar.setProgress(0);
                            }
                        });

                    }
                    if (DRAW_BOX&& result.getConfidence() >= MINIMUM_CONFIDENCE_DRAW) {
                        canvas.drawRect(location, paint);
                        cropToFrameTransform.mapRect(location);
                        result.setLocation(location);
                        mappedRecognitions.add(result);
                    }
                    tracker.trackResults(mappedRecognitions, currTimestamp);
                    trackingOverlay.postInvalidate();
                    computingDetection = false;
                    lastLabel = results.get(0).getTitle();
                }


            }
        });


    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private enum DetectorMode {
        TF_OD_API;
    }

}
