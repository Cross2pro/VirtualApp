package andhook.lib.utils;

public class ArrayUtils {
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    public static boolean isSameLength(Object[] array1, Object[] array2) {
        if ((array1 == null && array2 != null && array2.length > 0) ||
                (array2 == null && array1 != null && array1.length > 0) ||
                (array1 != null && array2 != null && array1.length != array2.length)) {
            return false;
        }
        return true;
    }

}
