package me.lokka30.levelledmobs.bukkit.utils;

import java.util.HashMap;

public final class ClassUtils {

    private ClassUtils() {
        throw new IllegalStateException("Instantiation of utility-type class");
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
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
                classExists = false;
            }

            classExistsMap.put(classpath, classExists);
            return classExists;
        }
    }

}
