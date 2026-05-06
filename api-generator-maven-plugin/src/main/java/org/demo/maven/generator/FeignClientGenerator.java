package org.demo.maven.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;

import org.demo.maven.util.NamingUtil;
import org.demo.maven.util.TypeMapper;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class FeignClientGenerator {

    private final OpenAPI openAPI;
    private final Path outputDir;
    private final String basePackage;

    public FeignClientGenerator(OpenAPI openAPI, File outputDir, String basePackage) {
        this.openAPI = openAPI;
        this.outputDir = outputDir.toPath();
        this.basePackage = basePackage;
    }

    public void generate() throws IOException {
        String clientName = NamingUtil.capitalize(openAPI.getInfo().getTitle()) + "Client";
        
        TypeSpec.Builder clientBuilder = TypeSpec.interfaceBuilder(clientName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(FeignClient.class)
                        .addMember("name", "$S", openAPI.getInfo().getTitle())
                        .addMember("url", "$S", "${api." + openAPI.getInfo().getTitle() + ".url:}")
                        .build());

        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                
                addMethod(clientBuilder, path, PathItem.HttpMethod.GET, pathItem.getGet());
                addMethod(clientBuilder, path, PathItem.HttpMethod.POST, pathItem.getPost());
                addMethod(clientBuilder, path, PathItem.HttpMethod.PUT, pathItem.getPut());
                addMethod(clientBuilder, path, PathItem.HttpMethod.DELETE, pathItem.getDelete());
            }
        }

        JavaFile javaFile = JavaFile.builder(basePackage + ".client", clientBuilder.build()).build();
        javaFile.writeTo(outputDir);
    }

    private void addMethod(TypeSpec.Builder builder, String path, PathItem.HttpMethod method, Operation op) {
        if (op == null) {
            return;
        }

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(op.getOperationId())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), TypeName.OBJECT));

        AnnotationSpec mappingAnno;
        switch (method) {
            case GET:
                mappingAnno = AnnotationSpec.builder(GetMapping.class).addMember("value", "$S", path).build();
                break;
            case POST:
                mappingAnno = AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", path).build();
                break;
            case PUT:
                mappingAnno = AnnotationSpec.builder(PutMapping.class).addMember("value", "$S", path).build();
                break;
            case DELETE:
                mappingAnno = AnnotationSpec.builder(DeleteMapping.class).addMember("value", "$S", path).build();
                break;
            default:
                mappingAnno = AnnotationSpec.builder(RequestMapping.class)
                        .addMember("value", "$S", path)
                        .addMember("method", "RequestMethod." + method.name())
                        .build();
        }
        methodBuilder.addAnnotation(mappingAnno);

        if (op.getParameters() != null) {
            for (Parameter param : op.getParameters()) {
                TypeName paramType = TypeMapper.toJavaType(param.getSchema());
                if ("path".equals(param.getIn())) {
                    methodBuilder.addParameter(paramType, param.getName())
                            .addAnnotation(AnnotationSpec.builder(PathVariable.class)
                                    .addMember("value", "$S", param.getName()).build());
                } else if ("query".equals(param.getIn())) {
                    methodBuilder.addParameter(paramType, param.getName())
                            .addAnnotation(AnnotationSpec.builder(RequestParam.class)
                                    .addMember("value", "$S", param.getName())
                                    .addMember("required", "$L", param.getRequired() != null && param.getRequired()).build());
                }
            }
        }

        io.swagger.v3.oas.models.parameters.RequestBody requestBody = op.getRequestBody();
        if (requestBody != null && requestBody.getContent() != null) {
            var content = requestBody.getContent().get("application/json");
            if (content != null && content.getSchema() != null) {
                var schema = content.getSchema();
                String dtoClassName = "Object";
                
                java.lang.reflect.Field refField = null;
                try {
                    refField = schema.getClass().getDeclaredField("$ref");
                    refField.setAccessible(true);
                    String ref = (String) refField.get(schema);
                    if (ref != null && ref.startsWith("#/components/schemas/")) {
                        dtoClassName = ref.substring("#/components/schemas/".length());
                    }
                } catch (Exception e) {
                    // ignore
                }
                
                if ("Object".equals(dtoClassName) && openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                    for (String schemaName : openAPI.getComponents().getSchemas().keySet()) {
                        Object schemaObj = openAPI.getComponents().getSchemas().get(schemaName);
                        if (schemaObj == schema || schemaObj.equals(schema)) {
                            dtoClassName = schemaName;
                            break;
                        }
                    }
                }
                TypeName bodyType = ClassName.get(basePackage + ".dto", dtoClassName);
                methodBuilder.addParameter(bodyType, "requestBody")
                        .addAnnotation(RequestBody.class);
            }
        }

        builder.addMethod(methodBuilder.build());
    }
}