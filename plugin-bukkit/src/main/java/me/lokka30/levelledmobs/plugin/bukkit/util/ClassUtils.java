package me.lokka30.levelledmobs.plugin.bukkit.util;

import java.util.HashMap;

public class ClassUtils {

    private ClassUtils() { throw new IllegalStateException("Instantiation of utility-type class"); }

    private static final HashMap<String, Boolean> classExistsMap = new HashMap<>();

    public static boolean classExists(final String classpath) {
        if(classExistsMap.containsKey(classpath)) {
            return classExistsMap.get(classpath);
        } else {
            boolean result = true;
            try {
                Class.forName(classpath);
            } catch(ClassNotFoundException e) {
                result = false;
            }
            classExistsMap.put(classpath, result);
            return result;
        }
    }

}
