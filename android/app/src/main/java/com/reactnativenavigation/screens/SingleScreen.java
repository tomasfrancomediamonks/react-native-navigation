package com.reactnativenavigation.screens;

import android.support.v7.app.AppCompatActivity;

import com.reactnativenavigation.params.ScreenParams;
import com.reactnativenavigation.views.ContentView;
import com.reactnativenavigation.views.LeftButtonOnClickListener;
import com.reactnativenavigation.views.MenuButtonOnClickListener;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class SingleScreen extends Screen {
    protected ContentView contentView;

    public SingleScreen(AppCompatActivity activity, ScreenParams screenParams,
                        LeftButtonOnClickListener titleBarBarBackButtonListener,
						MenuButtonOnClickListener rightButtonsClickListener) {
        super(activity, screenParams, titleBarBarBackButtonListener, rightButtonsClickListener);
    }

    @Override
    protected void createContent() {
        contentView = new ContentView(getContext(), screenParams.screenId, screenParams.navigationParams);
        addView(contentView, 0, createLayoutParams());
    }

    @Override
    public ContentView getContentView() {
        return contentView;
    }

    protected LayoutParams createLayoutParams() {
        LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        if (screenParams.styleParams.drawScreenBelowTopBar) {
            params.addRule(BELOW, topBar.getId());
        }
        return params;
    }

    @Override
    public void unmountReactView() {
        contentView.unmountReactView();
    }

    @Override
    public String getNavigatorEventId() {
        return screenParams.getNavigatorEventId();
    }

    @Override
    public void setOnDisplayListener(OnDisplayListener onContentViewDisplayedListener) {
        contentView.setOnDisplayListener(onContentViewDisplayedListener);
    }
}
