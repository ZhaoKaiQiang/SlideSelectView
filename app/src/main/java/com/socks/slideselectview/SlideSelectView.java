package com.socks.slideselectview;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * Created by zhaokaiqiang on 15/2/10.
 */
public class SlideSelectView extends View {

	//小圆半径
	private static final float RADIU_SMALL = 15;
	//大圆半径
	private static final float RADIU_BIG = 30;
	//线的高度
	private static float HEIGHT_LINE = 10;
	//线距离两头的边距
	private static float MARGEN_LINE = RADIU_BIG * 3;
	//小圆的数量
	private int countOfSmallCircle;
	//小圆的横坐标
	private float circlesX[];
	private Context mContext;
	//画笔
	private Paint mPaint;
	//文字画笔
	private TextPaint mTextPaint;
	//控件高度
	private float mHeight;
	//控件宽度
	private float mWidth;
	//大圆的横坐标
	private float bigCircleX;
	//是否是手指跟随模式
	private boolean isFollowMode;
	//手指按下的x坐标
	private float startX;
	//文字大小
	private float textSize;
	//文字宽度
	private float textWidth;
	//当前大球距离最近的位置
	private int currentPosition;
	//小圆之间的间距
	private float distanceX;
	//利率文字
	private String[] text4Rates;
	//依附效果实现
	private ValueAnimator valueAnimator;
	//用于纪录松手后的x坐标
	private float currentPositionX;

	private onSelectListener selectListener;


	public SlideSelectView(Context context) {
		this(context, null);
	}

	public SlideSelectView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideSelectView);
		countOfSmallCircle = a.getInt(R.styleable.SlideSelectView_circleCount, 5);
		textSize = a.getInt(R.styleable.SlideSelectView_textSize, (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
		a.recycle();

		mPaint = new Paint();
		mPaint.setColor(Color.WHITE);
		mPaint.setAntiAlias(true);

		textSize = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics());

		mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTextSize(textSize);

		currentPosition = countOfSmallCircle / 2;

	}

	/**
	 * 设置显示文本
	 *
	 * @param strings
	 */
	public void setString(String[] strings) {
		text4Rates = strings;
		textWidth = mTextPaint.measureText(text4Rates[0]);

		if (countOfSmallCircle != text4Rates.length) {
			throw new IllegalArgumentException("the count of small circle must be equal to the " +
					"text array length !");
		}

	}

	/**
	 * 设置监听器
	 *
	 * @param listener
	 */
	public void setOnSelectListener(onSelectListener listener) {
		selectListener = listener;
	}


	@Override
	protected void onDraw(Canvas canvas) {

		//画中间的线
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(HEIGHT_LINE);
		canvas.drawLine(MARGEN_LINE, mHeight / 2, mWidth - MARGEN_LINE, mHeight / 2,
				mPaint);
		//画小圆
		mPaint.setStyle(Paint.Style.FILL);
		for (int i = 0; i < countOfSmallCircle; i++) {
			canvas.drawCircle(circlesX[i], mHeight / 2, RADIU_SMALL, mPaint);
		}
		//画大圆的默认位置
		canvas.drawCircle(bigCircleX, mHeight / 2, RADIU_BIG, mPaint);
		//画文字
		canvas.drawText(text4Rates[currentPosition], circlesX[currentPosition] - textWidth / 2,
				(mHeight / 2) - RADIU_BIG -
						RADIU_SMALL,
				mTextPaint);

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:

				startX = event.getX();
				//如果手指按下的x坐标与大圆的x坐标的距离小于半径，则是follow模式
				if (Math.abs(startX - bigCircleX) <= RADIU_BIG) {
					isFollowMode = true;
				} else {
					isFollowMode = false;
				}

				break;
			case MotionEvent.ACTION_MOVE:

				//如果是follow模式，则大圆跟随手指移动
				if (isFollowMode) {
					//防止滑出边界
					if (event.getX() >= MARGEN_LINE && event.getX() <= (mWidth - MARGEN_LINE)) {
						//Log.d("TAG", "event.getX()=" + event.getX() + "__mWidth=" + mWidth);
						bigCircleX = event.getX();
						int position = (int) ((event.getX() - MARGEN_LINE) / (distanceX / 2));
						//更新当前位置
						currentPosition = (position + 1) / 2;
						invalidate();
					}

				}

				break;
			case MotionEvent.ACTION_UP:

				if (isFollowMode) {
					float endX = event.getX();
					//当前位置距离最近的小白点的距离
					float currentDistance = endX - MARGEN_LINE - currentPosition * distanceX;

					if ((currentPosition == 0 && currentDistance < 0) || (currentPosition == (text4Rates.length - 1) && currentDistance > 0)) {

						if (null != selectListener) {
							selectListener.onSelect(currentPosition);
						}
						return true;
					}

					currentPositionX = bigCircleX;

					valueAnimator = ValueAnimator.ofFloat(currentDistance);
					valueAnimator.setInterpolator(new AccelerateInterpolator());
					valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							float slideDistance = (float) animation.getAnimatedValue();
							bigCircleX = currentPositionX - slideDistance;
							invalidate();
						}
					});

					valueAnimator.setDuration(100);
					valueAnimator.start();
					if (null != selectListener) {
						selectListener.onSelect(currentPosition);
					}
				}

				break;
		}


		return true;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mHeight = h;
		mWidth = w;
		//计算每个小圆点的x坐标
		circlesX = new float[countOfSmallCircle];
		distanceX = (mWidth - MARGEN_LINE * 2) / (countOfSmallCircle - 1);
		for (int i = 0; i < countOfSmallCircle; i++) {
			circlesX[i] = i * distanceX + MARGEN_LINE;
		}

		bigCircleX = circlesX[currentPosition];
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int screenSize[] = getScreenSize((Activity) mContext);

		int resultWidth;
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);

		if (widthMode == MeasureSpec.EXACTLY) {
			resultWidth = widthSize;
		} else {
			resultWidth = screenSize[0];

			if (widthMode == MeasureSpec.AT_MOST) {
				resultWidth = Math.min(widthSize, screenSize[0]);
			}
		}

		int resultHeight;
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		if (heightMode == MeasureSpec.EXACTLY) {
			resultHeight = heightSize;
		} else {
			resultHeight = (int) (RADIU_BIG * 6);

			if (heightMode == MeasureSpec.AT_MOST) {
				resultHeight = Math.min(heightSize, resultHeight);
			}
		}

		setMeasuredDimension(resultWidth, resultHeight);

	}

	private static int[] getScreenSize(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return new int[]{metrics.widthPixels, metrics.heightPixels};
	}

	public interface onSelectListener {
		public void onSelect(int index);
	}

}