package cn.expandablelayout;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ExpandIconView extends View {
    private static final float MORE_STATE_ALPHA = -45f;
    private static final float LESS_STATE_ALPHA = 45f;
    private static final float DELTA_ALPHA = 90f;

    private static final float THICKNESS_PROPORTION = 5f / 36f;

    private static final float PADDING_PROPORTION = 4f / 24f;

    private static final long DEFAULT_ANIMATION_DURATION = 150;

    @IntDef({
            MORE,
            LESS,
            INTERMEDIATE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public static final int MORE = 0;
    public static final int LESS = 1;
    public static final int INTERMEDIATE = 2;

    @State
    private int state;
    private float alpha = MORE_STATE_ALPHA;
    private float centerTranslation = 0f;
    @FloatRange(from = 0.f, to = 1.f)
    private float fraction = 0f; // 0或1
    private float animationSpeed;

    private boolean switchColor = false;
    private int color = Color.BLACK;
    private final int colorMore;
    private final int colorLess;
    private final int colorIntermediate;

    @NonNull
    private final Paint paint;
    // 左右改成上下
    /*
    private final Point left = new Point();
    private final Point right = new Point();
    private final Point center = new Point();
    private final Point tempLeft = new Point();
    private final Point tempRight = new Point();
    */
    private final Point top = new Point();
    private final Point bottom = new Point();
    private final Point center = new Point();
    private final Point tempTop = new Point();
    private final Point tempBottom = new Point();

    private final boolean useDefaultPadding;
    private int padding;

    private final Path path = new Path();
    @Nullable
    private ValueAnimator arrowAnimator;

    public ExpandIconView(@NonNull Context context) {
        this(context, null);
    }
    public ExpandIconView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ExpandIconView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attrArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ExpandIconView, 0, 0);

        final boolean roundedCorners;
        final long animationDuration;
        try {
            roundedCorners = attrArray.getBoolean(R.styleable.ExpandIconView_eiv_roundedCorners, false);
            switchColor = attrArray.getBoolean(R.styleable.ExpandIconView_eiv_switchColor, false);
            color = attrArray.getColor(R.styleable.ExpandIconView_eiv_color, Color.BLACK);
            colorMore = attrArray.getColor(R.styleable.ExpandIconView_eiv_colorMore, Color.BLACK);
            colorLess = attrArray.getColor(R.styleable.ExpandIconView_eiv_colorLess, Color.BLACK);
            colorIntermediate = attrArray.getColor(R.styleable.ExpandIconView_eiv_colorIntermediate, -1);
            animationDuration = attrArray.getInteger(R.styleable.ExpandIconView_eiv_animationDuration, (int) DEFAULT_ANIMATION_DURATION);
            padding = attrArray.getDimensionPixelSize(R.styleable.ExpandIconView_eiv_padding, -1);
            useDefaultPadding = (padding == -1);
        } finally {
            attrArray.recycle();
        }

        {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setDither(true);
            if (roundedCorners) {
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
            }
        }

        animationSpeed = DELTA_ALPHA / animationDuration;
        setState(MORE, false);
    }

    /**
     * 外部调用
     */
    public void switchState() {
        switchState(true);
    }

    /**
     * Changes state and update View
     * 更改state
     * 调用setState
     * @param animate Indicate thaw state will be changed with animation or not
     */
    public void switchState(boolean animate) {
        final int newState;
        switch (state) {
            case MORE:
                newState = LESS;
                break;
            case LESS:
                newState = MORE;
                break;
            case INTERMEDIATE:
                newState = getFinalStateByFraction(); // 小于0.5是MORE
                break;
            default:
                throw new IllegalArgumentException("Unknown state [" + state + "]");
        }
        setState(newState, animate);
    }

    /**
     * Set one if two states and update view
     * 设置fraction
     * 调用updateArrow
     * @param state {@link #MORE} or {@link #LESS}
     * @param animate animate Indicates thaw state will be changed with animation or not
     * @throws IllegalArgumentException if {@param state} is invalid
     */
    public void setState(@State int state, boolean animate) {
        this.state = state;
        if (state == MORE) {
            fraction = 0f;
        } else if (state == LESS) {
            fraction = 1f;
        } else {
            throw new IllegalArgumentException("Unknown state, must be one of STATE_MORE = 0, STATE_LESS = 1");
        }
        updateArrow(animate);
    }

    public void setFraction(@FloatRange(from = 0.f, to = 1.f) float fraction, boolean animate) {
        if (fraction < 0f || fraction > 1f) {
            throw new IllegalArgumentException("Fraction value must be from 0 to 1f, fraction = " + fraction);
        }

        if (this.fraction == fraction) {
            return;
        }

        this.fraction = fraction;
        if (fraction == 0f) {
            state = MORE;
        } else if (fraction == 1f) {
            state = LESS;
        } else {
            state = INTERMEDIATE;
        }
        updateArrow(animate);
    }

    /**
     * Set duration of icon animation from state {@link #MORE} to state {@link #LESS}
     * @param animationDuration
     */
    public void setAnimationSpeed(long animationDuration) {
        animationSpeed = DELTA_ALPHA / animationDuration;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(centerTranslation, 0);
        canvas.drawPath(path, paint);
    }

    /**
     * 控件大小发生变化后，要重新绘制Arrow。
     * @param width
     * @param height
     * @param oldWidth
     * @param oldHeight
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        calculateArrowMetrics(width, height);
        updateArrowPath();
    }

    private void calculateArrowMetrics(int width, int height) {
        final int arrowMaxWidth = (height >= width ? width : height);
        if (useDefaultPadding) {
            padding = (int) (PADDING_PROPORTION * arrowMaxWidth);
        }
        final int arrowWidth = arrowMaxWidth - 2 * padding;
        float thickness = (int) (arrowWidth * THICKNESS_PROPORTION);
        paint.setStrokeWidth(thickness);

        center.set(width/2, height/2);
        top.set(center.x, center.y - arrowWidth/2);
        bottom.set(center.x, center.y + arrowWidth/2);
    }

    /**
     * 设置toAlpha
     *
     * @param animate
     */
    public void updateArrow(boolean animate) {
        float toAlpha = MORE_STATE_ALPHA + (fraction * DELTA_ALPHA);
        if (animate) {
            animateArrow(toAlpha);
        } else {
            cancelAnimation();
            alpha = toAlpha;
            if (switchColor) {
                updateColor(new ArgbEvaluator());
            }
            updateArrowPath();
            invalidate();
        }
    }

    private void updateArrowPath() {
        path.reset();
        if (top != null && bottom != null) {
            rotate(top, alpha, tempTop);
            rotate(bottom, -alpha, tempBottom);
            centerTranslation = (center.x - tempTop.x)/2;
            path.moveTo(tempTop.x, tempTop.y);
            path.lineTo(center.x, center.y);
            path.lineTo(tempBottom.x, tempBottom.y);
        }
    }

    /**
     * 1. 改变alpha = (float) valueAnimator.getAnimatedValue();
     * 2. updateArrowPath
     * 3. updateColor
     * @param toAlpha
     */
    private void animateArrow(float toAlpha) {
        cancelAnimation();
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(alpha, toAlpha);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                alpha = (float) valueAnimator.getAnimatedValue();
                updateArrowPath();
                if (switchColor) {
                    updateColor(colorEvaluator);
                }
                postInvalidateOnAnimation(); // 在动画中刷新
            }
        });
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.setDuration(calculateAnimationDuration(toAlpha))
                     .start();
        arrowAnimator = valueAnimator;
    }

    private void cancelAnimation() {
        if (arrowAnimator != null && arrowAnimator.isRunning()) {
            arrowAnimator.cancel();
        }
    }

    /**
     * 取中间色
     * @param colorEvaluator
     */
    private void updateColor(@NonNull ArgbEvaluator colorEvaluator) {
        float fraction;
        int colorFrom;
        int colorTo;
        if (colorIntermediate != -1) {
            colorFrom = alpha <= 0f ? colorMore : colorIntermediate;
            colorTo = alpha <= 0f ? colorIntermediate : colorLess;
            fraction = alpha <= 0f ? (1 + alpha/45f) : alpha/45f;
        } else {
            colorFrom = colorMore;
            colorTo = colorLess;
            fraction = (alpha + 45f) / 90f;
        }
        color = (int) colorEvaluator.evaluate(fraction, colorFrom, colorTo);
        paint.setColor(color);
    }

    private long calculateAnimationDuration(float toAlpha) {
        return (long) (Math.abs(toAlpha - alpha) / animationSpeed);
    }

    /**
     * rotate(left, -alpha, tempLeft);
     * rotate(right, alpha, tempRight);
     * @param startPosition
     * @param degrees
     * @param target
     */
    private void rotate(@NonNull Point startPosition, double degrees, @NonNull Point target) {
        double angle = Math.toRadians(degrees);
        int x = (int) (center.x - (startPosition.y - center.y) * Math.sin(angle));
        int y = (int) (center.y + (startPosition.y - center.y) * Math.cos(angle));
        target.set(x, y);
    }

    @State
    private int getFinalStateByFraction() {
        if (fraction < .5f) {
            return MORE;
        } else {
            return LESS;
        }
    }
}
