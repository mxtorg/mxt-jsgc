package org.demo.maven.generator;

import io.swagger.v3.oas.models.OpenAPI;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SwaggerUIConfigGenerator {

    public void generate(OpenAPI openAPI, File outputDir) throws IOException {
        String yaml = 
            "springdoc:\n" +
            "  api-docs:\n" +
            "    path: /v3/api-docs\n" +
            "  swagger-ui:\n" +
            "    path: /swagger-ui.html\n" +
            "    enabled: true\n";
        
        Path target = outputDir.toPath().resolve("application-swagger.yml");
        Files.write(target, yaml.getBytes(StandardCharsets.UTF_8));
    }
}