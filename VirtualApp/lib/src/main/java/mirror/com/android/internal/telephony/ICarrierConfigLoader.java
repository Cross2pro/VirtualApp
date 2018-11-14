package mirror.com.android.internal.telephony;

import android.os.IBinder;
import android.os.IInterface;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class ICarrierConfigLoader {
    public static Class<?> TYPE = RefClass.load(ICarrierConfigLoader.class, "com.android.internal.telephony.ICarrierConfigLoader");

    public static class Stub {
        public static Class<?> TYPE = RefClass.load(ICarrierConfigLoader.Stub.class, "com.android.internal.telephony.ICarrierConfigLoader$Stub");
        @MethodParams({IBinder.class})
        public static RefStaticMethod<IInterface> asInterface;
    }
}
