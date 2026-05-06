package org.demo.maven.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DroolsRuleGenerator {

    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        StringBuilder drl = new StringBuilder();
        
        drl.append("package org.demo.rules\n\n");
        drl.append("import ").append(basePackage).append(".dto.*;\n\n");

        Components components = openAPI.getComponents();
        if (components != null && components.getSchemas() != null) {
            for (Map.Entry<String, Schema> entry : components.getSchemas().entrySet()) {
                String schemaName = entry.getKey();
                drl.append("rule \"Validate").append(schemaName).append("\"\n");
                drl.append("  when\n");
                drl.append("    $r : ").append(schemaName).append("()\n");
                drl.append("  then\n");
                drl.append("    System.out.println(\"Valid ").append(schemaName).append("\");\n");
                drl.append("end\n\n");
            }
        }

        Path target = outputDir.toPath().resolve("drools/api-rules.drl");
        Files.createDirectories(target.getParent());
        Files.write(target, drl.toString().getBytes(StandardCharsets.UTF_8));
    }
}