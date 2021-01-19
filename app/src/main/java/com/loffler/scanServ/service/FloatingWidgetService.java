package com.loffler.scanServ.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.IBinder;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.loffler.scanServ.NavigationActivity;
import com.loffler.scanServ.ProductKeyActivity;
import com.loffler.scanServ.R;


public class FloatingWidgetService extends Service {

    private View overlayView;

    private WindowManager windowManager;

    private void addOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (this.overlayView == null) {
            this.overlayView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.view_top, null);
            Display display = this.windowManager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams((int)(point.x * 0.45D), (int)(point.y * 0.045D), 2002, 8, -3);
            layoutParams.gravity = 51;
            layoutParams.x = (int)(point.y * 0.2D);
            layoutParams.y = (int)(point.y * 0.005D);

            this.windowManager.addView(this.overlayView, layoutParams);
            this.overlayView.setOnTouchListener(new View.OnTouchListener() {
                private GestureDetector gestureDetector = new GestureDetector(FloatingWidgetService.this.getApplicationContext(), (GestureDetector.OnGestureListener)new GestureDetector.SimpleOnGestureListener() {
                    public boolean onDoubleTap(MotionEvent param2MotionEvent) {
                        Intent intent = new Intent((Context) FloatingWidgetService.this, NavigationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        FloatingWidgetService.this.startActivity(intent);
//                        FloatingWidgetService.this.stopSelf();
                        return false;
                    }
                });

                public boolean onTouch(View param1View, MotionEvent param1MotionEvent) {
                    this.gestureDetector.onTouchEvent(param1MotionEvent);
                    return false;
                }
            });
        }
    }


    public static void stopService(Context paramContext) {
        paramContext.stopService(new Intent(paramContext, FloatingWidgetService.class));
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        addOverlay();
    }

    public void onDestroy() {
        super.onDestroy();
        View view = this.overlayView;
        if (view != null) {
            this.windowManager.removeView(view);
            this.overlayView = null;
        }
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return super.onStartCommand(paramIntent, paramInt1, paramInt2);
    }
}