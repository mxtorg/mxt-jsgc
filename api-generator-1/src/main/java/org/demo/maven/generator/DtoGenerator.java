package org.demo.maven.generator;

import com.squareup.javapoet.*;
import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;
import org.demo.maven.util.NamingUtil;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DTO类生成器
 */
public class DtoGenerator implements CodeGenerator {

    @Override
    public String getName() {
        return "dto-generator";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void generate(ApiConfig config, File outputDir) throws GeneratorException {
        if (config.getComponents() == null || config.getComponents().getSchemas() == null) {
            return;
        }

        String basePackage = config.getGav().getPkg();
        String dtoPackage = basePackage + ".dto";

        Map<String, ApiConfig.Components.SchemaConfig> schemas = config.getComponents().getSchemas();

        for (Map.Entry<String, ApiConfig.Components.SchemaConfig> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            ApiConfig.Components.SchemaConfig schema = entry.getValue();

            // 跳过Endpoint类（没有properties）
            if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
                continue;
            }

            try {
                generateDtoClass(schemaName, schema, dtoPackage, outputDir);
            } catch (IOException e) {
                throw new GeneratorException("生成DTO类失败: " + schemaName, e);
            }
        }
    }

    private void generateDtoClass(String className, ApiConfig.Components.SchemaConfig schema,
                                  String packageName, File outputDir) throws IOException {
        Set<String> requiredFields = new HashSet<>();
        if (schema.getRequired() != null) {
            requiredFields.addAll(Arrays.asList(schema.getRequired()));
        }

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        // 添加类注解
        classBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Data")).build());
        
        if (schema.getDescription() != null) {
            AnnotationSpec schemaAnnotation = AnnotationSpec.builder(
                    ClassName.get("io.swagger.v3.oas.annotations.media", "Schema"))
                    .addMember("description", "$S", schema.getDescription())
                    .build();
            classBuilder.addAnnotation(schemaAnnotation);
        }

        // 添加构造函数注解
        classBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "NoArgsConstructor")).build());
        classBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "AllArgsConstructor")).build());

        // 添加字段
        if (schema.getProperties() != null) {
            for (Map.Entry<String, ApiConfig.Components.SchemaConfig.PropertyConfig> fieldEntry : schema.getProperties().entrySet()) {
                String fieldName = fieldEntry.getKey();
                ApiConfig.Components.SchemaConfig.PropertyConfig property = fieldEntry.getValue();

                FieldSpec fieldSpec = buildFieldSpec(fieldName, property, requiredFields.contains(fieldName));
                classBuilder.addField(fieldSpec);
            }
        }

        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                .indent("    ")
                .build();

        javaFile.writeTo(outputDir);
    }

    private FieldSpec buildFieldSpec(String fieldName, ApiConfig.Components.SchemaConfig.PropertyConfig property,
                                     boolean isRequired) {
        TypeName fieldType = getFieldType(property);

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE);

        // 添加校验注解
        if (isRequired) {
            if ("string".equals(property.getType())) {
                AnnotationSpec notBlank = AnnotationSpec.builder(
                        ClassName.get("jakarta.validation.constraints", "NotBlank"))
                        .addMember("message", "$S", fieldName + "不能为空")
                        .build();
                fieldBuilder.addAnnotation(notBlank);
            } else {
                AnnotationSpec notNull = AnnotationSpec.builder(
                        ClassName.get("jakarta.validation.constraints", "NotNull"))
                        .addMember("message", "$S", fieldName + "不能为空")
                        .build();
                fieldBuilder.addAnnotation(notNull);
            }
        }

        // 添加@Size注解
        if (property.getMinLength() != null || property.getMaxLength() != null) {
            AnnotationSpec.Builder sizeBuilder = AnnotationSpec.builder(
                    ClassName.get("jakarta.validation.constraints", "Size"));
            if (property.getMinLength() != null) {
                sizeBuilder.addMember("min", "$L", property.getMinLength());
                if (property.getMaxLength() == null) {
                    sizeBuilder.addMember("message", "$S", fieldName + "长度不能少于" + property.getMinLength() + "位");
                }
            }
            if (property.getMaxLength() != null) {
                sizeBuilder.addMember("max", "$L", property.getMaxLength());
                if (property.getMinLength() == null) {
                    sizeBuilder.addMember("message", "$S", fieldName + "长度不能超过" + property.getMaxLength() + "位");
                }
            }
            if (property.getMinLength() != null && property.getMaxLength() != null) {
                sizeBuilder.addMember("message", "$S", fieldName + "长度必须在" + property.getMinLength() + "-" + property.getMaxLength() + "位之间");
            }
            fieldBuilder.addAnnotation(sizeBuilder.build());
        }

        // 添加@Email注解
        if ("email".equals(property.getFormat())) {
            AnnotationSpec email = AnnotationSpec.builder(
                    ClassName.get("jakarta.validation.constraints", "Email"))
                    .addMember("message", "$S", fieldName + "格式不正确")
                    .build();
            fieldBuilder.addAnnotation(email);
        }

        // 添加@Schema注解
        AnnotationSpec schemaAnnotation = AnnotationSpec.builder(
                ClassName.get("io.swagger.v3.oas.annotations.media", "Schema"))
                .addMember("description", "$S", fieldName)
                .build();
        fieldBuilder.addAnnotation(schemaAnnotation);

        return fieldBuilder.build();
    }

    private TypeName getFieldType(ApiConfig.Components.SchemaConfig.PropertyConfig property) {
        String type = property.getType();
        String format = property.getFormat();

        if ("string".equals(type)) {
            return ClassName.get("java.lang", "String");
        }
        if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return ClassName.get("java.lang", "Long");
            }
            return ClassName.get("java.lang", "Integer");
        }
        if ("number".equals(type)) {
            if ("float".equals(format)) {
                return ClassName.get("java.lang", "Float");
            }
            return ClassName.get("java.lang", "Double");
        }
        if ("boolean".equals(type)) {
            return ClassName.get("java.lang", "Boolean");
        }

        return ClassName.get("java.lang", "Object");
    }
}
