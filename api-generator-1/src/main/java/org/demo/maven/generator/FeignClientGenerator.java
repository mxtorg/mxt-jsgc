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
 * Feign客户端生成器
 */
public class FeignClientGenerator implements CodeGenerator {

    @Override
    public String getName() {
        return "feign-generator";
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void generate(ApiConfig config, File outputDir) throws GeneratorException {
        if (config.getPaths() == null || config.getPaths().isEmpty()) {
            return;
        }

        String basePackage = config.getGav().getPkg();
        String feignPackage = basePackage + ".feign";
        String dtoPackage = basePackage + ".dto";
        String serviceName = config.getInfo().getTitle();

        // 按Endpoint类分组
        Map<String, List<PathOperation>> endpointOperations = groupByEndpoint(config.getPaths());

        // 为每个Endpoint生成Feign Client（简化版：实际可能一个服务一个Client）
        // 这里按照doc.md的示例，生成一个与info.title对应的Client
        try {
            List<PathOperation> allOperations = new ArrayList<>();
            for (List<PathOperation> ops : endpointOperations.values()) {
                allOperations.addAll(ops);
            }
            generateFeignClientClass(serviceName, allOperations, feignPackage, dtoPackage, outputDir);
        } catch (IOException e) {
            throw new GeneratorException("生成Feign Client类失败", e);
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

    private void generateFeignClientClass(String serviceName, List<PathOperation> operations,
                                          String feignPackage, String dtoPackage, File outputDir) throws IOException {
        String clientClassName = serviceName + "Client";

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(clientClassName)
                .addModifiers(Modifier.PUBLIC);

        // 添加@FeignClient注解
        AnnotationSpec feignClientAnnotation = AnnotationSpec.builder(
                ClassName.get("org.springframework.cloud.openfeign", "FeignClient"))
                .addMember("name", "$S", serviceName)
                .addMember("url", "$S", "${api." + serviceName + ".url:}")
                .build();
        interfaceBuilder.addAnnotation(feignClientAnnotation);

        // 添加方法
        for (PathOperation operation : operations) {
            MethodSpec methodSpec = buildMethodSpec(operation, dtoPackage);
            interfaceBuilder.addMethod(methodSpec);
        }

        JavaFile javaFile = JavaFile.builder(feignPackage, interfaceBuilder.build())
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
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

        // 添加映射注解
        ClassName mappingAnnotation = getMappingAnnotation(httpMethod);
        AnnotationSpec.Builder mappingBuilder = AnnotationSpec.builder(mappingAnnotation)
                .addMember("value", "$S", path);
        methodBuilder.addAnnotation(mappingBuilder.build());

        // 构建参数
        if (operationConfig.getRequest() != null) {
            ApiConfig.RequestConfig request = operationConfig.getRequest();
            if (request.getContent() != null && request.getContent().getApplicationJson() != null
                    && request.getContent().getApplicationJson().getSchema() != null) {
                String schemaRef = request.getContent().getApplicationJson().getSchema().get$ref();
                String requestDtoName = NamingUtil.refToClassName(schemaRef);
                ClassName requestType = ClassName.get(dtoPackage, requestDtoName);

                ParameterSpec paramSpec = ParameterSpec.builder(requestType, "requestBody")
                        .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "RequestBody")).build())
                        .build();
                methodBuilder.addParameter(paramSpec);
            }
        } else if (operationConfig.getParameters() != null) {
            for (ApiConfig.ParameterConfig param : operationConfig.getParameters()) {
                TypeName paramType = getParameterType(param);
                ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, param.getName())
                        .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.web.bind.annotation", "RequestParam"))
                                .addMember("value", "$S", param.getName())
                                .build());
                methodBuilder.addParameter(paramBuilder.build());
            }
        }

        // 设置返回类型
        TypeName returnType = getReturnType(operationConfig, dtoPackage);
        methodBuilder.returns(returnType);

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
                    if (response.getContent().getApplicationJson().getSchema() != null) {
                        String schemaRef = response.getContent().getApplicationJson().getSchema().get$ref();
                        if (schemaRef != null) {
                            String responseDtoName = NamingUtil.refToClassName(schemaRef);
                            return ClassName.get(dtoPackage, responseDtoName);
                        }
                    }
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
