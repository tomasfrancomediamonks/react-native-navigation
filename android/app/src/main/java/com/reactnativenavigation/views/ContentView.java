package com.reactnativenavigation.views;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.reactnativenavigation.NavigationApplication;
import com.reactnativenavigation.params.NavigationParams;
import com.reactnativenavigation.screens.SingleScreen;
import com.reactnativenavigation.utils.ViewUtils;
import com.reactnativenavigation.views.utils.ViewMeasurer;

public class ContentView extends ReactRootView {
    private final String screenId;
    private final NavigationParams navigationParams;
    private final Bundle passProps;

    boolean isContentVisible = false;
    private SingleScreen.OnDisplayListener onDisplayListener;
    protected ViewMeasurer viewMeasurer;

    public void setOnDisplayListener(SingleScreen.OnDisplayListener onDisplayListener) {
        this.onDisplayListener = onDisplayListener;
    }

    public ContentView(Context context, String screenId, NavigationParams navigationParams, Bundle passProps) {
        super(context);
        this.screenId = screenId;
        this.navigationParams = navigationParams;
        this.passProps = passProps;
        attachToJS();
        viewMeasurer = new ViewMeasurer();
    }

    public void setViewMeasurer(ViewMeasurer viewMeasurer) {
        this.viewMeasurer = viewMeasurer;
    }

    private void attachToJS() {
        Bundle bundle = new Bundle();
        if (this.passProps != null) {
            bundle.putAll(this.passProps);
        }
        if (this.navigationParams != null) {
            bundle.putAll(this.navigationParams.toBundle());
        }

        startReactApplication(NavigationApplication.instance.getReactGateway().getReactInstanceManager(), screenId, bundle);
    }

    public String getNavigatorEventId() {
        return navigationParams.navigatorEventId;
    }

    public void unmountReactView() {
        unmountReactApplication();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST);

        super.onMeasure(widthSpec, heightSpec);

        int measuredHeight = viewMeasurer.getMeasuredHeight(heightSpec);
        setMeasuredDimension(viewMeasurer.getMeasuredWidth(widthSpec), measuredHeight);
    }

    @Override
    public void onViewAdded(final View child) {
        super.onViewAdded(child);
        detectContentViewVisible(child);
    }

    private void detectContentViewVisible(View child) {
        if (onDisplayListener != null) {
            ViewUtils.runOnPreDraw(child, new Runnable() {
                @Override
                public void run() {
                    if (!isContentVisible) {
                        isContentVisible = true;
                        onDisplayListener.onDisplay();
                        onDisplayListener = null;
                    }
                }
            });
        }
    }
}
