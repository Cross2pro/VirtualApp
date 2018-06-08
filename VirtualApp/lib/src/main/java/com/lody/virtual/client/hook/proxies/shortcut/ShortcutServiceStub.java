package com.lody.virtual.client.hook.proxies.shortcut;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArraySet;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.ParceledListSliceCompat;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mirror.android.content.pm.IShortcutService;
import mirror.android.content.pm.ParceledListSlice;

/**
 * @author Lody
 */
public class ShortcutServiceStub extends BinderInvocationProxy {


    public ShortcutServiceStub() {
        super(IShortcutService.Stub.TYPE, "shortcut");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getManifestShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getDynamicShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("setDynamicShortcuts"));
        addMethodProxy(new StaticMethodProxy("addDynamicShortcuts") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                return true;
            }
        });
        addMethodProxy(new ReplaceCallingPkgMethodProxy("createShortcutResultIntent"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("disableShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("enableShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getRemainingCallCount"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getRateLimitResetTime"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getIconMaxDimensions"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getMaxShortcutCountPerActivity"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("reportShortcutUsed"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("onApplicationActive"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("hasShortcutHostPermission"));

        addMethodProxy(new RequestPinShortcut());
        addMethodProxy(new GetPinnedShortcuts());
        addMethodProxy(new ReplaceCallingPkgMethodProxy("updateShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("removeAllDynamicShortcuts"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("removeDynamicShortcuts"));
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static class RequestPinShortcut extends ReplaceCallingPkgMethodProxy {
        public RequestPinShortcut() {
            super("requestPinShortcut");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (!VASettings.ENABLE_INNER_SHORTCUT) {
                return false;
            }
            ShortcutInfo shortcutInfo = (ShortcutInfo) args[1];
            if (shortcutInfo == null) {
                return false;
            }
            args[1] = wrapper(VClient.get().getCurrentApplication(), shortcutInfo, getAppPkg(), getAppUserId());
            return super.call(who, method, args);
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static class GetPinnedShortcuts extends ReplaceCallingPkgMethodProxy {
        public GetPinnedShortcuts() {
            super("getPinnedShortcuts");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            Object parceledListSlice = super.call(who, method, args);
            if (parceledListSlice != null) {
                //ParceledListSlice<ShortcutInfo>
                List<ShortcutInfo> result = new ArrayList<>();
                if (!VASettings.ENABLE_INNER_SHORTCUT) {
                    return ParceledListSliceCompat.create(result);
                }
                List list = ParceledListSlice.getList.call(parceledListSlice);
                if (list != null) {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        Object obj = list.get(i);
                        if (obj != null && (obj instanceof ShortcutInfo)) {
                            ShortcutInfo info = (ShortcutInfo) obj;
                            ShortcutInfo target = unWrapper(VClient.get().getCurrentApplication(), info, getAppPkg(), getAppUserId());
                            if (target != null) {
                                result.add(target);
                            }
                        }
                    }
                }
                return ParceledListSliceCompat.create(result);
            }
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static ShortcutInfo wrapper(Context appContext, ShortcutInfo shortcutInfo, String pkg, int userId) {
        Icon icon = Reflect.on(shortcutInfo).opt("mIcon");
        Bitmap bmp;
        if (icon != null) {
            bmp = BitmapUtils.drawableToBitmap(icon.loadDrawable(appContext));
        } else {
            PackageManager pm = VirtualCore.get().getPackageManager();
            bmp = BitmapUtils.drawableToBitmap(appContext.getApplicationInfo().loadIcon(pm));
        }
        Intent proxyIntent = VirtualCore.get().wrapperShortcutIntent(shortcutInfo.getIntent(), null, pkg, userId);
        proxyIntent.putExtra("_VA_|categories", setToString(shortcutInfo.getCategories()));
        proxyIntent.putExtra("_VA_|activity", shortcutInfo.getActivity());

        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(VirtualCore.get().getContext(),
                pkg + "@" + userId + "/" + shortcutInfo.getId());
        if (shortcutInfo.getLongLabel() != null) {
            builder.setLongLabel(shortcutInfo.getLongLabel());
        }
        if (shortcutInfo.getShortLabel() != null) {
            builder.setShortLabel(shortcutInfo.getShortLabel());
        }
        builder.setIcon(Icon.createWithBitmap(bmp));
        builder.setIntent(proxyIntent);

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private static ShortcutInfo unWrapper(Context appContext, ShortcutInfo shortcutInfo, String _pkg, int _userId) throws URISyntaxException {
        Intent intent = shortcutInfo.getIntent();
        if (intent == null) {
            return null;
        }
        String pkg = intent.getStringExtra("_VA_|_pkg_");
        int userId = intent.getIntExtra("_VA_|_user_id_", 0);
        if (TextUtils.equals(pkg, _pkg) && userId == _userId) {
            String _id = shortcutInfo.getId();
            String id = _id.substring(_id.indexOf("/") + 1);
            Icon icon = Reflect.on(shortcutInfo).opt("mIcon");
            String uri = intent.getStringExtra("_VA_|_uri_");
            Intent targetIntent = null;
            if (!TextUtils.isEmpty(uri)) {
                targetIntent = Intent.parseUri(uri, 0);
            }
            ComponentName componentName = intent.getParcelableExtra("_VA_|activity");
            String categories = intent.getStringExtra("_VA_|categories");
            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(appContext, id);
            if (icon != null) {
                builder.setIcon(icon);
            }
            if (shortcutInfo.getLongLabel() != null) {
                builder.setLongLabel(shortcutInfo.getLongLabel());
            }
            if (shortcutInfo.getShortLabel() != null) {
                builder.setShortLabel(shortcutInfo.getShortLabel());
            }
            if (componentName != null) {
                builder.setActivity(componentName);
            }
            if (targetIntent != null) {
                builder.setIntent(targetIntent);
            }
            Set<String> cs = toSet(categories);
            if (cs != null) {
                builder.setCategories(cs);
            }
            return builder.build();
        } else {
            return null;
        }
    }

    private static <T> String setToString(Set<T> sets) {
        if (sets == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<T> iterator = sets.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            stringBuilder.append(iterator.next());
        }
        return stringBuilder.toString();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static Set<String> toSet(String allStr) {
        if (allStr == null) {
            return null;
        }
        String[] strs = allStr.split(",");
        Set<String> sets = new ArraySet<>();
        for (String str : strs) {
            sets.add(str);
        }
        return sets;
    }
}
