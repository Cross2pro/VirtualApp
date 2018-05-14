package com.lody.hooklib.art;

import android.os.Build;
import android.util.Log;


import com.lody.hooklib.art.vposed.VposedBridge;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * @author Lody
 */
public class ArtUtils {

    static {
        System.loadLibrary("vposed_art");
    }
    
    public static int[] getTypeIds(Class<?>[] types) {
        int size = types.length + 1;
        int[] ids = new int[size];
        int startPos = 1;
        for (Class<?> type : types) {
            ids[startPos++] = getTypeId(type);
        }
        ids[0] = getTypeId(Object.class);
        return ids;
    }

    public static int getReturnTypeId(Member member) {
        if (member instanceof Constructor) {
            return getTypeId(void.class);
        } else if (member instanceof Method) {
            return getTypeId(((Method) member).getReturnType());
        }
        throw new IllegalArgumentException();
    }

    public static void setObjectClass(Object object, Class<?> clazz) {
        try {
            Field field = Object.class.getDeclaredField("shadow$_klass_");
            field.setAccessible(true);
            field.set(object, clazz);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int getTypeId(Class<?> type) {
        if (type == void.class) {
            return -1;
        } else if (type == int.class) {
            return 0;
        } else if (type == short.class) {
            return 1;
        } else if (type == float.class) {
            return 2;
        } else if (type == boolean.class) {
            return 3;
        } else if (type == char.class) {
            return 4;
        } else if (type == byte.class) {
            return 5;
        } else if (type == long.class) {
            return 6;
        } else if (type == double.class) {
            return 7;
        } else { // Object
            return 8;
        }
    }

    public static Object onCallback(Member hooked, Object[] selfAndArgs) throws Exception {
        Log.e("VPosed", "onCallback : " + hooked + Arrays.toString(selfAndArgs));
        Object self = selfAndArgs[0];
        if (Modifier.isStatic(hooked.getModifiers())) {
            self = null;
        }
        Object[] args = new Object[selfAndArgs.length - 1];
        System.arraycopy(selfAndArgs, 1, args, 0, args.length);
        Object res = VposedBridge.handleHookedArtMethod(hooked, self, args);
        Log.e("VPosed", "return : " + res);
        return res;
    }

    public static Method createNonVirtualInitMethod(Constructor constructor) throws Throwable {
        Class<?> abstractMethodClass = Method.class.getSuperclass();
        Constructor<Method> methodConstructor = Method.class.getDeclaredConstructor();
        Field override = AccessibleObject.class.getDeclaredField(
                Build.VERSION.SDK_INT == Build.VERSION_CODES.M ? "flag" : "override");
        override.setAccessible(true);
        override.set(methodConstructor, true);

        Method method = methodConstructor.newInstance();
        method.setAccessible(true);
        for (Field field : abstractMethodClass.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(method, field.get(constructor));
        }
        return method;
    }

    public static Member hookMethod(Member member) {
        try {
            Class.forName(member.getDeclaringClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Class<?>[] types;
        if (member instanceof Method) {
            types = ((Method) member).getParameterTypes();
        } else if (member instanceof Constructor) {
            types = ((Constructor) member).getParameterTypes();
        } else {
            throw new IllegalArgumentException();
        }
        boolean isStatic = Modifier.isStatic(member.getModifiers());
        Member backup = (Member) hookMethod(member.getDeclaringClass(), member, isStatic, types.length + 1, ArtUtils.getTypeIds(types), ArtUtils.getReturnTypeId(member));
        if (backup instanceof Constructor) {
            try {
                return ArtUtils.createNonVirtualInitMethod((Constructor) backup);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        return backup;
    }


    private static native Object hookMethod(Class<?> methodClass, Object method, boolean isStatic, int paramCount, int[] paramTypeIds, int returnType);

    private static native void reserved0();

    private static native void reserved1();

}
