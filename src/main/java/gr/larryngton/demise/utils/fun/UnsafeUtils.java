package gr.larryngton.demise.utils.fun;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
    public static Unsafe theUnsafe = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Class<?> unsafeKlass = Unsafe.class;
            Field theUnsafeField = unsafeKlass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            return (Unsafe) theUnsafeField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't get the unsafe");
        }
    }
}
