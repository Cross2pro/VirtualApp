package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.WindowManager;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.server.am.AttributeCache;

import mirror.com.android.internal.R_Hide;

/**
 * @author Lody
 */
public class WindowPreviewActivity extends Activity {

    private long startTime;


    public static void previewActivity(int userId, ActivityInfo info) {
        Context context = VirtualCore.get().getContext();
        Intent windowBackgroundIntent = new Intent(context, WindowPreviewActivity.class);
        windowBackgroundIntent.putExtra("_VA_|user_id", userId);
        windowBackgroundIntent.putExtra("_VA_|activity_info", info);
        windowBackgroundIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        windowBackgroundIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(windowBackgroundIntent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        ActivityInfo info = intent.getParcelableExtra("_VA_|activity_info");
        int userId = intent.getIntExtra("_VA_|user_id", -1);
        if (info == null || userId == -1) {
            finish();
            return;
        }
        int theme = info.theme;
        if (theme == 0) {
            theme = info.applicationInfo.theme;
        }
        AttributeCache.Entry windowExt = AttributeCache.instance().get(info.packageName, theme,
                R_Hide.styleable.Window.get());
        if (windowExt != null) {
            boolean fullscreen = windowExt.array.getBoolean(R_Hide.styleable.Window_windowFullscreen.get(), false);
            boolean translucent = windowExt.array.getBoolean(R_Hide.styleable.Window_windowIsTranslucent.get(), false);
            boolean disablePreview = windowExt.array.getBoolean(R_Hide.styleable.Window_windowDisablePreview.get(), false);
            if (disablePreview) {
                return;
            }
            if (fullscreen) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            Drawable drawable = null;
            try {
                drawable = windowExt.array.getDrawable(R_Hide.styleable.Window_windowBackground.get());
            } catch (Throwable e) {
                // ignore
            }
            if (drawable == null) {
                AttributeCache.Entry viewEnt = AttributeCache.instance().get(info.packageName, info.theme,
                        R_Hide.styleable.View.get());
                if (viewEnt != null) {
                    try {
                        drawable = viewEnt.array.getDrawable(R_Hide.styleable.View_background.get());
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }

            if (drawable != null) {
                getWindow().setBackgroundDrawable(drawable);
            } else {
                if (!translucent) {
                    getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                }
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.dimAmount = 0.4f;
                getWindow().setAttributes(lp);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }
    }

    @Override
    public void onBackPressed() {
        long time = System.currentTimeMillis();
        if (time - startTime > 5000L) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
