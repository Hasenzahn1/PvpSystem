package me.hasenzahn1.pvp.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ObjectPrinter {

    public static void print(Object obj){
        System.out.println(printInternal(obj));
    }

    public static String printInternal(Object obj) {
        if (obj == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        Class<?> clazz = obj.getClass();
        sb.append("=== Object of type: ").append(clazz.getName()).append(" ===\n");

        // Durchlaufe die Klassenhierarchie von unten nach oben
        while (clazz != null && clazz != Object.class) {
            sb.append("\n--- Fields from class: ").append(clazz.getSimpleName()).append(" ---\n");

            Field[] fields = clazz.getDeclaredFields();

            if (fields.length == 0) {
                sb.append("(no fields)\n");
            }

            for (Field field : fields) {
                // Statische Felder überspringen (optional)
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true); // Zugriff auch auf private Felder

                try {
                    Object value = field.get(obj);
                    String modifiers = Modifier.toString(field.getModifiers());
                    sb.append("  ").append(modifiers).append(" ")
                            .append(field.getType().getSimpleName()).append(" ")
                            .append(field.getName()).append(" = ").append(value).append("\n");
                } catch (IllegalAccessException e) {
                    sb.append("  ").append(field.getName()).append(" = <inaccessible>\n");
                }
            }

            // Zur Elternklasse wechseln
            clazz = clazz.getSuperclass();
        }

        sb.append("\n===================\n");
        return sb.toString();
    }
}