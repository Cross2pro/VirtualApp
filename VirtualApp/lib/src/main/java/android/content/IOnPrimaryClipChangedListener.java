/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\project\\va_official\\VirtualApp\\lib\\src\\main\\aidl\\android\\content\\IOnPrimaryClipChangedListener.aidl
 */
package android.content;
// Declare any non-default types here with import statements

public interface IOnPrimaryClipChangedListener extends android.os.IInterface
{
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements android.content.IOnPrimaryClipChangedListener
    {
        private static final java.lang.String DESCRIPTOR = "android.content.IOnPrimaryClipChangedListener";
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an android.content.IOnPrimaryClipChangedListener interface,
         * generating a proxy if needed.
         */
        public static android.content.IOnPrimaryClipChangedListener asInterface(android.os.IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof android.content.IOnPrimaryClipChangedListener))) {
                return ((android.content.IOnPrimaryClipChangedListener)iin);
            }
            return new android.content.IOnPrimaryClipChangedListener.Stub.Proxy(obj);
        }
        @Override public android.os.IBinder asBinder()
        {
            return this;
        }
        @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        {
            switch (code)
            {
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_dispatchPrimaryClipChanged:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.dispatchPrimaryClipChanged();
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
        private static class Proxy implements android.content.IOnPrimaryClipChangedListener
        {
            private android.os.IBinder mRemote;
            Proxy(android.os.IBinder remote)
            {
                mRemote = remote;
            }
            @Override public android.os.IBinder asBinder()
            {
                return mRemote;
            }
            public java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }
            @Override public void dispatchPrimaryClipChanged() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_dispatchPrimaryClipChanged, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
        static final int TRANSACTION_dispatchPrimaryClipChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    }
    public void dispatchPrimaryClipChanged() throws android.os.RemoteException;
}
