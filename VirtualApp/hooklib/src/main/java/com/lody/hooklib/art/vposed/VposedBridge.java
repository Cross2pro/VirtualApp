/*
 * Original work Copyright (c) 2005-2008, The Android Open Source Project
 * Modified work Copyright (c) 2013, rovo89 and Tungstwenty
 * Modified work Copyright (c) 2015, Alibaba Mobile Infrastructure (Android) Team
 * Modified work Copyright (c) 2017, Lody
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lody.hooklib.art.vposed;

import android.util.Log;

import com.lody.hooklib.art.ArtUtils;
import com.lody.hooklib.art.vposed.XC_MethodHook.MethodHookParam;
import com.lody.hooklib.art.vposed.XC_MethodHook.Unhook;
import com.lody.hooklib.art.vposed.XC_MethodHook.XC_MethodKeepHook;
import com.lody.hooklib.art.vposed.XC_MethodReplacement.XC_MethodKeepReplacement;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * need android's version >= 21
 * @see android.os.Build.VERSION#SDK_INT >= 21
 */
public final class VposedBridge {

    private static final String TAG = "VposedBridge";

    private static final Object[] EMPTY_ARRAY = new Object[0];
    public static final ClassLoader BOOTCLASSLOADER = ClassLoader.getSystemClassLoader();


    // built-in handlers
    private static final Map<Member, CopyOnWriteSortedSet<XC_MethodHook>> hookedMethodCallbacks
            = new HashMap<>();

    private static final Map<Member, Member> hookedMethodBackups
            = new HashMap<>();

    private static final ArrayList<Unhook> allUnhookCallbacks = new ArrayList<Unhook>();


    /**
     * Writes a message to BASE_DIR/log/debug.log (needs to have chmod 777)
     *
     * @param text log message
     */
    public synchronized static void log(String text) {
        Log.i(TAG, text);
    }

    /**
     * Log the stack trace
     *
     * @param t The Throwable object for the stacktrace
     * @see VposedBridge#log(String)
     */
    public synchronized static void log(Throwable t) {
        log(Log.getStackTraceString(t));
    }

    /**
     * Hook any method with the specified callback
     *
     * @param hookMethod The method to be hooked
     * @param callback
     */
    public static Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (!(hookMethod instanceof Method) && !(hookMethod instanceof Constructor<?>)) {
            throw new IllegalArgumentException("only methods and constructors can be hooked");
        }

        boolean newMethod = false;
        CopyOnWriteSortedSet<XC_MethodHook> callbacks;
        synchronized (hookedMethodCallbacks) {
            callbacks = hookedMethodCallbacks.get(hookMethod);
            if (callbacks == null) {
                callbacks = new CopyOnWriteSortedSet<XC_MethodHook>();
                hookedMethodCallbacks.put(hookMethod, callbacks);
                newMethod = true;
            }
        }
        callbacks.add(callback);
        if (newMethod) {
            Member backupMethod = ArtUtils.hookMethod(hookMethod);
            hookedMethodBackups.put(hookMethod, backupMethod);
        }
        return callback.new Unhook(hookMethod);
    }

    /**
     * Removes the callback for a hooked method
     *
     * @param hookMethod The method for which the callback should be removed
     * @param callback   The reference to the callback as specified in {@link #hookMethod}
     */
    public static void unhookMethod(Member hookMethod, XC_MethodHook callback) {
        CopyOnWriteSortedSet<XC_MethodHook> callbacks;
        synchronized (hookedMethodCallbacks) {
            callbacks = hookedMethodCallbacks.get(hookMethod);
            if (callbacks == null)
                return;
        }
        callbacks.remove(callback);
    }

    public static Set<Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<Unhook> unhooks = new HashSet<Unhook>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName))
                unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    public static Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook))
            throw new IllegalArgumentException("no callback defined");

        XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        Method m = VposedHelpers.findMethodExact(clazz, methodName, parameterTypesAndCallback);
        Log.i(TAG, "findAndHookMethod: " + m.toGenericString());
        Unhook unhook = hookMethod(m, callback);
        if (!(callback instanceof XC_MethodKeepHook
                || callback instanceof XC_MethodKeepReplacement)) {
            synchronized (allUnhookCallbacks) {
                allUnhookCallbacks.add(unhook);
            }
        }
        return unhook;
    }

    public static void unhookAllMethods() {
        synchronized (allUnhookCallbacks) {
            for (int i = 0; i < allUnhookCallbacks.size(); i++) {
                allUnhookCallbacks.get(i).unhook();
            }
            allUnhookCallbacks.clear();
        }
    }

    public static Set<Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<Unhook> unhooks = new HashSet<Unhook>();
        for (Member constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    public static Object handleHookedArtMethod(Member hookedMethod, Object thisObject, Object[] args) {
        CopyOnWriteSortedSet<XC_MethodHook> callbacks;

        synchronized (hookedMethodCallbacks) {
            callbacks = hookedMethodCallbacks.get(hookedMethod);
        }
        Object[] callbacksSnapshot = callbacks.getSnapshot();
        final int callbacksLength = callbacksSnapshot.length;
        if (callbacksLength == 0) {
            try {
                return invokeOriginalMethod(hookedMethod, thisObject, args);
            } catch (Exception e) {
                log(e.getCause());
            }
        }

        MethodHookParam param = new MethodHookParam();
        param.method = hookedMethod;
        param.thisObject = thisObject;
        param.args = args;

        // call "before method" callbacks
        int beforeIdx = 0;
        do {
            try {
                ((XC_MethodHook) callbacksSnapshot[beforeIdx]).beforeHookedMethod(param);
            } catch (Throwable t) {
                log(t);

                // reset result (ignoring what the unexpectedly exiting callback did)
                param.setResult(null);
                param.returnEarly = false;
                continue;
            }

            if (param.returnEarly) {
                // skip remaining "before" callbacks and corresponding "after" callbacks
                beforeIdx++;
                break;
            }
        } while (++beforeIdx < callbacksLength);

        // call original method if not requested otherwise
        if (!param.returnEarly) {
            try {
                Object result = invokeOriginalMethod(hookedMethod, param.thisObject, param.args);
                param.setResult(result);
            } catch (Exception e) {
                log(e);
                param.setThrowable(e);
            }
        }

        // call "after method" callbacks
        int afterIdx = beforeIdx - 1;
        do {
            Object lastResult = param.getResult();
            Throwable lastThrowable = param.getThrowable();

            try {
                ((XC_MethodHook) callbacksSnapshot[afterIdx]).afterHookedMethod(param);
            } catch (Throwable t) {
                VposedBridge.log(t);

                // reset to last result (ignoring what the unexpectedly exiting callback did)
                if (lastThrowable == null)
                    param.setResult(lastResult);
                else
                    param.setThrowable(lastThrowable);
            }
        } while (--afterIdx >= 0);

        if (param.hasThrowable()) {
            final Throwable throwable = param.getThrowable();
            if (throwable instanceof IllegalAccessException || throwable instanceof InvocationTargetException
                    || throwable instanceof InstantiationException) {
                // reflect exception, get the origin cause
                final Throwable cause = throwable.getCause();

                // We can not change the exception flow of origin call, rethrow
                Log.e(TAG, "origin call throw exception (not a real crash, just record for debug):", cause);
                VposedBridge.<RuntimeException>throwNoCheck(param.getThrowable().getCause(), null);
                return null; //never reach.
            } else {
                // the exception cause by epic self, just log.
                Log.w(TAG, "epic cause exception in call bridge!!");
            }
            return null; // never reached.
        } else {
            final Object result = param.getResult();
            Log.d(TAG, "return :" + result);
            return result;
        }
    }

    /**
     * Just for throw an checked exception without check
     *
     * @param exception The checked exception.
     * @param dummy     dummy.
     * @param <T>       fake type
     * @throws T the checked exception.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwNoCheck(Throwable exception, Object dummy) throws T {
        throw (T) exception;
    }


    /**
     * Basically the same as {@link Method#invoke}, but calls the original method
     * as it was before the interception by Xposed. Also, access permissions are not checked.
     *
     * @param member     Method to be called
     * @param thisObject For non-static calls, the "this" pointer
     * @param args       Arguments for the method call as Object[] array
     * @return The result returned from the invoked method
     * @throws NullPointerException      if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException    if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException  if the number of arguments doesn't match the number of parameters, the receiver
     *                                   is incompatible with the declaring class, or an argument could not be unboxed
     *                                   or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException if an exception was thrown by the invoked method
     */
    public static Object invokeOriginalMethod(Member member, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        Member backupMember = hookedMethodBackups.get(member);
        if (backupMember == null) {
            throw new IllegalArgumentException("Origin method not exist.");
        }
        if (args == null) {
            args = EMPTY_ARRAY;
        }
        if (member instanceof Constructor) {
            Method nonVirtualInitMethod = (Method) backupMember;
            nonVirtualInitMethod.setAccessible(true);
            nonVirtualInitMethod.invoke(thisObject, args);
            return null;
        } else {
            Method method = (Method) backupMember;
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        }
    }

    public static class CopyOnWriteSortedSet<E> {
        private transient volatile Object[] elements = EMPTY_ARRAY;

        public synchronized boolean add(E e) {
            int index = indexOf(e);
            if (index >= 0)
                return false;

            Object[] newElements = new Object[elements.length + 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            newElements[elements.length] = e;
            Arrays.sort(newElements);
            elements = newElements;
            return true;
        }

        public synchronized boolean remove(E e) {
            int index = indexOf(e);
            if (index == -1)
                return false;

            Object[] newElements = new Object[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
            elements = newElements;
            return true;
        }

        public synchronized void clear() {
            elements = EMPTY_ARRAY;
        }

        private int indexOf(Object o) {
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
            return -1;
        }

        public Object[] getSnapshot() {
            return elements;
        }
    }
}