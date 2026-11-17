package cn.wanyiwige.alexcavesspellsbook.traveller.client;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Invoke optional public APIs, including implementations in non-public anonymous classes. */
public final class XaeroReflection {
    public static Object call(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        for (Method method : receiver.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !method.getParameterTypes()[i].isInstance(args[i])) matches = false;
            }
            if (!matches) continue;
            Method accessible = publicDeclaration(receiver.getClass(), receiver, name,
                    method.getParameterTypes(), new HashSet<>());
            if (accessible == null) throw new IllegalAccessException("No accessible public API for " + method);
            // Invoking the public superclass/interface declaration retains virtual dispatch to the override.
            return accessible.invoke(receiver, args);
        }
        throw new NoSuchMethodException(receiver.getClass().getName() + "." + name);
    }

    private static Method publicDeclaration(Class<?> type, Object receiver, String name,
                                            Class<?>[] parameters, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) return null;
        try {
            Method method = type.getMethod(name, parameters);
            if (method.canAccess(receiver)) return method;
        } catch (NoSuchMethodException ignored) { }
        for (Class<?> api : type.getInterfaces()) {
            Method method = publicDeclaration(api, receiver, name, parameters, visited);
            if (method != null) return method;
        }
        return publicDeclaration(type.getSuperclass(), receiver, name, parameters, visited);
    }

    private XaeroReflection() { }
}
