package org.demo.maven.util;

/**
 * 命名工具类
 * 提供路径转方法名、引用转类名等命名转换功能
 */
public class NamingUtil {

    private NamingUtil() {
        // 工具类禁止实例化
    }

    /**
     * 路径转方法名
     *
     * @param httpMethod HTTP方法（get/post/put/delete）
     * @param path       路径（如 /users/instance）
     * @return 方法名（如 postUserInstance）
     */
    public static String pathToMethodName(String httpMethod, String path) {
        if (path == null || path.isEmpty()) {
            return httpMethod;
        }
        
        // 移除开头的'/'
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        
        // 按'/'分割
        String[] parts = cleanPath.split("/");
        
        StringBuilder result = new StringBuilder();
        result.append(httpMethod.toLowerCase());
        
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                result.append(capitalize(part));
            }
        }
        
        return result.toString();
    }

    /**
     * 引用路径转类名
     *
     * @param ref 引用路径（如 #/components/schemas/DemoEndpoint）
     * @return 类名（如 DemoEndpoint）
     */
    public static String refToClassName(String ref) {
        if (ref == null || ref.isEmpty()) {
            return "";
        }
        
        int lastSlashIndex = ref.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < ref.length() - 1) {
            return ref.substring(lastSlashIndex + 1);
        }
        
        return ref;
    }

    /**
     * 下划线转驼峰
     *
     * @param str 下划线字符串（如 user_name）
     * @return 驼峰字符串（如 userName）
     */
    public static String underscoreToCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }

    /**
     * 首字母大写
     *
     * @param str 输入字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 首字母小写
     *
     * @param str 输入字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 包名转路径
     *
     * @param packageName 包名（如 org.demo.cloud）
     * @return 路径（如 org/demo/cloud）
     */
    public static String packageToPath(String packageName) {
        if (packageName == null) {
            return "";
        }
        return packageName.replace('.', '/');
    }
}
