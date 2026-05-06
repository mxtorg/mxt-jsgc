package org.demo.maven.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.demo.maven.util.TypeMapper;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DtoGenerator {

    private final OpenAPI openAPI;
    private final Path outputDir;
    private final String basePackage;

    public DtoGenerator(OpenAPI openAPI, File outputDir, String basePackage) {
        this.openAPI = openAPI;
        this.outputDir = outputDir.toPath();
        this.basePackage = basePackage;
    }

    @SuppressWarnings("unchecked")
    public void generate() throws IOException {
        Components components = openAPI.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }
        
        Map<String, Schema> schemas = components.getSchemas();
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            String name = entry.getKey();
            Schema schema = entry.getValue();
            
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnoreProperties"))
                            .addMember("ignoreUnknown", "true")
                            .build());

            Map<String, Schema> properties = schema.getProperties();
            List<String> required = schema.getRequired() != null ? schema.getRequired() : Collections.emptyList();
            
            if (properties != null) {
                for (Map.Entry<String, Schema> prop : properties.entrySet()) {
                    String fieldName = prop.getKey();
                    Schema propSchema = prop.getValue();
                    TypeName fieldType = TypeMapper.toJavaType(propSchema);
                    
                    FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE);
                    addValidationAnnotations(fieldBuilder, propSchema, required.contains(fieldName));
                    classBuilder.addField(fieldBuilder.build());
                    
                    classBuilder.addMethod(createGetter(fieldName, fieldType));
                    classBuilder.addMethod(createSetter(fieldName, fieldType));
                }
            }

            JavaFile javaFile = JavaFile.builder(basePackage + ".dto", classBuilder.build())
                    .build();
            javaFile.writeTo(outputDir);
        }
    }

    private void addValidationAnnotations(FieldSpec.Builder field, Schema schema, boolean isRequired) {
        if (isRequired) {
            field.addAnnotation(NotNull.class);
        }
        
        Integer minLength = schema.getMinLength();
        Integer maxLength = schema.getMaxLength();
        if (minLength != null || maxLength != null) {
            AnnotationSpec.Builder sizeBuilder = AnnotationSpec.builder(Size.class);
            if (minLength != null) {
                sizeBuilder.addMember("min", "$L", minLength);
            }
            if (maxLength != null) {
                sizeBuilder.addMember("max", "$L", maxLength);
            }
            field.addAnnotation(sizeBuilder.build());
        }
        
        if ("email".equals(schema.getFormat())) {
            field.addAnnotation(Email.class);
        }
        
        if (schema.getMinimum() != null) {
            field.addAnnotation(AnnotationSpec.builder(Min.class)
                    .addMember("value", "$L", schema.getMinimum().longValue())
                    .build());
        }
        
        if (schema.getMaximum() != null) {
            field.addAnnotation(AnnotationSpec.builder(Max.class)
                    .addMember("value", "$L", schema.getMaximum().longValue())
                    .build());
        }
        
        if (schema.getPattern() != null) {
            field.addAnnotation(AnnotationSpec.builder(Pattern.class)
                    .addMember("regexp", "$S", schema.getPattern())
                    .build());
        }
    }

    private MethodSpec createGetter(String fieldName, TypeName type) {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return MethodSpec.methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return this.$N", fieldName)
                .build();
    }

    private MethodSpec createSetter(String fieldName, TypeName type) {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }
}