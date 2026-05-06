package org.demo.maven.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;

public class TypeMapper {
    
    public static TypeName toJavaType(Schema schema) {
        if (schema == null) {
            return TypeName.OBJECT;
        }
        
        String type = schema.getType();
        String format = schema.getFormat();
        
        if ("string".equals(type)) {
            return ClassName.get(String.class);
        } else if ("integer".equals(type)) {
            if ("int32".equals(format)) {
                return TypeName.INT;
            } else {
                return TypeName.LONG;
            }
        } else if ("number".equals(type)) {
            if ("float".equals(format)) {
                return TypeName.FLOAT;
            } else {
                return TypeName.DOUBLE;
            }
        } else if ("boolean".equals(type)) {
            return TypeName.BOOLEAN;
        } else if ("array".equals(type)) {
            Schema items = ((ArraySchema) schema).getItems();
            return ParameterizedTypeName.get(ClassName.get(List.class), toJavaType(items));
        } else if ("object".equals(type)) {
            return ClassName.get(Object.class);
        } else {
            return ClassName.get(String.class);
        }
    }
    
    public static String resolveRefName(String ref) {
        if (ref != null && ref.startsWith("#/components/schemas/")) {
            return ref.substring("#/components/schemas/".length());
        }
        return "Object";
    }
}