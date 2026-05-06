package org.demo.maven.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MarkdownDocGenerator {

    public void generate(OpenAPI openAPI, File outputDir) throws IOException {
        StringBuilder md = new StringBuilder();
        
        md.append("# ").append(openAPI.getInfo().getTitle()).append("\n\n");
        md.append("## Description\n");
        md.append(openAPI.getInfo().getDescription() != null ? openAPI.getInfo().getDescription() : "").append("\n\n");
        
        md.append("## Version\n");
        md.append(openAPI.getInfo().getVersion()).append("\n\n");
        
        md.append("## Endpoints\n\n");
        md.append("| HTTP Method | Path | Operation ID | Description |\n");
        md.append("|-------------|------|--------------|-------------|\n");
        
        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                
                appendOperation(md, path, "GET", pathItem.getGet());
                appendOperation(md, path, "POST", pathItem.getPost());
                appendOperation(md, path, "PUT", pathItem.getPut());
                appendOperation(md, path, "DELETE", pathItem.getDelete());
            }
        }
        
        md.append("\n## Schemas\n\n");
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            for (String schemaName : openAPI.getComponents().getSchemas().keySet()) {
                md.append("- ").append(schemaName).append("\n");
            }
        }
        
        Path target = outputDir.toPath().resolve("api-docs.md");
        Files.write(target, md.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendOperation(StringBuilder md, String path, String method, Operation op) {
        if (op != null) {
            md.append("| ").append(method)
              .append(" | ").append(path)
              .append(" | ").append(op.getOperationId())
              .append(" | ").append(op.getDescription() != null ? op.getDescription() : "")
              .append(" |\n");
        }
    }
}