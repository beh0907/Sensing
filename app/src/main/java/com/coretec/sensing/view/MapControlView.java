package com.coretec.sensing.view;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatImageView;

import com.coretec.sensing.activity.MapActivity;
import com.coretec.sensing.listener.OnTouchMapListener;
import com.coretec.sensing.utils.Const;

import static com.coretec.sensing.utils.Const.BOTTOM_BLANK_METER;
import static com.coretec.sensing.utils.Const.BOTTOM_BLANK_PIXEL;
import static com.coretec.sensing.utils.Const.LEFT_BLANK_METER;
import static com.coretec.sensing.utils.Const.LEFT_BLANK_PIXEL;
import static com.coretec.sensing.utils.Const.METER_PER_PIXEL;
import static com.coretec.sensing.utils.Const.PIXEL_PER_METER;

public class MapControlView extends AppCompatImageView implements View.OnTouchListener {

    //경로 그리기
    private Paint paint;
    private Path path;

    public Matrix matrix = new Matrix();
    public Matrix savedMatrix2 = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private int mode = Const.NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private boolean isFirst;
    private boolean isInit = false;
    private MoveImageView moveImageView;
    private MapActivity mapActivity;
    private OnTouchMapListener onTouchMapListener;

    //롱클릭 이벤트 구현
    private Handler mHandler = new Handler();
    private LongPressCheckRunnable longPressCheckRunnable = new LongPressCheckRunnable();
    private int longPressTimeout;
    private int scaledTouchSlope;
    private boolean isLongPressed = false;

    public MapControlView(Context context) {
        super(context);
        setOnTouchListener(this);
        mapActivity = (MapActivity) context;

        if (Looper.myLooper() != Looper.getMainLooper())
            throw new RuntimeException();
        longPressTimeout = ViewConfiguration.getLongPressTimeout() * 2;
        scaledTouchSlope = ViewConfiguration.get(context).getScaledTouchSlop();
        initPath();
    }

    public MapControlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
        mapActivity = (MapActivity) context;

        if (Looper.myLooper() != Looper.getMainLooper())
            throw new RuntimeException();
        longPressTimeout = ViewConfiguration.getLongPressTimeout() * 2;
        scaledTouchSlope = ViewConfiguration.get(context).getScaledTouchSlop();
        initPath();
    }

    public MapControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setOnTouchListener(this);
        mapActivity = (MapActivity) context;

        if (Looper.myLooper() != Looper.getMainLooper())
            throw new RuntimeException();
        longPressTimeout = ViewConfiguration.getLongPressTimeout() * 2;
        scaledTouchSlope = ViewConfiguration.get(context).getScaledTouchSlop();
        initPath();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        canvas.setMatrix(matrix);
        canvas.drawPath(path, paint);

        Drawable drawable = getDrawable();
        if (drawable == null)
            return;

//        Log.d("맵 사이즈", drawable.getIntrinsicWidth() + " X " + drawable.getIntrinsicHeight());
//        Log.d("맵 사이즈2", getWidth() + " X " + getHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInit) {
            init();
            isInit = true;
        }
    }

    public void initPath() {
        paint = new Paint();
        path = new Path();
        isFirst = true;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(30f);
        paint.setColor(Color.CYAN);
    }

    public void addPath(float input_x, float input_y) {
        float scaleX = (getDrawable().getIntrinsicWidth() / 2848f);
        float scaleY = (getDrawable().getIntrinsicHeight() / 4574f);

        input_x *= scaleX;
        input_y *= scaleY;

        if (isFirst) {
            path.moveTo(input_x, input_y);
            isFirst = false;
        } else {
            path.lineTo(input_x, input_y);
        }
        invalidate();
    }

    public float[] pointPixelToMeter(float[] point) {
        int parentWidth = getDrawable().getIntrinsicWidth();
        int parentHeight = getDrawable().getIntrinsicHeight();

        float scaleX = (parentWidth / 2848f);
        float scaleY = (parentHeight / 4574f);

        float reverseY = (parentHeight - point[1]) / scaleY;

        point[0] /= scaleX;
        point[1] /= scaleY;

        return new float[]{(float) ((point[0] * PIXEL_PER_METER) - LEFT_BLANK_METER), (float) ((reverseY * PIXEL_PER_METER) - BOTTOM_BLANK_METER)};
    }

    public int[] pointMeterToPixel(double[] point) {
        int parentWidth = getDrawable().getIntrinsicWidth();
        int parentHeight = getDrawable().getIntrinsicHeight();

        float scaleX = (parentWidth / 2848f);
        float scaleY = (parentHeight / 4574f);

        point[0] = (point[0] * METER_PER_PIXEL) + LEFT_BLANK_PIXEL;
        point[1] = (point[1] * METER_PER_PIXEL) + BOTTOM_BLANK_PIXEL;

        point[0] *= scaleX;
        point[1] = parentHeight - (point[1] * scaleY);

        point[0] /= scaleX;
        point[1] /= scaleY;

        return new int[]{(int) point[0], (int) point[1]};
    }

    private float[] getTouchPixel(float srcX, float srcY, Matrix matrix) {
        //터치 좌표
        float[] touchLocation = new float[]{srcX, srcY};

        //터치 좌표를 이미지 해상도 좌표로 변환
        Matrix inverse = new Matrix();
        matrix.invert(inverse);
        inverse.mapPoints(touchLocation);

        Log.d("터치 픽셀", touchLocation[0] + " - " + touchLocation[1]);

        return touchLocation;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ((InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
//        Log.d("이미지 사이즈", getWidth() + " X " + getAltitude());
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = Const.DRAG;

                float[] value = new float[9];
                matrix.getValues(value);

                moveImageView = mapActivity.getImageViewPosition(event.getX(), event.getY(), matrix, value[0]);

                if (moveImageView != null)
                    startTimeout();
                break;

            case MotionEvent.ACTION_MOVE:
                if (moveImageView != null) {

                    if (Math.abs(event.getX() - start.x) > scaledTouchSlope || Math.abs(start.y - event.getY()) > scaledTouchSlope)
                        stopTimeout();

                    value = new float[9];
                    matrix.getValues(value);

                    float[] pixelPoint = getTouchPixel(event.getX(), event.getY() + 300, matrix);
                    float[] meterPoint = pointPixelToMeter(pixelPoint);

                    Log.d("터치 -> 미터 변환", meterPoint[0] + " - " + meterPoint[1]);

                    mapActivity.setImageViewPosition(moveImageView, pixelPoint, meterPoint);
                    return true;
                }

                switch (mode) {
                    case Const.DRAG:
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                        break;

                    case Const.ZOOM:
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (moveImageView != null) {
                    moveImageView = null;
                    return true;
                }

                //10픽셀 이상 움직이지 않았을 경우 한 부분 터치로 인식 함
                if (start.x + 10 > event.getX() && start.x - 10 < event.getX() && start.y + 10 > event.getY() && start.y - 10 < event.getY()) {
                    //포인트 추가
                    Log.d("포인트 추가", "추가+++");
                    mapActivity.addTouchPoint(getTouchPixel(event.getX(), event.getY(), matrix));

                    //해당 좌표로 이동
//                    matrix.postTranslate(touchLocation[0], touchLocation[1]);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
//                Log.d("MotionEvent", "Pointer Down");
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(event);
                    mode = Const.ZOOM;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = Const.NONE;
                break;

            case MotionEvent.ACTION_CANCEL:
                stopTimeout();
                break;
        }


        if (moveImageView == null) {
            // 매트릭스 값 튜닝.
            matrixTurning(matrix);
            setImageMatrix(matrix);

            if (onTouchMapListener != null) {
                onTouchMapListener.onTouchMap();
            }
        }

        return true;
    }


    public void init() {
        matrixTurning(matrix);
        setImageMatrix(matrix);
    }

    public void setOnTouchMapView(OnTouchMapListener onTouchMapListener) {
        this.onTouchMapListener = onTouchMapListener;
        setImagePit(matrix);
    }

    //이미지 핏
    public void setImagePit(Matrix matrix) {
        // 매트릭스 값
        float[] value = new float[9];
        matrix.getValues(value);

        // 초기 확대 배율
        value[0] = value[4] = 1f;

        // 이미지 크기
        Drawable drawable = getDrawable();
        if (drawable == null)
            return;

        matrix.setValues(value);
        setImageMatrix(matrix);
    }

    public void initPosition(int x, int y) {
        float[] value = new float[9];
        matrix.getValues(value);

        value[0] = value[4] = 1f;

        matrix.setValues(value);
        matrix.postTranslate(-x, -y);

        matrixTurning(matrix);
        setImageMatrix(matrix);
    }


    public void setPointPosition(int x, int y) {
        float[] value = new float[9];
        matrix.getValues(value);

        value[0] = value[4] = 1f;

        float scaleX = getDrawable().getIntrinsicWidth() / 2848f;
        float scaleY = getDrawable().getIntrinsicHeight() / 4574f;

        Log.d("줌 상태", value[0] + "");

        //나온 결과값은 좌측상단을 기준으로 하기 때문에
        //중심에 표출하기 위해 +800을 추가함
        value[2] = (int) -(x * scaleX) + (getWidth() / 2f);
        value[5] = (int) -(y * scaleY) + (getHeight() / 2f);


//        Log.d("Size", "Width : " + width + ", Height : " + height);
//        Log.d("Translate Position", "X : " + value[2] + ", Y : " + value[5]);
//        matrix.postTranslate(posX, posY);
        matrix.setValues(value);
//        matrix.postTranslate(-x, -y);

        matrixTurning(matrix);
        setImageMatrix(matrix);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        mid.set(x / 2, y / 2);
    }

    private void matrixTurning(Matrix matrix) {

        float[] value = new float[9];
        matrix.getValues(value);

        float[] savedValue = new float[9];
        savedMatrix2.getValues(savedValue);

        // 뷰 크기
        int width = getWidth();
        int height = getHeight();

        // 이미지 크기
        Drawable drawable = getDrawable();
        if (drawable == null)
            return;

        int imageWidth = drawable.getIntrinsicWidth();
        int imageHeight = drawable.getIntrinsicHeight();

        int scaleWidth = (int) (imageWidth * value[0]);
        int scaleHeight = (int) (imageHeight * value[4]);

        // 이미지가 바깥으로 나가지 않도록.
        if (value[2] < width - scaleWidth) value[2] = width - scaleWidth;
        if (value[5] < height - scaleHeight) value[5] = height - scaleHeight;

        value[2] = value[2] > 0 ? 0 : value[2];
        value[5] = value[5] > 0 ? 0 : value[5];

        // 2.5배 이상 확대 하지 않도록
        if (value[0] > 2.5f || value[4] > 2.5f) {
            value[0] = savedValue[0];
            value[4] = savedValue[4];
            value[2] = savedValue[2];
            value[5] = savedValue[5];
        }

        if (imageHeight > height) {
            if (scaleHeight < height) {
                value[0] = value[4] = (float) height / imageHeight;

                scaleHeight = (int) (imageHeight * value[4]);

                if (scaleHeight > height)
                    value[0] = value[4] = (float) height / imageHeight;
            }
        }
        // 원래부터 작은 얘들은 본래 크기보다 작게 하지 않도록
        else {
            if (value[0] < 1) value[0] = 1;
            if (value[4] < 1) value[4] = 1;
        }

        scaleHeight = (int) (imageHeight * value[4]);

        if (scaleHeight < height) {
            value[5] = (float) height / 2 - (float) scaleHeight / 2;
        }

        matrix.setValues(value);
        savedMatrix2.set(matrix);
    }


    public void startTimeout() {
        isLongPressed = false;
        mHandler.postDelayed(longPressCheckRunnable, longPressTimeout);
    }


    public void stopTimeout() {
        if (!isLongPressed)
            mHandler.removeCallbacks(longPressCheckRunnable);
    }


    private class LongPressCheckRunnable implements Runnable {
        @Override
        public void run() {
            isLongPressed = true;
            if (moveImageView != null) {
                moveImageView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                mapActivity.deleteImageView(moveImageView);

                //터치 이벤트 강제로 종료
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Instrumentation instrumentation = new Instrumentation();
                        MotionEvent event = MotionEvent.obtain(0, 0,
                                MotionEvent.ACTION_UP, 0, 0, 0);
                        instrumentation.sendPointerSync(event);
                    }
                }).start();
            }
        }
    }

}