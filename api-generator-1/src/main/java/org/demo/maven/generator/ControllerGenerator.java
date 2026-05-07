package org.demo.maven.generator;

import com.squareup.javapoet.*;
import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;
import org.demo.maven.util.NamingUtil;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Controller类生成器
 */
public class ControllerGenerator implements CodeGenerator {

    @Override
    public String getName() {
        return "controller-generator";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public void generate(ApiConfig config, File outputDir) throws GeneratorException {
        if (config.getPaths() == null || config.getPaths().isEmpty()) {
            return;
        }

        String basePackage = config.getGav().getPkg();
        String controllerPackage = basePackage + ".controller";
        String dtoPackage = basePackage + ".dto";

        // 按Endpoint类分组
        Map<String, List<PathOperation>> endpointOperations = groupByEndpoint(config.getPaths());

        // 生成每个Controller类
        for (Map.Entry<String, List<PathOperation>> entry : endpointOperations.entrySet()) {
            String endpointClassName = entry.getKey();
            List<PathOperation> operations = entry.getValue();

            try {
                // 获取Endpoint类的描述
                String description = getEndpointDescription(config, endpointClassName);
                generateControllerClass(endpointClassName, description, operations, controllerPackage, dtoPackage, outputDir);
            } catch (IOException e) {
                throw new GeneratorException("生成Controller类失败: " + endpointClassName, e);
            }
        }
    }

    private Map<String, List<PathOperation>> groupByEndpoint(Map<String, ApiConfig.PathConfig> paths) {
        Map<String, List<PathOperation>> result = new HashMap<>();

        for (Map.Entry<String, ApiConfig.PathConfig> pathEntry : paths.entrySet()) {
            String path = pathEntry.getKey();
            ApiConfig.PathConfig pathConfig = pathEntry.getValue();
            String schemaRef = pathConfig.getSchema() != null ? pathConfig.getSchema().get$ref() : null;
            String endpointClassName = schemaRef != null ? NamingUtil.refToClassName(schemaRef) : "DefaultEndpoint";

            List<PathOperation> operations = result.computeIfAbsent(endpointClassName, k -> new ArrayList<>());

            // 添加各HTTP方法
            if (pathConfig.getPost() != null) {
                operations.add(new PathOperation(path, "post", pathConfig.getPost()));
            }
            if (pathConfig.getPut() != null) {
                operations.add(new PathOperation(path, "put", pathConfig.getPut()));
            }
            if (pathConfig.getGet() != null) {
                operations.add(new PathOperation(path, "get", pathConfig.getGet()));
            }
            if (pathConfig.getDelete() != null) {
                operations.add(new PathOperation(path, "delete", pathConfig.getDelete()));
            }
        }

        return result;
    }

    private String getEndpointDescription(ApiConfig config, String endpointClassName) {
        if (config.getComponents() != null && config.getComponents().getSchemas() != null) {
            ApiConfig.Components.SchemaConfig schema = config.getComponents().getSchemas().get(endpointClassName);
            if (schema != null && schema.getDescription() != null) {
                return schema.getDescription();
            }
        }
        return endpointClassName + " Endpoint";
    }

    private void generateControllerClass(String className, String description, List<PathOperation> operations,
                                         String controllerPackage, String dtoPackage, File outputDir) throws IOException {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        // 添加类注解
        classBuilder.addAnnotation(AnnotationSpec.builder(
                ClassName.get("org.springframework.web.bind.annotation", "RestController")).build());
        classBuilder.addAnnotation(AnnotationSpec.builder(
                ClassName.get("lombok.extern.slf4j", "Slf4j")).build());

        AnnotationSpec tagAnnotation = AnnotationSpec.builder(
                ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag"))
                .addMember("name", "$S", description)
                .build();
        classBuilder.addAnnotation(tagAnnotation);

        // 添加方法
        for (PathOperation operation : operations) {
            MethodSpec methodSpec = buildMethodSpec(operation, dtoPackage);
            classBuilder.addMethod(methodSpec);
        }

        JavaFile javaFile = JavaFile.builder(controllerPackage, classBuilder.build())
                .indent("    ")
                .build();

        javaFile.writeTo(outputDir);
    }

    private MethodSpec buildMethodSpec(PathOperation operation, String dtoPackage) {
        String httpMethod = operation.httpMethod;
        String path = operation.path;
        ApiConfig.OperationConfig operationConfig = operation.config;

        String methodName = NamingUtil.pathToMethodName(httpMethod, path);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC);

        // 添加映射注解
        ClassName mappingAnnotation = getMappingAnnotation(httpMethod);
        AnnotationSpec.Builder mappingBuilder = AnnotationSpec.builder(mappingAnnotation)
                .addMember("value", "$S", path);
        methodBuilder.addAnnotation(mappingBuilder.build());

        // 添加@Operation注解
        if (operationConfig.getSummary() != null && !operationConfig.getSummary().isEmpty()) {
            AnnotationSpec operationAnnotation = AnnotationSpec.builder(
                    ClassName.get("io.swagger.v3.oas.annotations", "Operation"))
                    .addMember("summary", "$S", operationConfig.getSummary())
                    .build();
            methodBuilder.addAnnotation(operationAnnotation);
        }

        // 构建参数
        StringBuilder paramNames = new StringBuilder();
        if (operationConfig.getRequest() != null) {
            // POST/PUT请求体
            ApiConfig.RequestConfig request = operationConfig.getRequest();
            if (request.getContent() != null && request.getContent().getApplicationJson() != null
                    && request.getContent().getApplicationJson().getSchema() != null) {
                String schemaRef = request.getContent().getApplicationJson().getSchema().get$ref();
                String requestDtoName = NamingUtil.refToClassName(schemaRef);
                ClassName requestType = ClassName.get(dtoPackage, requestDtoName);
                
                ParameterSpec paramSpec = ParameterSpec.builder(requestType, "request")
                        .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build();
                methodBuilder.addParameter(paramSpec);
                paramNames.append("request");
            }
        } else if (operationConfig.getParameters() != null) {
            // GET请求参数
            boolean first = true;
            for (ApiConfig.ParameterConfig param : operationConfig.getParameters()) {
                if (!first) {
                    paramNames.append(", ");
                }
                first = false;

                TypeName paramType = getParameterType(param);
                ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, param.getName())
                        .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "RequestParam"))
                                .addMember("value", "$S", param.getName())
                                .build());
                methodBuilder.addParameter(paramBuilder.build());
                paramNames.append(param.getName());
            }
        }

        // 设置返回类型
        TypeName returnType = getReturnType(operationConfig, dtoPackage);
        methodBuilder.returns(returnType);

        // 添加方法体
        String summary = operationConfig.getSummary() != null ? operationConfig.getSummary() : "处理请求";
        methodBuilder.addComment("todo 实现业务逻辑")
                //.addStatement("log.info($S, $L)", summary + "，入参: {}", paramNames.toString())
                .addStatement("return $L", getDefaultValue(returnType));

        return methodBuilder.build();
    }

    private ClassName getMappingAnnotation(String httpMethod) {
        switch (httpMethod.toLowerCase()) {
            case "post":
                return ClassName.get("org.springframework.web.bind.annotation", "PostMapping");
            case "put":
                return ClassName.get("org.springframework.web.bind.annotation", "PutMapping");
            case "get":
                return ClassName.get("org.springframework.web.bind.annotation", "GetMapping");
            case "delete":
                return ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping");
            default:
                return ClassName.get("org.springframework.web.bind.annotation", "RequestMapping");
        }
    }

    private TypeName getParameterType(ApiConfig.ParameterConfig param) {
        if (param.getSchema() == null) {
            return ClassName.get("java.lang", "String");
        }
        String type = param.getSchema().getType();
        String format = param.getSchema().getFormat();

        if ("string".equals(type)) {
            return ClassName.get("java.lang", "String");
        }
        if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return ClassName.get("java.lang", "Long");
            }
            return ClassName.get("java.lang", "Integer");
        }
        if ("boolean".equals(type)) {
            return ClassName.get("java.lang", "Boolean");
        }
        return ClassName.get("java.lang", "String");
    }

    private TypeName getReturnType(ApiConfig.OperationConfig operationConfig, String dtoPackage) {
        if (operationConfig.getResponses() != null) {
            for (Map.Entry<String, ApiConfig.ResponseConfig> responseEntry : operationConfig.getResponses().entrySet()) {
                ApiConfig.ResponseConfig response = responseEntry.getValue();
                if (response.getContent() != null && response.getContent().getApplicationJson() != null) {
                    // 优先看schema
                    if (response.getContent().getApplicationJson().getSchema() != null) {
                        String schemaRef = response.getContent().getApplicationJson().getSchema().get$ref();
                        if (schemaRef != null) {
                            String responseDtoName = NamingUtil.refToClassName(schemaRef);
                            return ClassName.get(dtoPackage, responseDtoName);
                        }
                    }
                    // 其次看parameters
                    if (response.getContent().getApplicationJson().getParameters() != null
                            && response.getContent().getApplicationJson().getParameters().length > 0) {
                        ApiConfig.ParameterConfig param = response.getContent().getApplicationJson().getParameters()[0];
                        return getParameterType(param);
                    }
                }
            }
        }
        return ClassName.get("java.lang", "Void");
    }

    private String getDefaultValue(TypeName returnType) {
        String typeName = returnType.toString();
        if (typeName.contains("Long")) {
            return "1L";
        }
        if (typeName.contains("Integer") || typeName.contains("int")) {
            return "1";
        }
        if (typeName.contains("Boolean") || typeName.contains("boolean")) {
            return "true";
        }
        if (typeName.contains("String")) {
            return "\"\"";
        }
        if (typeName.contains("Void")) {
            return "";
        }
        // 对象类型
        if (returnType instanceof ClassName) {
            ClassName className = (ClassName) returnType;
            return "new " + className.simpleName() + "()";
        }
        return "null";
    }

    private static class PathOperation {
        final String path;
        final String httpMethod;
        final ApiConfig.OperationConfig config;

        PathOperation(String path, String httpMethod, ApiConfig.OperationConfig config) {
            this.path = path;
            this.httpMethod = httpMethod;
            this.config = config;
        }
    }
}
