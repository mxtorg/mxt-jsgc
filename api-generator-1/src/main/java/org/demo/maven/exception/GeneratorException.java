package org.demo.maven.exception;

/**
 * 代码生成异常基类
 */
public class GeneratorException extends Exception {

    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
