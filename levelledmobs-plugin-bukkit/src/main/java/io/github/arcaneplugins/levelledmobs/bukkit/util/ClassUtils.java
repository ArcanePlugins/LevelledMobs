package io.github.arcaneplugins.levelledmobs.bukkit.util;

import java.util.HashMap;

public final class ClassUtils {

    private ClassUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    /*
    Key: String (classpath); val: Boolean (classExists)
     */
    private static final HashMap<String, Boolean> classExistsMap = new HashMap<>();

    public static boolean classExists(final String classpath) {
        if(classExistsMap.containsKey(classpath)) {
            return classExistsMap.get(classpath);
        } else {
            boolean classExists = true;

            try {
                Class.forName(classpath);
            } catch(final ClassNotFoundException ignored) {
                classExists = false;
            }

            classExistsMap.put(classpath, classExists);
            return classExists;
        }
    }

}
