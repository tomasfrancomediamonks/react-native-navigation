package com.reactnativenavigation.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.reactnativenavigation.animation.VisibilityAnimator;
import com.reactnativenavigation.params.BaseScreenParams;
import com.reactnativenavigation.params.StyleParams;
import com.reactnativenavigation.params.TitleBarButtonParams;
import com.reactnativenavigation.params.TitleBarLeftButtonParams;
import com.reactnativenavigation.utils.ViewUtils;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class TopBar extends AppBarLayout {
    protected TitleBar titleBar;
    protected FrameLayout titleBarAndContextualMenuContainer;
    protected TopTabs topTabs;
    private VisibilityAnimator visibilityAnimator;

    public TopBar(Context context) {
        super(context);
        setId(ViewUtils.generateViewId());
        createTopBarVisibilityAnimator();
        createLayout();
    }

    private void createTopBarVisibilityAnimator() {
        ViewUtils.runOnPreDraw(this, new Runnable() {
            @Override
            public void run() {
                visibilityAnimator = new VisibilityAnimator(TopBar.this,
                        VisibilityAnimator.HideDirection.Up,
                        getHeight());
            }
        });
    }

    protected void createLayout() {
        titleBarAndContextualMenuContainer = new FrameLayout(getContext());
        addView(titleBarAndContextualMenuContainer);
    }

    public void addTitleBarAndSetButtons(List<TitleBarButtonParams> rightButtons,
										 MenuButtonOnClickListener rightButtonsClickListener,
										 TitleBarLeftButtonParams leftButton,
										 LeftButtonOnClickListener leftButtonOnClickListener,
										 String navigatorEventId, boolean overrideBackPressInJs) {
        titleBar = createTitleBar();
        addTitleBar();
        addButtons(rightButtons, rightButtonsClickListener, leftButton, leftButtonOnClickListener, navigatorEventId, overrideBackPressInJs);
    }

    protected TitleBar createTitleBar() {
        return new TitleBar(getContext());
    }

    protected void addTitleBar() {
        titleBarAndContextualMenuContainer.addView(titleBar, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    private void addButtons(List<TitleBarButtonParams> rightButtons, MenuButtonOnClickListener rightButtonsClickListener, TitleBarLeftButtonParams leftButton, LeftButtonOnClickListener leftButtonOnClickListener, String navigatorEventId, boolean overrideBackPressInJs) {
        titleBar.setRightButtons(rightButtons, rightButtonsClickListener, navigatorEventId);
        titleBar.setLeftButton(leftButton, leftButtonOnClickListener, navigatorEventId, overrideBackPressInJs);
    }

    public void setTitle(String title) {
        titleBar.setTitle(title);
    }

    public void setSubtitle(String subtitle) {
        titleBar.setSubtitle(subtitle);
    }

    public void setButtonColor(StyleParams styleParams) {
        titleBar.setButtonColor(styleParams.titleBarButtonColor);
    }

    public void setStyle(StyleParams styleParams) {
        if (styleParams.topBarColor.hasColor()) {
            setBackgroundColor(styleParams.topBarColor.getColor());
        }
        if (styleParams.topBarTransparent) {
            setTransparent();
        }
        if (styleParams.titleBarHeight > -1) {
            int navBarHeight = (int) TypedValue.applyDimension(2, styleParams.titleBarHeight, getResources().getDisplayMetrics());
            setMinimumHeight(navBarHeight);
            setGravity(Gravity.BOTTOM);
        }
        titleBar.setStyle(styleParams);
        setTopTabsStyle(styleParams);
        if (!styleParams.topBarElevationShadowEnabled) {
            disableElevationShadow();
        }
    }

    private void setTransparent() {
        setBackgroundColor(Color.TRANSPARENT);
        disableElevationShadow();
    }

    private void disableElevationShadow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(null);
        }
    }

    public void setTitleBarRightButtons(String navigatorEventId, List<TitleBarButtonParams> titleBarButtons, MenuButtonOnClickListener rightButtonsClickListener) {
        titleBar.setRightButtons(titleBarButtons, rightButtonsClickListener, navigatorEventId);
    }

    public TopTabs initTabs() {
        topTabs = new TopTabs(getContext());
        addView(topTabs, new ViewGroup.LayoutParams(MATCH_PARENT, (int) ViewUtils.convertDpToPixel(48)));
        return topTabs;
    }

    public void setTitleBarLeftButton(String navigatorEventId,
                                      LeftButtonOnClickListener leftButtonOnClickListener,
                                      TitleBarLeftButtonParams titleBarLeftButtonParams,
                                      boolean overrideBackPressInJs) {
        titleBar.setLeftButton(titleBarLeftButtonParams, leftButtonOnClickListener, navigatorEventId,
                overrideBackPressInJs);
    }

    private void setTopTabsStyle(StyleParams style) {
        if (topTabs == null) {
            return;
        }
        topTabs.setTopTabsTextColor(style);
        topTabs.setSelectedTabIndicatorStyle(style);
        topTabs.setScrollable(style);
    }

    public void destroy() {

    }

    public void onViewPagerScreenChanged(BaseScreenParams screenParams) {
        titleBar.onViewPagerScreenChanged(screenParams);
    }

    public void setVisible(boolean visible, boolean animate) {
        titleBar.setVisibility(!visible);
        visibilityAnimator.setVisible(visible, animate);
    }
}
