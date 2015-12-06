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

    private View createDotView(Context context) {
        ImageView dot = new ImageView(context);
        dot.setImageResource(R.drawable.loading_dots_dot);
        ((GradientDrawable) dot.getDrawable()).setColor(mDotsColor);
        return dot;
    }

    private void startAnimationIfAttached() {
        if (mIsAttachedToWindow) {
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
            // We already running
            return;
        }

        createAnimation();
        startAnimationIfAttached();
    }

    public final void stopAnimation() {
        if (mAnimation != null) {
            mAnimation.cancel();
            mAnimation = null;
        }
    }

    private void calculateCachedValues() {
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

    private void initializeDots(Context context) {
        // Create the dots
        mDots = new ArrayList<>(mDotsCount);
        LayoutParams dotParams = new LayoutParams(mDotSize, mDotSize);
        LayoutParams spaceParams = new LayoutParams(mDotSpace, mDotSize);
        for (int i = 0; i < mDotsCount; i++) {
            // Add dot
            View dotView = createDotView(context);
            addView(dotView, dotParams);
            mDots.add(dotView);

            // Add space
            if (i < mDotsCount - 1) {
                addView(new View(context), spaceParams);
            }
        }
    }
}

