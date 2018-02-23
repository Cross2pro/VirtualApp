package mirror.android.widget;

import android.os.IInterface;

import mirror.RefClass;
import mirror.RefStaticMethod;
import mirror.RefStaticObject;

public class Toast {

    public static final int LENGTH_LONG = 1;
    public static final int LENGTH_SHORT = 0;

    public static Class<?> TYPE = RefClass.load(Toast.class, android.widget.Toast.class);

    public static RefStaticObject<IInterface> sService;
    public static RefStaticMethod<android.widget.Toast> makeText;
    public static mirror.RefMethod<Void> show;

}
