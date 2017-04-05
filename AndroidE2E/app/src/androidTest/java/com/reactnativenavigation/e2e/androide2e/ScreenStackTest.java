package com.reactnativenavigation.e2e.androide2e;

import android.support.test.uiautomator.By;

import org.junit.Ignore;
import org.junit.Test;

public class ScreenStackTest extends BaseTest {

	@Test
	public void pushAndPopScreen() throws Exception {
		launchTheApp();
		assertMainShown();
		elementByText("PUSH").click();
		assertExists(By.text("Pushed Screen"));
		elementByText("POP").click();
		assertMainShown();
	}

	@Test
	@Ignore
	public void popScreenDeepInTheStack() throws Exception {
		launchTheApp();
		assertMainShown();
		elementByText("PUSH").click();
		assertExists(By.text("Pushed Screen"));
		assertExists(By.text("Stack Position: 1"));
		elementByText("PUSH").click();
		assertExists(By.text("Stack Position: 2"));
	}
}