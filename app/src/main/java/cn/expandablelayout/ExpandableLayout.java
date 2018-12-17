package cn.expandablelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ExpandableLayout extends RelativeLayout {
    private Boolean isAnimationRunning = false; // 动画是否在执行
    private Boolean isOpened = false; // 是否完全展开
    private Integer duration; // 动画时长
    private FrameLayout contentLayout; // 展开和缩放的内容
    private FrameLayout headerLayout; // 控制按钮
    private Animation animation; // View.startAnimation(animation);

    public  ExpandableLayout(Context context) {
        this(context, null);
    }
    public ExpandableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public ExpandableLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(final Context context, AttributeSet attrs) {
        final View rootView = View.inflate(context, R.layout.view_expandable, this);
        headerLayout = rootView.findViewById(R.id.view_expandable_headerlayout);
        contentLayout = rootView.findViewById(R.id.view_expandable_contentLayout);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);
        final int headerID = typedArray.getResourceId(R.styleable.ExpandableLayout_el_headerLayout, -1);
        final int contentID = typedArray.getResourceId(R.styleable.ExpandableLayout_el_contentLayout, -1);
        if (headerID == -1 || contentID == -1) {
            throw new IllegalArgumentException("HeaderLayout and ContentLayout cannot be null");
        }
        if (isInEditMode())
            return;
        duration = typedArray.getInt(R.styleable.ExpandableLayout_el_duration, getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));

        final View contentView = View.inflate(context, contentID, null);
        contentView.measure(0,0);
        Log.d("MainActivity", contentView.getMeasuredWidth()+ " " + contentView.getMeasuredHeight()); // LOG, contentView的宽高。
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        params.gravity = Gravity.END;
        contentView.setLayoutParams(params);
        //contentView.setLayoutParams(new ViewGroup.LayoutParams(contentView.getMeasuredWidth(), contentView.getMeasuredHeight()));
        contentLayout.addView(contentView);
        contentLayout.setVisibility(GONE);

        final View headerView = View.inflate(context, headerID, null);

        headerView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        final ExpandIconView headerIcon = headerView.findViewById(R.id.header_text);
        headerLayout.addView(headerView);
        headerLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAnimationRunning) {
                    headerIcon.switchState();
                    if (contentLayout.getVisibility() == VISIBLE) {
                        collapse(contentLayout);
                    } else {
                        expand(contentLayout);
                    }
                    isAnimationRunning = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isAnimationRunning = false;
                        }
                    }, duration);
                }
            }
        });

        typedArray.recycle();
    }

    private void expand(final View v) { // v是FrameLayout
        v.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Log.d(v.getClass().getName(), v.getMeasuredWidth()+" "+v.getMeasuredHeight());
        Log.d("expand", v.getLayoutParams().width+ " "+v.getLayoutParams().height+"");
        final int targetWidth = v.getMeasuredWidth();
        v.getLayoutParams().width = 0;
        v.getLayoutParams().height = v.getMeasuredHeight();
        v.setVisibility(VISIBLE);
        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    isOpened = true;
                }
                v.getLayoutParams().width = (interpolatedTime == 1) ? LayoutParams.WRAP_CONTENT : (int) (targetWidth * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(300);
        v.startAnimation(animation);
    }

    private void collapse(final View v) {
        //final int initialHeight = v.getMeasuredHeight();
        final int initialWidth = v.getMeasuredWidth();
        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                    isOpened = false;
                } else {
                    v.getLayoutParams().width = initialWidth - (int) (initialWidth * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(duration);
        v.startAnimation(animation);
    }
}
