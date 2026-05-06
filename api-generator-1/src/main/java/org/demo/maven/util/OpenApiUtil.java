package org.demo.maven.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.maven.model.ApiConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI/JSON Schema解析工具类
 */
public class OpenApiUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private OpenApiUtil() {
        // 工具类禁止实例化
    }

    /**
     * 解析JSON Schema文件
     *
     * @param file JSON文件
     * @return ApiConfig对象
     * @throws IOException 解析异常
     */
    public static ApiConfig parse(File file) throws IOException {
        return objectMapper.readValue(file, ApiConfig.class);
    }

    /**
     * 验证配置文件
     *
     * @param config ApiConfig对象
     * @return 错误信息列表
     */
    public static List<String> validate(ApiConfig config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("配置不能为空");
            return errors;
        }

        // 验证info
        if (config.getInfo() == null) {
            errors.add("info节点不能为空");
        } else {
            if (config.getInfo().getTitle() == null || config.getInfo().getTitle().isEmpty()) {
                errors.add("info.title不能为空");
            }
            if (config.getInfo().getDescription() == null || config.getInfo().getDescription().isEmpty()) {
                errors.add("info.description不能为空");
            }
            if (config.getInfo().getVersion() == null || config.getInfo().getVersion().isEmpty()) {
                errors.add("info.version不能为空");
            }
        }

        // 验证git
        if (config.getGit() == null) {
            errors.add("git节点不能为空");
        } else {
            if (config.getGit().getUrl() == null || config.getGit().getUrl().isEmpty()) {
                errors.add("git.url不能为空");
            }
            if (config.getGit().getBranch() == null || config.getGit().getBranch().isEmpty()) {
                errors.add("git.branch不能为空");
            }
            if (config.getGit().getToken() == null || config.getGit().getToken().isEmpty()) {
                errors.add("git.token不能为空");
            }
        }

        // 验证gav
        if (config.getGav() == null) {
            errors.add("gav节点不能为空");
        } else {
            if (config.getGav().getGav() == null || config.getGav().getGav().isEmpty()) {
                errors.add("gav.gav不能为空");
            }
            if (config.getGav().getPkg() == null || config.getGav().getPkg().isEmpty()) {
                errors.add("gav.pkg不能为空");
            }
            if (config.getGav().getRepository() == null || config.getGav().getRepository().length == 0) {
                errors.add("gav.repository不能为空");
            }
        }

        // 验证paths
        if (config.getPaths() == null || config.getPaths().isEmpty()) {
            errors.add("paths节点不能为空");
        }

        // 验证components
        if (config.getComponents() == null) {
            errors.add("components节点不能为空");
        }

        return errors;
    }

    /**
     * 从字符串解析
     *
     * @param json JSON字符串
     * @return ApiConfig对象
     * @throws IOException 解析异常
     */
    public static ApiConfig parse(String json) throws IOException {
        return objectMapper.readValue(json, ApiConfig.class);
    }
}
