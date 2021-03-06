package com.reactnativenavigation.screens;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.reactnativenavigation.NavigationApplication;
import com.reactnativenavigation.params.FabParams;
import com.reactnativenavigation.params.ScreenParams;
import com.reactnativenavigation.params.StyleParams;
import com.reactnativenavigation.params.TitleBarButtonParams;
import com.reactnativenavigation.params.TitleBarLeftButtonParams;
import com.reactnativenavigation.utils.KeyboardVisibilityDetector;
import com.reactnativenavigation.utils.Task;
import com.reactnativenavigation.views.LeftButtonOnClickListener;
import com.reactnativenavigation.views.MenuButtonOnClickListener;

import java.util.List;
import java.util.Stack;

public class ScreenStack {
    private static final String TAG = "ScreenStack";

    public interface OnScreenPop {
        void onScreenPopAnimationEnd();
    }

    private final AppCompatActivity activity;
    private RelativeLayout parent;
    private LeftButtonOnClickListener leftButtonOnClickListener;
    private MenuButtonOnClickListener rightButtonsClickListener;
    private Stack<Screen> stack = new Stack<>();
	private boolean disableBackNavigation = false;
    private final KeyboardVisibilityDetector keyboardVisibilityDetector;
    private boolean isStackVisible = false;
    private final String navigatorId;

    public String getNavigatorId() {
        return navigatorId;
    }

    public ScreenStack(AppCompatActivity activity,
                       RelativeLayout parent,
                       String navigatorId,
                       LeftButtonOnClickListener leftButtonOnClickListener,
					   MenuButtonOnClickListener rightButtonsClickListener) {
        this.activity = activity;
        this.parent = parent;
        this.navigatorId = navigatorId;
        this.leftButtonOnClickListener = leftButtonOnClickListener;
		this.rightButtonsClickListener = rightButtonsClickListener;
        keyboardVisibilityDetector = new KeyboardVisibilityDetector(parent);
    }

    public void newStack(final ScreenParams params, LayoutParams layoutParams) {
        final Screen nextScreen = ScreenFactory.create(activity, params, leftButtonOnClickListener, rightButtonsClickListener);
        final Screen previousScreen = stack.peek();
        if (isStackVisible) {
            pushScreenToVisibleStack(layoutParams, nextScreen, previousScreen, new Screen.OnDisplayListener() {
                @Override
                public void onDisplay() {
                    removeElementsBelowTop();
                }
            });
        } else {
            pushScreenToInvisibleStack(layoutParams, nextScreen, previousScreen);
            removeElementsBelowTop();
        }
    }

    private void removeElementsBelowTop() {
        while (stack.size() > 1) {
            Screen screen = stack.get(0);
            parent.removeView(screen);
            screen.destroy();
            stack.remove(0);
        }
    }

    public void pushInitialScreenWithAnimation(final ScreenParams initialScreenParams, LayoutParams params) {
        isStackVisible = true;
        pushInitialScreen(initialScreenParams, params);
        final Screen screen = stack.peek();
        screen.setOnDisplayListener(new Screen.OnDisplayListener() {
            @Override
            public void onDisplay() {
                screen.show(initialScreenParams.animateScreenTransitions);
                screen.setStyle();
            }
        });
    }

    public void pushInitialScreen(ScreenParams initialScreenParams, LayoutParams params) {
        Screen initialScreen = ScreenFactory.create(activity, initialScreenParams, leftButtonOnClickListener, rightButtonsClickListener);
        initialScreen.setVisibility(View.INVISIBLE);
        initialScreen.setOnDisplayListener(new Screen.OnDisplayListener() {
            @Override
            public void onDisplay() {
                if (isStackVisible) {
                    NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("willAppear", stack.peek().getNavigatorEventId());
                    NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("didAppear", stack.peek().getNavigatorEventId());
                }
            }
        });
        addScreen(initialScreen, params);
    }

    public void push(final ScreenParams params, LayoutParams layoutParams) {
        Screen nextScreen = ScreenFactory.create(activity, params, leftButtonOnClickListener, rightButtonsClickListener);
        final Screen previousScreen = stack.peek();
        if (isStackVisible) {
            if (nextScreen.screenParams.sharedElementsTransitions.isEmpty()) {
                pushScreenToVisibleStack(layoutParams, nextScreen, previousScreen);
            } else {
                pushScreenToVisibleStackWithSharedElementTransition(layoutParams, nextScreen, previousScreen);
            }
        } else {
            pushScreenToInvisibleStack(layoutParams, nextScreen, previousScreen);
        }
    }

    private void pushScreenToVisibleStack(LayoutParams layoutParams, final Screen nextScreen,
                                          final Screen previousScreen) {
        pushScreenToVisibleStack(layoutParams, nextScreen, previousScreen, null);
    }

    private void pushScreenToVisibleStack(LayoutParams layoutParams,
                                          final Screen nextScreen,
                                          final Screen previousScreen,
                                          @Nullable final Screen.OnDisplayListener onDisplay) {
        nextScreen.setVisibility(View.INVISIBLE);
        addScreen(nextScreen, layoutParams);
        NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("willDisappear", previousScreen.getNavigatorEventId());
        nextScreen.setOnDisplayListener(new Screen.OnDisplayListener() {
            @Override
            public void onDisplay() {
                nextScreen.show(nextScreen.screenParams.animateScreenTransitions, new Runnable() {
                    @Override
                    public void run() {
                        if (onDisplay != null) onDisplay.onDisplay();
                        NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("didDisappear", previousScreen.getNavigatorEventId());
                        parent.removeView(previousScreen);
                    }
                });
            }
        });
    }

    private void pushScreenToVisibleStackWithSharedElementTransition(LayoutParams layoutParams, final Screen nextScreen, final Screen previousScreen) {
        nextScreen.setVisibility(View.INVISIBLE);
        nextScreen.setOnDisplayListener(new Screen.OnDisplayListener() {
            @Override
            public void onDisplay() {
                nextScreen.showWithSharedElementsTransitions(previousScreen.sharedElements.getToElements(), new Runnable() {
                    @Override
                    public void run() {
                        parent.removeView(previousScreen);
                    }
                });
            }
        });
        addScreen(nextScreen, layoutParams);
    }

    private void pushScreenToInvisibleStack(LayoutParams layoutParams, Screen nextScreen, Screen previousScreen) {
        nextScreen.setVisibility(View.INVISIBLE);
        addScreen(nextScreen, layoutParams);
        parent.removeView(previousScreen);
    }

    private void addScreen(Screen screen, LayoutParams layoutParams) {
        parent.addView(screen, layoutParams);
        stack.push(screen);
    }

    public void pop(boolean animated) {
        pop(animated, null);
    }

    public void pop(final boolean animated, @Nullable final OnScreenPop onScreenPop) {
        if (!canPop()) {
            return;
        }
        if (keyboardVisibilityDetector.isKeyboardVisible()) {
            keyboardVisibilityDetector.setKeyboardCloseListener(new Runnable() {
                @Override
                public void run() {
                    keyboardVisibilityDetector.setKeyboardCloseListener(null);
                    popInternal(animated, onScreenPop);
                }
            });
            keyboardVisibilityDetector.closeKeyboard();
        } else {
            popInternal(animated, onScreenPop);
        }
    }

    private void popInternal(final boolean animated, @Nullable final OnScreenPop onScreenPop) {
        final Screen toRemove = stack.pop();
        final Screen previous = stack.peek();
        swapScreens(animated, toRemove, previous, onScreenPop);
    }

    private void swapScreens(boolean animated, final Screen toRemove, Screen previous, OnScreenPop onScreenPop) {
        readdPrevious(previous);
        previous.setStyle();
        hideScreen(animated, toRemove, previous);
        if (onScreenPop != null) {
            onScreenPop.onScreenPopAnimationEnd();
        }
    }

    private void hideScreen(boolean animated, final Screen toRemove, Screen previous) {
        Runnable onAnimationEnd = new Runnable() {
            @Override
            public void run() {
                toRemove.destroy();
                parent.removeView(toRemove);
            }
        };
        if (animated) {
            toRemove.animateHide(previous.sharedElements.getToElements(), onAnimationEnd);
        } else {
            toRemove.hide(previous.sharedElements.getToElements(), onAnimationEnd);
        }
    }

    public Screen peek() {
        return stack.peek();
    }

    public Screen bottom() {
        return stack.get(0);
    }

    public boolean empty() {
		return stack.empty();
	}

    private void readdPrevious(Screen previous) {
        previous.setVisibility(View.VISIBLE);
        NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("willAppear", previous.getNavigatorEventId());
        NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("didAppear", previous.getNavigatorEventId());
        parent.addView(previous, 0);
    }

    public void popToRoot(final boolean animated, @Nullable final OnScreenPop onScreenPop) {
        if (keyboardVisibilityDetector.isKeyboardVisible()) {
            keyboardVisibilityDetector.setKeyboardCloseListener(new Runnable() {
                @Override
                public void run() {
                    keyboardVisibilityDetector.setKeyboardCloseListener(null);
                    popToRootInternal(animated, onScreenPop);
                }
            });
            keyboardVisibilityDetector.closeKeyboard();
        } else {
            popToRootInternal(animated, onScreenPop);
        }
    }

    private void popToRootInternal(final boolean animated, @Nullable final OnScreenPop onScreenPop) {
        while (canPop()) {
            if (stack.size() == 2) {
                popInternal(animated, onScreenPop);
            } else {
                popInternal(animated, null);
            }
        }
    }

    public void destroy() {
        for (Screen screen : stack) {
            screen.destroy();
            parent.removeView(screen);
        }
        stack.clear();
    }

    public boolean canPop() {
        return stack.size() > 1 && !isPreviousScreenAttachedToWindow() && !disableBackNavigation;
    }

    private boolean isPreviousScreenAttachedToWindow() {
        Screen previousScreen = stack.get(stack.size() - 2);
        if (previousScreen.getParent() != null) {
            Log.w(TAG, "Can't pop stack. reason: previous screen is already attached");
            return true;
        }
        return false;
    }

    public void setScreenTopBarVisible(String screenInstanceId, final boolean visible, final boolean animate) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen param) {
                param.setTopBarVisible(visible, animate);
            }
        });
    }

    public void setScreenTitleBarTitle(String screenInstanceId, final String title) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen param) {
                param.setTitleBarTitle(title);
            }
        });
    }

    public void setScreenTitleBarSubtitle(String screenInstanceId, final String subtitle) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen param) {
                param.setTitleBarSubtitle(subtitle);
            }
        });
    }

    public void setScreenTitleBarRightButtons(String screenInstanceId, final String navigatorEventId, final List<TitleBarButtonParams> titleBarButtons) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen param) {
                param.setTitleBarRightButtons(navigatorEventId, titleBarButtons);
            }
        });
    }

    public void setScreenTitleBarLeftButton(String screenInstanceId, final String navigatorEventId, final TitleBarLeftButtonParams titleBarLeftButtonParams) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen screen) {
                screen.setTitleBarLeftButton(navigatorEventId, leftButtonOnClickListener, titleBarLeftButtonParams);
            }
        });
    }

    public void setFab(String screenInstanceId, final FabParams fabParams) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen screen) {
                screen.setFab(fabParams);
            }
        });
    }

	public void setDisableBackNavigation(boolean disableBackNavigation)
	{
		this.disableBackNavigation = disableBackNavigation;

		StyleParams params = stack.peek().getStyleParams();
		params.backButtonHidden = disableBackNavigation;
		stack.peek().getTopBar().setStyle(params);
	}

	public boolean getDisableBackNavigation()
	{
		return this.disableBackNavigation;
	}

	public void updateScreenStyle(String screenInstanceId, final Bundle styleParams) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen screen) {
                if (isScreenVisible(screen)) {
                    screen.updateVisibleScreenStyle(styleParams);
                } else {
                    screen.updateInvisibleScreenStyle(styleParams);
                }
            }
        });
    }

    private boolean isScreenVisible(Screen screen) {
        return isStackVisible && peek() == screen;
    }

    public void selectTopTabByTabIndex(String screenInstanceId, final int index) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen screen) {
                if (screen.screenParams.hasTopTabs()) {
                    ((ViewPagerScreen) screen).selectTopTabByTabIndex(index);
                }
            }
        });
    }

    public void selectTopTabByScreen(final String screenInstanceId) {
        performOnScreen(screenInstanceId, new Task<Screen>() {
            @Override
            public void run(Screen screen) {
                ((ViewPagerScreen) screen).selectTopTabByTabByScreen(screenInstanceId);
            }
        });
    }

    public StyleParams getCurrentScreenStyleParams() {
        return stack.peek().getStyleParams();
    }

    public boolean handleBackPressInJs() {
        ScreenParams currentScreen = stack.peek().screenParams;
        if (currentScreen.overrideBackPressInJs) {
            NavigationApplication.instance.getEventEmitter().sendNavigatorEvent("backPress", currentScreen.getNavigatorEventId());
            return true;
        }
        return false;
    }

    private void performOnScreen(String screenInstanceId, Task<Screen> task) {
        if (stack.isEmpty()) {
            return;
        }
        for (Screen screen : stack) {
            if (screen.hasScreenInstance(screenInstanceId)) {
                task.run(screen);
                return;
            }
        }
    }

    public void show() {
        isStackVisible = true;
        if (!stack.empty()) {
            stack.peek().setStyle();
            stack.peek().setVisibility(View.VISIBLE);
            NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("willAppear", stack.peek().getNavigatorEventId());
            NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("didAppear", stack.peek().getNavigatorEventId());
        }
    }

    public void hide() {
        if (!stack.empty()) {
            NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("willDisappear", stack.peek().getNavigatorEventId());
            NavigationApplication.instance.getEventEmitter().sendScreenChangedEvent("didDisappear", stack.peek().getNavigatorEventId());
            isStackVisible = false;
            stack.peek().setVisibility(View.INVISIBLE);
        }
    }

    public String rootScreenId() {
		return stack.get(0).screenParams.screenId;
	}
}
