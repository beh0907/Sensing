package com.coretec.sensing.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.coretec.sensing.utils.Const;

import static com.coretec.sensing.utils.Const.MAP_HEIGHT;
import static com.coretec.sensing.utils.Const.MAP_WIDTH;


public class MoveImageView extends AppCompatImageView implements View.OnTouchListener {
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private Matrix parentMatrix;

    private int parentWidth;
    private int parentHeight;
    private int posX;
    private int posY;

    private PointF start = new PointF();

    private int mode = Const.NONE;

    private boolean isInit = false;

    public MoveImageView(Context context) {
        super(context);
    }

    public MoveImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MoveImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoveImageView(Matrix parentMatrix, int parentWidth, int parentHeight, int posX, int posY, Context context) {
        super(context);
        this.parentMatrix = parentMatrix;
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        this.posX = posX;
        this.posY = posY;

    }

    public int[] getPosLocation() {
        return new int[]{posX, posY};
    }

    public void setPosLocation(float[] posLocation) {
        posX = (int) posLocation[0];
        posY = (int) posLocation[1];
        initPosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        canvas.setMatrix(matrix);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInit) {
            initPosition();
            isInit = true;
        }
    }

    public void initPosition() {
        // 매트릭스 값
        float[] value = new float[9];
        matrix.getValues(value);

        float[] parentValue = new float[9];
        parentMatrix.getValues(parentValue);


        int imageWidth = getDrawable().getIntrinsicWidth();
        int imageHeight = getDrawable().getIntrinsicHeight();

//        Log.d("Parent", "Width : " + parentWidth + ", Height : " + parentHeight);
//        Log.d("Image", "Width : " + imageWidth + ", Height : " + imageHeight);

        // 초기 확대 배율
        value[0] = value[4] = 1f;

        float scaleX = parentValue[0] * (parentWidth / MAP_WIDTH);
        float scaleY = parentValue[4] * (parentHeight / MAP_HEIGHT);

//        Log.d("Scale", "X : " + scaleX + ", Y : " + scaleY);

        value[2] = parentValue[2] + (int) (posX * scaleX) - (imageWidth / 2);
        value[5] = parentValue[5] + (int) (posY * scaleY) - (imageHeight / 2);

        matrix.setValues(value);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());

                mode = Const.DRAG;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == Const.DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                }
                break;

            case MotionEvent.ACTION_UP:
                mode = Const.NONE;
                break;
        }


        // 매트릭스 값 튜닝.
//        matrixTurning(matrix);
        setImageMatrix(matrix);

        return true;
    }
}

