package org.demo.maven.util;

public class NamingUtil {
    
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "Api";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    public static String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
    
    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : str.toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    public static String toPascalCase(String str) {
        String camelCase = toCamelCase(str);
        return capitalize(camelCase);
    }
}