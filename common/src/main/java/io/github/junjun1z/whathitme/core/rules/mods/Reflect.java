package io.github.junjun1z.whathitme.core.rules.mods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Small reflection helpers of the quest mod providers (names never depend on Minecraft mappings). */
final class Reflect {

    private Reflect() {}

    /** The first class of these names that exists, null = none */
    static Class<?> first(String... names) {
        for (String n : names) {
            try {
                return Class.forName(n);
            } catch (Throwable ignored) {
                // next
            }
        }
        return null;
    }

    static Object staticField(Class<?> c, String name) throws Exception {
        Field f = c.getField(name);
        return f.get(null);
    }

    /** A public method by name whose parameters accept these argument types (null type = any object) */
    static Method method(Class<?> c, String name, Class<?>... args) {
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] p = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < p.length && ok; i++) ok = args[i] == null || box(p[i]).isAssignableFrom(box(args[i]));
            if (ok) return m;
        }
        return null;
    }

    static Method staticMethod(Class<?> c, String name, Class<?>... args) {
        Method m = method(c, name, args);
        return m != null && Modifier.isStatic(m.getModifiers()) ? m : null;
    }

    /** A public no-argument method of the object whose result fits this type (e.g. Entity#getServer), null = none */
    static Object getterOfType(Object o, Class<?> type) throws Exception {
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) && type.isAssignableFrom(m.getReturnType())
                && m.getReturnType() != Object.class) {
                Object v = m.invoke(o);
                if (v != null) return v;
            }
        }
        return null;
    }

    /** The value of a public no-argument method, else of a public field of that name; null = neither */
    static Object read(Object o, String method, String field) {
        try {
            Method m = method == null ? null : method(o.getClass(), method);
            if (m != null) {
                open(m);
                return m.invoke(o);
            }
            return field == null ? null : o.getClass().getField(field).get(o);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Public methods of non-public implementation classes need this */
    static void open(Method m) {
        try {
            m.setAccessible(true);
        } catch (Throwable ignored) {
            // keep the normal access check
        }
    }

    /** {@link #read} as a collection (empty when missing) */
    static java.util.Collection<?> list(Object o, String method, String field) {
        Object v = read(o, method, field);
        return v instanceof java.util.Collection ? (java.util.Collection<?>) v : java.util.Collections.emptyList();
    }

    /** "id" or "id	title" for the pack editor; formatting codes removed */
    static String entry(String id, Object title) {
        String t = title == null ? "" : String.valueOf(title).replaceAll("§.", "").replace('	', ' ').trim();
        return t.isEmpty() ? id : id + "	" + t;
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == short.class) return Short.class;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        return Character.class;
    }

    static boolean truth(Object o) {
        return o instanceof Boolean && (Boolean) o;
    }
}
