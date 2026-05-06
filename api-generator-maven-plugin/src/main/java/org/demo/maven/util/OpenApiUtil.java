package org.demo.maven.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.File;
import java.io.IOException;

public class OpenApiUtil {
    
    public static OpenAPI parseOpenAPI(File specFile) throws IOException {
        if (!specFile.exists()) {
            throw new IOException("API spec file not found: " + specFile.getAbsolutePath());
        }
        
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        
        return new OpenAPIV3Parser().read(specFile.getAbsolutePath(), null, options);
    }
    
    public static String getExtensionValue(OpenAPI openAPI, String extensionName) {
        if (openAPI.getExtensions() != null && openAPI.getExtensions().containsKey(extensionName)) {
            Object value = openAPI.getExtensions().get(extensionName);
            return value != null ? value.toString() : null;
        }
        return null;
    }
    
    public static String getGav(OpenAPI openAPI) {
        return getExtensionValue(openAPI, "x-gav");
    }
    
    public static String getRepository(OpenAPI openAPI) {
        return getExtensionValue(openAPI, "x-repository");
    }
    
    public static String getPackage(OpenAPI openAPI) {
        return getExtensionValue(openAPI, "x-pkg");
    }
}