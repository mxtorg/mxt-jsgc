package org.demo.maven.generator;

import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;

import java.io.File;

/**
 * 代码生成器接口
 */
public interface CodeGenerator {

    /**
     * 获取生成器名称
     *
     * @return 生成器名称
     */
    String getName();

    /**
     * 获取执行顺序（数值越小越先执行）
     *
     * @return 执行顺序
     */
    int getOrder();

    /**
     * 执行代码生成
     *
     * @param config    API配置
     * @param outputDir 输出目录
     * @throws GeneratorException 生成异常
     */
    void generate(ApiConfig config, File outputDir) throws GeneratorException;
}
