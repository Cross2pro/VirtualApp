package mirror.android.content;

import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

/**
 * Created by Administrator on 2018/1/9 0009.
 */

public class Intent {
    public static Class<?> Class = RefClass.load(Intent.class, android.content.Intent.class);
    @MethodParams({String.class})
    public static RefMethod<IBinder> getIBinderExtra;

    @MethodParams({String.class, IBinder.class})
    public static RefMethod<Intent> putExtra;
}
