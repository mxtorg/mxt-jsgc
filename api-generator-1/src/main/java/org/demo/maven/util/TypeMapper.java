package org.demo.maven.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.swagger.v3.oas.models.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 类型映射工具类
 * 提供JSON Schema类型到Java类型的映射功能
 */
public class TypeMapper {

    private TypeMapper() {
        // 工具类禁止实例化
    }

    /**
     * 将JSON Schema类型转换为Java类型
     *
     * @param schema JSON Schema对象
     * @return JavaPoet TypeName
     */
    public static TypeName toJavaType(Schema<?> schema) {
        if (schema == null) {
            return TypeName.OBJECT;
        }

        String type = schema.getType();
        String format = schema.getFormat();
        String ref = schema.get$ref();

        // 处理引用类型
        if (ref != null && !ref.isEmpty()) {
            String className = NamingUtil.refToClassName(ref);
            // 这里暂时用Object，实际需要从components中获取包名
            return ClassName.get("", className);
        }

        // 处理数组类型
        if ("array".equals(type)) {
            Schema<?> items = schema.getItems();
            TypeName itemType = items != null ? toJavaType(items) : TypeName.OBJECT;
            return ParameterizedTypeName.get(ClassName.get(List.class), itemType);
        }

        // 处理基本类型
        if ("string".equals(type)) {
            return handleStringType(format);
        }

        if ("integer".equals(type)) {
            return handleIntegerType(format);
        }

        if ("number".equals(type)) {
            return handleNumberType(format);
        }

        if ("boolean".equals(type)) {
            return ClassName.get(Boolean.class);
        }

        return TypeName.OBJECT;
    }

    /**
     * 处理字符串类型
     */
    private static TypeName handleStringType(String format) {
        if (format == null) {
            return ClassName.get(String.class);
        }
        switch (format) {
            case "date":
                return ClassName.get(LocalDate.class);
            case "date-time":
                return ClassName.get(LocalDateTime.class);
            default:
                return ClassName.get(String.class);
        }
    }

    /**
     * 处理整数类型
     */
    private static TypeName handleIntegerType(String format) {
        if (format == null) {
            return ClassName.get(Integer.class);
        }
        switch (format) {
            case "int32":
                return ClassName.get(Integer.class);
            case "int64":
                return ClassName.get(Long.class);
            default:
                return ClassName.get(Integer.class);
        }
    }

    /**
     * 处理数字类型
     */
    private static TypeName handleNumberType(String format) {
        if (format == null) {
            return ClassName.get(Double.class);
        }
        switch (format) {
            case "float":
                return ClassName.get(Float.class);
            case "double":
                return ClassName.get(Double.class);
            default:
                return ClassName.get(Double.class);
        }
    }

    /**
     * 获取类型的默认值字符串
     *
     * @param type Java类型
     * @return 默认值字符串
     */
    public static String getDefaultValue(TypeName type) {
        if (type == null) {
            return "null";
        }

        String typeName = type.toString();

        if (typeName.equals("java.lang.Long")) {
            return "1L";
        }
        if (typeName.equals("java.lang.Integer")) {
            return "1";
        }
        if (typeName.equals("java.lang.Boolean")) {
            return "true";
        }
        if (typeName.equals("java.lang.String")) {
            return "\"\"";
        }
        if (typeName.equals("java.lang.Double") || typeName.equals("java.lang.Float")) {
            return "0.0";
        }

        // 对象类型
        if (type instanceof ClassName) {
            ClassName className = (ClassName) type;
            String simpleName = className.simpleName();
            if (!simpleName.equals("Object") && !simpleName.equals("String")
                    && !simpleName.equals("Integer") && !simpleName.equals("Long")
                    && !simpleName.equals("Boolean") && !simpleName.equals("Double")
                    && !simpleName.equals("Float")) {
                return "new " + simpleName + "()";
            }
        }

        return "null";
    }
}
