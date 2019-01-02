package com.yzh.revenuechart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.Scroller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 作者：YZH
 * <p>
 * 创建时间：2019/1/1 10:13
 * <p>
 * 描述：
 * <p>
 * 修订历史：
 */
public class BrokenLineView extends View {

    //画笔
    private Paint mTextPaint;
    private Paint mBodyPaint;
    private Paint mHorizontalPaint;
    private Paint mBgPaint;


    private int mSelectItem;

    private DisplayMetrics dm;
    //最大值
    private float maxValue;
    //行数
    private int mLines;
    private ArrayList<String> xRawData;
    private ArrayList<ArrayList<Double>> mYDatas;

    //圆点半径
    private float mSpotRadius;
    private float mSpotInnerRadius;

    private int mTextHeight;
    private int mLeftTextMaxWidth;
    private int mBottomLastTextWidth;
    private float mDx;
    private float mDy;
    //宽高比
    private float mAspectRatio;
    private float mLeftPadding;
    private float mBottomPadding;
    private float mTextSize;
    private Paint.FontMetrics mFontMetrics;

    private int mClickRange;
    private ArrayList<ArrayList<Rect>> mClickRects;
    private boolean mShowGradation;
    private float mTopPadding;
    private float mRightPadding;
    private int mTextColor;
    private int mTextSelectedColor;
    private int mCurveLineColor;
    private int mHorizontalLineColor;
    private int mTextBgColor;
    private int mSelectIndex;
    private Runnable mRunnable;
    private float mX;
    private int mScrollX;
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    private int mDw;

    public BrokenLineView(Context context) {
        this(context, null);
    }

    public BrokenLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        //取出自定义的属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BrokenLineStyle);

        mAspectRatio = typedArray.getFloat(R.styleable.BrokenLineStyle_AspectRatio, 0.8f);
        mLines = typedArray.getInteger(R.styleable.BrokenLineStyle_Lines, 4);
        mSpotRadius = typedArray.getDimension(R.styleable.BrokenLineStyle_SpotRadius, dip2px(3.5f));
        mSpotInnerRadius = typedArray.getDimension(R.styleable.BrokenLineStyle_SpotInnerRadius, dip2px(1.75f));
        mClickRange = typedArray.getDimensionPixelSize(R.styleable.BrokenLineStyle_ClickRange, (int) dip2px(12));
        mTextSize = typedArray.getDimension(R.styleable.BrokenLineStyle_TextSize, dip2px(11));
        mShowGradation = typedArray.getBoolean(R.styleable.BrokenLineStyle_ShowGradation, true);

        mLeftPadding = typedArray.getDimension(R.styleable.BrokenLineStyle_LeftPadding, dip2px(8));
        mBottomPadding = typedArray.getDimension(R.styleable.BrokenLineStyle_BottomPadding, dip2px(8));
        mTopPadding = typedArray.getDimension(R.styleable.BrokenLineStyle_TopPadding, dip2px(25));
        mRightPadding = typedArray.getDimension(R.styleable.BrokenLineStyle_RightPadding, dip2px(12));

        mTextColor = typedArray.getColor(R.styleable.BrokenLineStyle_TextColor, 0xFF505452);
        mTextSelectedColor = typedArray.getColor(R.styleable.BrokenLineStyle_TextSelectedColor, 0xFFFF6825);
        mCurveLineColor = typedArray.getColor(R.styleable.BrokenLineStyle_CurveLineColor, 0xFFFF6825);
        mTextBgColor = typedArray.getColor(R.styleable.BrokenLineStyle_TextBgColor, 0xFFFF6825);
        mHorizontalLineColor = typedArray.getColor(R.styleable.BrokenLineStyle_HorizontalLineColor, 0xFFEBEBEB);
        typedArray.recycle();

        initView();
    }

    private void initView() {
        this.mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mHorizontalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mFontMetrics = mTextPaint.getFontMetrics();

        mBodyPaint.setStrokeWidth(dip2px(1));
        mBodyPaint.setStyle(Paint.Style.STROKE);
        mBodyPaint.setColor(mCurveLineColor);

        mHorizontalPaint.setStrokeWidth(dip2px(1));
        mHorizontalPaint.setColor(mHorizontalLineColor);

        mBgPaint.setColor(mTextBgColor);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setMaskFilter(new BlurMaskFilter(dip2px(2), BlurMaskFilter.Blur.SOLID));

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int h;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            w = MeasureSpec.getSize(widthMeasureSpec);
            h = MeasureSpec.getSize(heightMeasureSpec);
        } else if (widthMode == MeasureSpec.EXACTLY) {
            w = MeasureSpec.getSize(widthMeasureSpec);
            h = (int) (w * mAspectRatio);
        } else if (heightMode == MeasureSpec.EXACTLY) {
            h = MeasureSpec.getSize(heightMeasureSpec);
            BigDecimal hDecimal = new BigDecimal(h);
            BigDecimal ratio = new BigDecimal(mAspectRatio);
            w = (int) hDecimal.divide(ratio, 2, RoundingMode.CEILING).floatValue();
        } else {
            int selfWidth = MeasureSpec.getSize(widthMeasureSpec);
            int selfHeight = MeasureSpec.getSize(heightMeasureSpec);
            if (selfWidth + selfHeight == 0) {
                w = dm.widthPixels;
                h = (int) (w * mAspectRatio);
            } else if (selfWidth == 0) {
                h = selfHeight;
                BigDecimal hDecimal = new BigDecimal(h);
                BigDecimal ratio = new BigDecimal(mAspectRatio);
                w = (int) hDecimal.divide(ratio, 2, RoundingMode.CEILING).floatValue();
            } else {
                w = selfWidth;
                h = (int) (w * mAspectRatio);
            }
        }
        //保存测量结果
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (xRawData == null || mYDatas == null) {
            return;
        }
        //计算需要用到的数值
        initData();
        // 画直线（横向）
        drawAllXLine(canvas);
        // 画直线（纵向）
        drawAllYLine(canvas);
        mClickRects = new ArrayList<>();
        for (int i = 0; i < mYDatas.size(); i++) {
            ArrayList<Double> yData = mYDatas.get(i);
            // 获取各个点的坐标
            Point[] points = getPoints(yData);
            if (mShowGradation) {
                //绘制渐变
                drawGradation(canvas, points);
                //绘制曲线
                drawCurve(canvas, points);
            } else {
                //绘制直线
                drawLine(canvas, points);
            }
            //绘制点和其它
            drawSpot(canvas, points, yData, i);

        }
        if (mRunnable != null) {
            mBodyPaint.setStyle(Paint.Style.FILL);
            mRunnable.run();
            mBodyPaint.setStyle(Paint.Style.STROKE);
        }
    }

    //绘制点和其它
    private void drawSpot(Canvas canvas, Point[] points, ArrayList<Double> yData, int index) {
        ArrayList<Rect> rects = new ArrayList<>();

        for (int i = 0; i < points.length; i++) {
            mBodyPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(points[i].x, points[i].y, mSpotRadius, mBodyPaint);
            if (i == mSelectItem && mSelectIndex == index) {
                int finalI = i;
                mRunnable = () -> {
                    float d = dip2px(5);
                    Rect rect = new Rect();
                    String text = getMoney(yData.get(finalI));
                    mTextPaint.getTextBounds(text, 0, text.length(), rect);
                    float dx = 0;
                    if (points[finalI].x + rect.width() / 2f > getWidth() - d) {
                        dx = getWidth() - points[finalI].x - rect.width() / 2f - d * 2;
                    }

                    if (points[finalI].x - d < rect.width() / 2f) {
                        dx = rect.width() / 2f - points[finalI].x + d * 2;
                    }


                    mBgPaint.setPathEffect(new CornerPathEffect(d / 4));
                    Path path = new Path();
                    float y = points[finalI].y - mSpotRadius;
                    path.moveTo(points[finalI].x, y);
                    path.rLineTo(d * 1.5f, -d * 1.5f);
                    path.rLineTo(-3 * d, 0);
                    path.close();
                    canvas.drawPath(path, mBgPaint);

                    mBgPaint.setPathEffect(new CornerPathEffect(d));
                    path = new Path();
                    path.moveTo(points[finalI].x, y - d);
                    path.rLineTo(rect.width() / 2f + dx + d, 0);
                    path.rLineTo(0, -rect.height() - d * 2);
                    path.rLineTo(-rect.width() - d * 2, 0);
                    path.rLineTo(0, rect.height() + d * 2);
                    path.close();

                    canvas.drawPath(path, mBgPaint);

                    mTextPaint.setColor(0xffffffff);
                    canvas.drawText(text, points[finalI].x - rect.width() / 2f + dx, points[finalI].y - rect.height() - d, mTextPaint);
                    mTextPaint.setColor(mTextColor);
                    mBodyPaint.setColor(0xffffffff);
                    canvas.drawCircle(points[finalI].x, points[finalI].y, mSpotInnerRadius, mBodyPaint);
                    mBodyPaint.setColor(mCurveLineColor);
                };

            }


            Rect rect = new Rect(points[i].x - mClickRange, points[i].y - mClickRange, points[i].x + mClickRange, points[i].y + mClickRange);
            rects.add(rect);
        }
        mBodyPaint.setStyle(Paint.Style.STROKE);
        mClickRects.add(rects);
    }

    private void initData() {


        double tmp = 0;
        for (ArrayList<Double> yData : mYDatas) {
            for (Double aDouble : yData) {
                if (aDouble > tmp) {
                    tmp = aDouble;
                }
            }
        }

        if (tmp >= mLines) {
            int v = (int) (tmp / mLines);
            int length = (v + "").length() - 1;
            int i = (int) ((v / (int) Math.pow(10, length) + 1) * Math.pow(10, length));
            maxValue = i * mLines;
        } else {
            maxValue = mLines;
        }


        Rect rect = new Rect();
        mLeftTextMaxWidth = 0;

        for (int i = 0; i < mLines + 1; i++) {
            String text = getYText(maxValue / mLines * i);
            mTextPaint.getTextBounds(text, 0, text.length(), rect);
            if (mLeftTextMaxWidth < rect.width()) {
                mLeftTextMaxWidth = rect.width();
            }

        }
        mTextHeight = rect.height();

        String text = xRawData.get(xRawData.size() - 1);
        mTextPaint.getTextBounds(text, 0, text.length(), rect);
        mBottomLastTextWidth = rect.width();


        mDx = (getWidth() - getPaddingLeft() - getPaddingRight() - mRightPadding - mLeftPadding - mLeftTextMaxWidth - mBottomLastTextWidth / 2f) / (xRawData.size() - 1);
        mDy = (getHeight() - getPaddingTop() - getPaddingBottom() - mTopPadding - mBottomPadding - mFontMetrics.bottom - mTextHeight * 2) / mLines;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //手指位置地点
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                View parent = (View) getParent();
                mDw = getWidth() - parent.getWidth();
                if (mDw > 0) {
                    mScroller.forceFinished(true);
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(event);
                    }
                    mScrollX = getScrollX();
                    mX = event.getX();
                }
                showInfo(event);

                break;
            case MotionEvent.ACTION_MOVE:
                if (mDw > 0) {
                    mVelocityTracker.addMovement(event);

                    float x = event.getX();
                    int dx = (int) (mX - x + mScrollX);
                    if (dx < 0) {
                        dx = 0;
                    }
                    if (dx > mDw) {
                        dx = mDw;
                    }
                    scrollTo(dx, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDw > 0) {
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    int velocityX = (int) mVelocityTracker.getXVelocity();
                    mScroller.fling(getScrollX(), 0, -velocityX, 0, 0, mDw, 0, 0);
                    postInvalidate();
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        // 如果返回true，表示动画还没有结束
        // 因为前面startScroll，所以只有在startScroll完成时 才会为false
        if (mScroller.computeScrollOffset()) {
            // 产生了动画效果 每次滚动一点
            scrollTo(mScroller.getCurrX(), 0);
            //刷新View 否则效果可能有误差
            postInvalidate();
        }
    }

    private void showInfo(MotionEvent event) {
        int item = -1;
        Map<Integer, Float> index = new HashMap<>();
        for (int i = mClickRects.size() - 1; i >= 0; i--) {
            ArrayList<Rect> rects = mClickRects.get(i);

            float x = event.getX() + mScrollX;
            float y = event.getY();
            if (item != -1) {
                Rect rect = rects.get(item);
                if (rect.contains((int) x, (int) y)) {
                    float dx = rect.centerX() - x;
                    float dy = rect.centerY() - y;
                    index.put(i, dx * dx + dy * dy);
                    break;
                }
                continue;
            }

            for (int j = 0; j < rects.size(); j++) {
                Rect rect = rects.get(j);
                if (rect.contains((int) x, (int) y)) {
                    float dx = rect.centerX() - x;
                    float dy = rect.centerY() - y;
                    mSelectItem = j;
                    item = j;
                    index.put(i, dx * dx + dy * dy);
                    break;
                }
            }
        }
        if (!index.isEmpty()) {
            Iterator<Map.Entry<Integer, Float>> iterator = index.entrySet().iterator();
            float temp = Float.MAX_VALUE;
            while (iterator.hasNext()) {
                Map.Entry<Integer, Float> next = iterator.next();
                Float value = next.getValue();
                if (value < temp) {
                    temp = value;
                    mSelectIndex = next.getKey();
                }
            }
            if (mOnclickListener != null) {
            }
        }
        invalidate();

    }

    /**
     * 画所有横向表格，包括X轴
     */
    private void drawAllXLine(Canvas canvas) {
        for (int i = 0; i < mLines + 1; i++) {
            String text = getYText(maxValue / mLines * (mLines - i));
            Rect rect = new Rect();
            mTextPaint.getTextBounds(text, 0, text.length(), rect);

            float ty = rect.height() + mDy * i + getPaddingTop() + mTopPadding;
            canvas.drawText(text, getPaddingLeft() + mLeftTextMaxWidth - rect.width(), ty, mTextPaint);

            float y = ty - mTextHeight / 2f;
            canvas.drawLine(getPaddingLeft() + mLeftTextMaxWidth + mLeftPadding, y, getWidth() - getPaddingRight() - mRightPadding - mBottomLastTextWidth / 2f, y, mHorizontalPaint);// Y坐标

        }
    }

    /**
     * 画所有纵向表格，包括Y轴
     */

    private void drawAllYLine(Canvas canvas) {

        Rect rect = new Rect();
        for (int i = 0; i < xRawData.size(); i++) {
            mTextPaint.getTextBounds(xRawData.get(i), 0, xRawData.get(i).length(), rect);
            if (i == mSelectItem) {
                mTextPaint.setColor(mTextSelectedColor);
                canvas.drawText(xRawData.get(i), getPaddingLeft() + mLeftTextMaxWidth + mLeftPadding + mDx * i - rect.width() / 2f, getHeight() - getPaddingBottom() - mFontMetrics.bottom, mTextPaint);
                mTextPaint.setColor(mTextColor);
            } else {
                canvas.drawText(xRawData.get(i), getPaddingLeft() + mLeftTextMaxWidth + mLeftPadding + mDx * i - rect.width() / 2f, getHeight() - getPaddingBottom() - mFontMetrics.bottom, mTextPaint);
            }
        }
    }

    //得到路径
    private Path getPath(Point[] points, boolean close) {
        Path path = new Path();
        path.moveTo(points[0].x, points[0].y);

        for (int i = 0; i < points.length - 1; i++) {
            float wt = (points[i].x + points[i + 1].x) / 2f;

            path.cubicTo(wt, points[i].y, wt, points[i + 1].y, points[i + 1].x, points[i + 1].y);
        }
        if (close) {
            float y = getHeight() - mTextHeight * 1.5f - getPaddingBottom() - mFontMetrics.bottom - mBottomPadding;
            path.lineTo(points[points.length - 1].x, y);
            path.lineTo(points[0].x, y);
            path.lineTo(points[0].x, points[0].y);
        }
        return path;
    }

    //绘制曲线
    private void drawCurve(Canvas canvas, Point[] points) {
        Path path = getPath(points, false);
        canvas.drawPath(path, mBodyPaint);
    }

    //绘制渐变
    private void drawGradation(Canvas canvas, Point[] points) {
        Path path = getPath(points, true);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        float y = getHeight() - mTextHeight * 1.5f - getPaddingBottom() - mFontMetrics.bottom - mBottomPadding;
        LinearGradient lg = new LinearGradient(0, y, 0, mTextHeight / 2f + getPaddingTop() + mTopPadding, 0x16FF6825, 0x5eFF6825, Shader.TileMode.MIRROR);
        paint.setShader(lg);
        canvas.drawPath(path, paint);
    }

    //绘制直线
    private void drawLine(Canvas canvas, Point[] points) {
        for (int i = 0; i < points.length - 1; i++) {
            canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, mBodyPaint);
        }
    }

    private Point[] getPoints(ArrayList<Double> yRawData) {
        Point[] points = new Point[yRawData.size()];
        for (int i = 0; i < yRawData.size(); i++) {
            points[i] = new Point((int) (getPaddingLeft() + mLeftTextMaxWidth + mLeftPadding + mDx * i + 0.5), (int) (mTextHeight / 2f + (mLines - (yRawData.get(i) / (maxValue / mLines))) * mDy + 0.5 + getPaddingTop() + mTopPadding));
        }
        return points;
    }

    public void setLines(int lines) {
        mLines = lines;
        invalidate();
    }

    public void setxRawData(ArrayList<String> xRawData) {
        this.xRawData = xRawData;
        mSelectItem = xRawData.size() - 1;
        invalidate();
    }

    public void setyRawData(ArrayList<Double> yRawData) {
        mYDatas = new ArrayList<>();
        mYDatas.add(yRawData);
        mSelectIndex = mYDatas.size() - 1;
        invalidate();
    }

    public void addyRawData(ArrayList<Double> yRawData) {
        if (mYDatas == null) {
            mYDatas = new ArrayList<>();
        }
        mYDatas.add(yRawData);
        mSelectIndex = mYDatas.size() - 1;
        invalidate();
    }

    private String getMoney(double num) {

        DecimalFormat formater = new DecimalFormat("#0.00");
        formater.setRoundingMode(RoundingMode.FLOOR);


        if (num >= 100000000) {
            return formater.format(num / 100000000) + "亿元";
        }
        if (num >= 10000) {
            return formater.format(num / 10000) + "万元";
        }
        return formater.format(num) + "元";
    }

    private String getYText(double num) {

        DecimalFormat formater = new DecimalFormat("#0.00");
        formater.setRoundingMode(RoundingMode.FLOOR);


        if (num >= 100000000) {
            return formater.format(num / 100000000) + "E";
        }
        if (num >= 10000) {
            return formater.format(num / 10000) + "W";
        }
        if (num >= 1000) {
            return formater.format(num / 1000) + "k";
        }
        return formater.format(num);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private float dip2px(float dpValue) {
        return dpValue * dm.density;
    }

    private OnclickListener mOnclickListener;

    public void setOnclickListener(OnclickListener onclickListener) {
        mOnclickListener = onclickListener;
    }

    public interface OnclickListener {
        void click();
    }
}
