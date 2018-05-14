package com.lody.virtual.helper.ipcbus;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */
public class IPCMethod {

    private static final int VAL_PARCELABLE = 4;
    private static final int VAL_PARCELABLEARRAY = 16;

    private int code;
    private Method method;
    private String interfaceName;
    private MethodParamConverter[] converters;
    private MethodParamConverter resultConverter;


    public IPCMethod(int code, Method method, String interfaceName) {
        this.code = code;
        this.method = method;
        this.interfaceName = interfaceName;
        Class<?>[] parameterTypes = method.getParameterTypes();
        converters = new MethodParamConverter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            converters[i] = getConverter(parameterTypes[i]);
        }
        Class<?> returnType = method.getReturnType();
        resultConverter = getConverter(returnType);
    }

    private MethodParamConverter getConverter(Class<?> type) {
        if (isAidlParam(type)) {
            return new AidlParamConverter(type);
        }
        if (type.isArray()) {
            return new ParcelArrayParamConverter(type.getComponentType());
        }
        return null;
    }

    private boolean isAidlParam(Class<?> type) {
        return type.isInterface() && IInterface.class.isAssignableFrom(type);
    }


    public String getInterfaceName() {
        return interfaceName;
    }

    public Method getMethod() {
        return method;
    }

    public void handleTransact(Object server, Parcel data, Parcel reply) {
        data.enforceInterface(interfaceName);
        Object[] parameters = data.readArray(getClass().getClassLoader());
        if (parameters != null && parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                if (converters[i] != null) {
                    parameters[i] = converters[i].convert(parameters[i]);
                }
            }
        }
        try {
            Object res = method.invoke(server, parameters);
            reply.writeNoException();
            reply.writeValue(res);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            reply.writeException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            reply.writeException(e);
        }
    }

    private static Method findAsInterfaceMethod(Class<?> type) {
        for (Class<?> innerClass : type.getDeclaredClasses()) {
            // public static class Stub extends Binder implements IType
            if (Modifier.isStatic(innerClass.getModifiers())
                    && Binder.class.isAssignableFrom(innerClass)
                    && type.isAssignableFrom(innerClass)) {
                // public static IType asInterface(android.os.IBinder obj)
                for (Method method : innerClass.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        Class<?>[] types = method.getParameterTypes();
                        if (types.length == 1 && types[0] == IBinder.class) {
                            return method;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Can not found the " + type.getName() + "$Stub.asInterface method.");
    }


    private static final Map<Class<?>, String> sAncestorClasses = new HashMap<>();


    private static boolean hasClass(String className) {
        try {
            IPCMethod.class.getClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String getAncestorClass(Class<?> clazz) {
        synchronized (sAncestorClasses) {
            String target = sAncestorClasses.get(clazz);
            if (target == null) {
                while (!hasClass(clazz.getName())) {
                    clazz = clazz.getSuperclass();
                    if (clazz == Object.class || clazz == null) {
                        break;
                    }
                }
                if (clazz != null) {
                    target = clazz.getName();
                    sAncestorClasses.put(clazz, target);
                }
            }
            return target;
        }
    }

    private static void writeParcelable(Parcel data, Parcelable arg,int parcelableFlags) {
        if(arg == null){
            data.writeValue(null);
        }else {
            String className = arg.getClass().getName();
            if (!hasClass(className)) {
                String ancestorClassName = getAncestorClass(arg.getClass());
                if (ancestorClassName == null) {
                    throw new RuntimeException("Can't find ancestor class for " + arg.getClass());
                }
                className = ancestorClassName;
            }
            data.writeString(className);
            arg.writeToParcel(data, parcelableFlags);
        }
    }

    private static void writeParcelableArray(Parcel data, Parcelable[] array,int parcelableFlags) {
        if(array != null) {
            int N = array.length;
            data.writeInt(N);
            for (int i=0; i<N; i++) {
                writeParcelable(data, array[i], parcelableFlags);
            }
        }else{
            data.writeInt(-1);
        }
    }

    public Object callRemote(IBinder server, Object[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        Object result;
        try {
            data.writeInterfaceToken(interfaceName);
            if (args == null) {
                data.writeValue(null);
            } else {
                data.writeInt(args.length);
                for (Object arg : args) {
                    if (arg == null) {
                        data.writeValue(null);
                    } else if (arg instanceof Parcelable) {
                        data.writeInt(VAL_PARCELABLE);
                        writeParcelable(data, (Parcelable) arg, 0);
                    } else if (arg instanceof Parcelable[]) {
                        data.writeInt(VAL_PARCELABLEARRAY);
                        writeParcelableArray(data, (Parcelable[]) arg, 0);
                    } else {
                        data.writeValue(arg);
                    }
                }
            }
            server.transact(code, data, reply, 0);
            reply.readException();
            result = reply.readValue(getClass().getClassLoader());
            if (resultConverter != null) {
                result = resultConverter.convert(result);
            }
        } finally {
            data.recycle();
            reply.recycle();
        }
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IPCMethod ipcMethod = (IPCMethod) o;

        return method != null ? method.equals(ipcMethod.method) : ipcMethod.method == null;
    }

    public interface MethodParamConverter {
        Object convert(Object param);
    }

    private class ParcelArrayParamConverter implements MethodParamConverter {

        private Class<?> componentType;

        public ParcelArrayParamConverter(Class<?> componentType) {
            this.componentType = componentType;
        }

        @Override
        public Object convert(Object param) {
            if (param instanceof Parcelable[]) {
                Parcelable[] parcelables = (Parcelable[]) param;
                Object[] results = (Object[]) Array.newInstance(componentType, parcelables.length);
                System.arraycopy(parcelables, 0, results, 0, results.length);
                return results;
            }
            return param;
        }
    }

    private class AidlParamConverter implements MethodParamConverter {

        private Method asInterfaceMethod;
        private Class<?> type;

        AidlParamConverter(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object convert(Object param) {
            if (param != null) {
                if (asInterfaceMethod == null) {
                    synchronized (this) {
                        if (asInterfaceMethod == null) {
                            asInterfaceMethod = findAsInterfaceMethod(type);
                        }
                    }
                }
                try {
                    return asInterfaceMethod.invoke(null, param);
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            } else if (type == int.class || type == Integer.class) {
                return -1;
            }
            return null;
        }
    }

}
