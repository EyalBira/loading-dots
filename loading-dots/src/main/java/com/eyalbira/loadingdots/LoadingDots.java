package com.eyalbira.loadingdots;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Customizable bouncing dots view for smooth loading effect.
 * Mostly used in chat bubbles to indicate the other person is typing.
 *
 * GitHub: https://github.com/EyalBira/loading-dots
 * Created by eyalbiran on 12/5/15.
 */
public class LoadingDots extends LinearLayout {

    public static final int DEFAULT_DOTS_COUNT = 3;
    public static final int DEFAULT_LOOP_DURATION = 600;
    public static final int DEFAULT_LOOP_START_DELAY = 100;
    public static final int DEFAULT_JUMP_DURATION = 400;

    private List<View> mDots;
    private ValueAnimator mAnimation;
    private boolean mIsAttachedToWindow;

    private boolean mAutoPlay;

    // Dots appearance attributes
    private int mDotsColor;
    private int mDotsCount;
    private int mDotSize;
    private int mDotSpace;

    // Animation time attributes
    private int mLoopDuration;
    private int mLoopStartDelay;

    // Animation behavior attributes
    private int mJumpDuration;
    private int mJumpHeight;

    // Cached Calculations
    private int mJumpHalfTime;
    private int[] mDotsStartTime;
    private int[] mDotsJumpUpEndTime;
    private int[] mDotsJumpDownEndTime;

    int firstDot        = Color.parseColor("#ff3b59");
    int secondDot       = Color.parseColor("#ff850d");
    int thirdDot        = Color.parseColor("#ffc20d");
    int fourthDot       = Color.parseColor("#4dde92");
    int fifthDot        = Color.parseColor("#3dc4fb");
    public LoadingDots(Context context) {
        super(context);
        init(null);
    }

    public LoadingDots(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LoadingDots(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoadingDots(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Context context = getContext();
        Resources resources = context.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadingDots);

        mAutoPlay = a.getBoolean(R.styleable.LoadingDots_LoadingDots_auto_play, true);

        mDotsColor = a.getColor(R.styleable.LoadingDots_LoadingDots_dots_color, Color.GRAY);
        mDotsCount = a.getInt(R.styleable.LoadingDots_LoadingDots_dots_count, DEFAULT_DOTS_COUNT);
        mDotSize = a.getDimensionPixelSize(R.styleable.LoadingDots_LoadingDots_dots_size,
                resources.getDimensionPixelSize(R.dimen.LoadingDots_dots_size_default));
        mDotSpace = a.getDimensionPixelSize(R.styleable.LoadingDots_LoadingDots_dots_space,
                resources.getDimensionPixelSize(R.dimen.LoadingDots_dots_space_default));

        mLoopDuration = a.getInt(R.styleable.LoadingDots_LoadingDots_loop_duration, DEFAULT_LOOP_DURATION);
        mLoopStartDelay = a.getInt(R.styleable.LoadingDots_LoadingDots_loop_start_delay, DEFAULT_LOOP_START_DELAY);

        mJumpDuration = a.getInt(R.styleable.LoadingDots_LoadingDots_jump_duration, DEFAULT_JUMP_DURATION);
        mJumpHeight = a.getDimensionPixelSize(R.styleable.LoadingDots_LoadingDots_jump_height,
                resources.getDimensionPixelSize(R.dimen.LoadingDots_jump_height_default));

        a.recycle();

        // Setup LinerLayout
        setOrientation(HORIZONTAL);
        setGravity(Gravity.BOTTOM);

        calculateCachedValues();
        initializeDots(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // We allow the height to save space for the jump height
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + mJumpHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachedToWindow = true;

        createAnimationIfAutoPlay();
        if (mAnimation != null && getVisibility() == View.VISIBLE) {
            mAnimation.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
        if (mAnimation != null) {
            mAnimation.end();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        switch (visibility) {
            case VISIBLE:
                createAnimationIfAutoPlay();
                startAnimationIfAttached();
                break;
            case INVISIBLE:
            case GONE:
                if (mAnimation != null) {
                    mAnimation.end();
                }
                break;
        }
    }

    private View createDotView(Context context, int color) {
        ImageView dot = new ImageView(context);
        dot.setImageResource(R.drawable.loading_dots_dot);
        ((GradientDrawable) dot.getDrawable()).setColor(color);
        return dot;
    }

    private void startAnimationIfAttached() {
        if (mIsAttachedToWindow && !mAnimation.isRunning()) {
            mAnimation.start();
        }
    }

    private void createAnimationIfAutoPlay() {
        if (mAutoPlay) {
            createAnimation();
        }
    }

    private void createAnimation() {
        if (mAnimation != null) {
            // We already have an animation
            return;
        }
        calculateCachedValues();
        initializeDots(getContext());

        mAnimation = ValueAnimator.ofInt(0, mLoopDuration);
        mAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int dotsCount = mDots.size();
                int from = 0;

                int animationValue = (Integer) valueAnimator.getAnimatedValue();

                if (animationValue < mLoopStartDelay) {
                    // Do nothing
                    return;
                }

                for (int i = 0; i < dotsCount; i++) {
                    View dot = mDots.get(i);

                    int dotStartTime = mDotsStartTime[i];

                    float animationFactor;
                    if (animationValue < dotStartTime) {
                        // No animation is needed for this dot yet
                        animationFactor = 0f;
                    } else if (animationValue < mDotsJumpUpEndTime[i]) {
                        // Animate jump up
                        animationFactor = (float) (animationValue - dotStartTime) / mJumpHalfTime;
                    } else if (animationValue < mDotsJumpDownEndTime[i]) {
                        // Animate jump down
                        animationFactor = 1 - ((float) (animationValue - dotStartTime - mJumpHalfTime) / (mJumpHalfTime));
                    } else {
                        // Dot finished animation for this loop
                        animationFactor = 0f;
                    }

                    float translationY = (-mJumpHeight - from) * animationFactor;
                    dot.setTranslationY(translationY);
                }
            }
        });
        mAnimation.setDuration(mLoopDuration);
        mAnimation.setRepeatCount(Animation.INFINITE);
    }

    public final void startAnimation() {
        if (mAnimation != null && mAnimation.isRunning()) {
            // We are already running
            return;
        }

        createAnimation();
        startAnimationIfAttached();
    }

    public final void stopAnimation() {
        if (mAnimation != null) {
            mAnimation.end();
            mAnimation = null;
        }
    }

    private void calculateCachedValues() {
        verifyNotRunning();

        // The offset is the time delay between dots start animation
        int startOffset = (mLoopDuration - (mJumpDuration + mLoopStartDelay)) / (mDotsCount - 1);

        // Dot jump half time ( jumpTime/2 == going up == going down)
        mJumpHalfTime = mJumpDuration / 2;

        mDotsStartTime = new int[mDotsCount];
        mDotsJumpUpEndTime = new int[mDotsCount];
        mDotsJumpDownEndTime = new int[mDotsCount];

        for (int i = 0; i < mDotsCount; i++) {
            int startTime = mLoopStartDelay + startOffset * i;
            mDotsStartTime[i] = startTime;
            mDotsJumpUpEndTime[i] = startTime + mJumpHalfTime;
            mDotsJumpDownEndTime[i] = startTime + mJumpDuration;
        }
    }

    private void verifyNotRunning() {
        if (mAnimation != null) {
            throw new IllegalStateException("Can't change properties while animation is running!");
        }
    }

    private void initializeDots(Context context) {
        verifyNotRunning();
        removeAllViews();

        // Create the dots
        mDots = new ArrayList<>(mDotsCount);
        LayoutParams dotParams = new LayoutParams(mDotSize, mDotSize);
        LayoutParams spaceParams = new LayoutParams(mDotSpace, mDotSize);

        ArrayList<Integer> colores = new ArrayList<>(Arrays.asList(firstDot,secondDot,thirdDot,fourthDot,fifthDot) );

        for (int i = 0; i < mDotsCount; i++) {
            // Add dot

            View dotView = createDotView(context,colores.get(i));
            addView(dotView, dotParams);
            mDots.add(dotView);

            // Add space
            if (i < mDotsCount - 1) {
                addView(new View(context), spaceParams);
            }
        }
    }

    // Setters and getters

    /**
     * Set AutoPlay to true to play the loading animation automatically when view is attached and visible.
     * xml: LoadingDots_auto_play
     * @param autoPlay
     */
    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
    }

    public boolean getAutoPlay() {
        return mAutoPlay;
    }

    /**
     * Set the color to be used for the dots fill color
     * xml: LoadingDots_dots_color
     * @param color resolved color value
     */
    public void setDotsColor(int color) {
        verifyNotRunning();
        mDotsColor = color;
    }

    /**
     * Set the color to be used for the dots fill color
     * xml: LoadingDots_dots_color
     * @param colorRes color resource
     */
    public void setDotsColorRes(int colorRes) {
        setDotsColor(getContext().getResources().getColor(colorRes));
    }

    public int getDotsColor() {
        return mDotsColor;
    }

    /**
     * Set the number of dots
     * xml: LoadingDots_dots_count
     * @param count dots count
     */
    public void setDotsCount(int count) {
        verifyNotRunning();
        mDotsCount = count;
    }

    public int getDotsCount() {
        return mDotsCount;
    }

    /**
     * Set the dots size
     * xml: LoadingDots_dots_size
     * @param size size in pixels
     */
    public void setDotsSize(int size) {
        verifyNotRunning();
        mDotSize = size;
    }

    /**
     * Set the dots size
     * xml: LoadingDots_dots_size
     * @param sizeRes size resource
     */
    public void setDotsSizeRes(int sizeRes) {
        setDotsSize(getContext().getResources().getDimensionPixelSize(sizeRes));
    }

    public int getDotsSize() {
        return mDotSize;
    }

    /**
     * Set the space between dots
     * xml: LoadingDots_dots_space
     * @param space space in pixels
     */
    public void setDotsSpace(int space) {
        verifyNotRunning();
        mDotSpace = space;
    }

    /**
     * Set the space between dots
     * xml: LoadingDots_dots_space
     * @param spaceRes space size resource
     */
    public void setDotsSpaceRes(int spaceRes) {
        setDotsSpace(getContext().getResources().getDimensionPixelSize(spaceRes));
    }

    public int getDotsSpace() {
        return mDotSpace;
    }

    /**
     * Set the loop duration. This is the duration for the entire animation loop (including start delay)
     * xml: LoadingDots_loop_duration
     * @param duration duration in milliseconds
     */
    public void setLoopDuration(int duration) {
        verifyNotRunning();
        mLoopDuration = duration;
    }

    public int getLoopDuration() {
        return mLoopDuration;
    }

    /**
     * Set the loop start delay. Each loop will delay the animation by the given value.
     * xml: LoadingDots_loop_start_delay
     * @param startDelay delay duration in milliseconds
     */
    public void  setLoopStartDelay(int startDelay) {
        verifyNotRunning();
        mLoopStartDelay = startDelay;
    }

    public int getLoopStartDelay() {
        return mLoopStartDelay;
    }

    /**
     * Set the dots jump duration. This is the duration it takes a single dot to complete the jump.
     * Jump duration starts when the dot first start to rise until it settle back to base location.
     * xml: LoadingDots_jump_duration
     * @param jumpDuration
     */
    public void setJumpDuraiton(int jumpDuration) {
        verifyNotRunning();
        mJumpDuration = jumpDuration;
    }

    public int getJumpDuration() {
        return mJumpDuration;
    }

    /**
     * Set the jump height of the dots. The entire view will include this height to allow the dots
     * animation to draw properly. The entire view height will be DotsSize + JumpHeight.
     * xml: LoadingDots_jump_height
     * @param height size in pixels
     */
    public void setJumpHeight(int height) {
        verifyNotRunning();
        mJumpHeight = height;
    }

    /**
     * Set the jump height of the dots. The entire view will include this height to allow the dots
     * animation to draw properly. The entire view height will be DotsSize + JumpHeight.
     * xml: LoadingDots_jump_height
     * @param heightRes size resource
     */
    public void setJumpHeightRes(int heightRes) {
        setJumpHeight(getContext().getResources().getDimensionPixelSize(heightRes));
    }

    public int getJumpHeight() {
        return mJumpHeight;
    }
}

