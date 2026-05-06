package org.demo.maven.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.swagger.v3.oas.models.OpenAPI;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WireMockStubGenerator {

    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        TypeSpec.Builder stubClass = TypeSpec.classBuilder("WireMockStubs")
                .addModifiers(Modifier.PUBLIC);

        MethodSpec setup = MethodSpec.methodBuilder("setupStubs")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("// TODO: implement stubs per path")
                .build();

        stubClass.addMethod(setup);

        JavaFile.builder(basePackage + ".mock", stubClass.build()).build().writeTo(outputDir.toPath());
    }
}