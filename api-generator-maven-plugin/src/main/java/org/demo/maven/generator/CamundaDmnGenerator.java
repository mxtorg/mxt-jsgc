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

public class CamundaDmnGenerator {

    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        StringBuilder dmn = new StringBuilder();
        
        dmn.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        dmn.append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\"\n");
        dmn.append("             xmlns:dmndi=\"https://www.omg.org/spec/DMN/20191111/DMNDI/\"\n");
        dmn.append("             xmlns:dc=\"http://www.omg.org/spec/DMN/20191111/DC/\"\n");
        dmn.append("             id=\"api-model\"\n");
        dmn.append("             name=\"API Model\">\n");

        Components components = openAPI.getComponents();
        if (components != null && components.getSchemas() != null) {
            for (Map.Entry<String, Schema> entry : components.getSchemas().entrySet()) {
                String schemaName = entry.getKey();
                dmn.append("  <itemDefinition id=\"").append(schemaName).append("\">\n");
                dmn.append("    <structureRef>").append(basePackage).append(".dto.").append(schemaName).append("</structureRef>\n");
                dmn.append("  </itemDefinition>\n");
            }
        }

        dmn.append("</definitions>");

        Path target = outputDir.toPath().resolve("camunda/api-model.dmn");
        Files.createDirectories(target.getParent());
        Files.write(target, dmn.toString().getBytes(StandardCharsets.UTF_8));
    }
}