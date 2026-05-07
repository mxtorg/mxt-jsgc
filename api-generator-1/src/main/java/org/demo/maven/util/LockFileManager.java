package org.demo.maven.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lock文件管理器
 * 负责管理API生成器的Lock文件，用于跟踪生成状态和判断是否需要重新生成
 */
public class LockFileManager {

    private static final String LOCK_FILE_NAME = ".api-generator.lock";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private LockFileManager() {
        // 工具类禁止实例化
    }

    /**
     * Lock文件数据结构
     */
    public static class LockFile {
        @JsonProperty("generatedAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private ZonedDateTime generatedAt;

        @JsonProperty("configMd5")
        private String configMd5;

        @JsonProperty("generatedFiles")
        private List<String> generatedFiles;

        @JsonProperty("generatorVersion")
        private String generatorVersion;

        public LockFile() {
            this.generatedFiles = new ArrayList<>();
        }

        public ZonedDateTime getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(ZonedDateTime generatedAt) {
            this.generatedAt = generatedAt;
        }

        public String getConfigMd5() {
            return configMd5;
        }

        public void setConfigMd5(String configMd5) {
            this.configMd5 = configMd5;
        }

        public List<String> getGeneratedFiles() {
            return generatedFiles;
        }

        public void setGeneratedFiles(List<String> generatedFiles) {
            this.generatedFiles = generatedFiles;
        }

        public String getGeneratorVersion() {
            return generatorVersion;
        }

        public void setGeneratorVersion(String generatorVersion) {
            this.generatorVersion = generatorVersion;
        }
    }

    /**
     * 获取Lock文件路径
     *
     * @param projectBasedir 项目根目录
     * @return Lock文件的File对象
     */
    public static File getLockFile(Path projectBasedir) {
        return projectBasedir.resolve(LOCK_FILE_NAME).toFile();
    }

    /**
     * 读取现有的Lock文件
     *
     * @param projectBasedir 项目根目录
     * @return LockFile对象，如果文件不存在或读取失败则返回null
     */
    public static LockFile readLockFile(Path projectBasedir) {
        File lockFile = getLockFile(projectBasedir);
        if (!lockFile.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(lockFile)) {
            return OBJECT_MAPPER.readValue(fis, LockFile.class);
        } catch (IOException e) {
            System.err.println("[LockFileManager] 读取Lock文件失败: " + lockFile.getAbsolutePath() + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 创建新的Lock文件
     *
     * @param projectBasedir    项目根目录
     * @param configContent     配置文件内容
     * @param generatedFiles    生成的文件的相对路径列表
     * @param generatorVersion  生成器版本号
     * @return true表示创建成功，false表示失败
     */
    public static boolean createLockFile(Path projectBasedir, String configContent,
                                         List<String> generatedFiles, String generatorVersion) {
        File lockFile = getLockFile(projectBasedir);

        LockFile lock = new LockFile();
        lock.setGeneratedAt(ZonedDateTime.now());
        lock.setConfigMd5(calculateMd5(configContent));
        lock.setGeneratedFiles(new ArrayList<>(generatedFiles));
        lock.setGeneratorVersion(generatorVersion);

        try {
            String json = OBJECT_MAPPER.writeValueAsString(lock);
            Files.writeString(lockFile.toPath(), json, StandardCharsets.UTF_8);
            System.out.println("[LockFileManager] Lock文件已创建: " + lockFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("[LockFileManager] 创建Lock文件失败: " + lockFile.getAbsolutePath() + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 更新Lock文件
     *
     * @param projectBasedir    项目根目录
     * @param configContent     配置文件内容
     * @param generatedFiles    生成的文件的相对路径列表
     * @param generatorVersion  生成器版本号
     * @return true表示更新成功，false表示失败
     */
    public static boolean updateLockFile(Path projectBasedir, String configContent,
                                        List<String> generatedFiles, String generatorVersion) {
        return createLockFile(projectBasedir, configContent, generatedFiles, generatorVersion);
    }

    /**
     * 判断是否需要重新生成
     * 需要重新生成的条件：
     * 1. Lock文件不存在
     * 2. 配置文件内容发生变化（MD5不同）
     * 3. 生成器版本发生变化
     *
     * @param projectBasedir    项目根目录
     * @param configContent     当前配置文件内容
     * @param generatorVersion 当前生成器版本号
     * @return true表示需要重新生成，false表示不需要
     */
    public static boolean shouldRegenerate(Path projectBasedir, String configContent, String generatorVersion) {
        File lockFile = getLockFile(projectBasedir);

        if (!lockFile.exists()) {
            System.out.println("[LockFileManager] Lock文件不存在，需要重新生成");
            return true;
        }

        LockFile existingLock = readLockFile(projectBasedir);
        if (existingLock == null) {
            System.out.println("[LockFileManager] Lock文件读取失败，需要重新生成");
            return true;
        }

        String currentMd5 = calculateMd5(configContent);
        if (!currentMd5.equals(existingLock.getConfigMd5())) {
            System.out.println("[LockFileManager] 配置文件已变更，需要重新生成");
            System.out.println("[LockFileManager] 原MD5: " + existingLock.getConfigMd5() + ", 当前MD5: " + currentMd5);
            return true;
        }

        if (!generatorVersion.equals(existingLock.getGeneratorVersion())) {
            System.out.println("[LockFileManager] 生成器版本已变更，需要重新生成");
            System.out.println("[LockFileManager] 原版本: " + existingLock.getGeneratorVersion() + ", 当前版本: " + generatorVersion);
            return true;
        }

        System.out.println("[LockFileManager] 配置无变化，无需重新生成");
        return false;
    }

    /**
     * 计算字符串的MD5哈希值
     *
     * @param content 输入字符串
     * @return 32位MD5十六进制字符串，如果输入为null则返回null
     */
    public static String calculateMd5(String content) {
        if (content == null) {
            return null;
        }
        return DigestUtils.md5Hex(content);
    }

    /**
     * 计算文件的MD5哈希值
     *
     * @param file 输入文件
     * @return 32位MD5十六进制字符串，如果文件不存在或读取失败则返回null
     */
    public static String calculateFileMd5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fis);
        } catch (IOException e) {
            System.err.println("[LockFileManager] 计算文件MD5失败: " + file.getAbsolutePath() + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 删除Lock文件
     *
     * @param projectBasedir 项目根目录
     * @return true表示删除成功或文件不存在，false表示删除失败
     */
    public static boolean deleteLockFile(Path projectBasedir) {
        File lockFile = getLockFile(projectBasedir);
        if (!lockFile.exists()) {
            return true;
        }

        try {
            Files.delete(lockFile.toPath());
            System.out.println("[LockFileManager] Lock文件已删除: " + lockFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("[LockFileManager] 删除Lock文件失败: " + lockFile.getAbsolutePath() + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取Lock文件信息（友好格式）
     *
     * @param projectBasedir 项目根目录
     * @return Lock文件信息字符串，如果文件不存在则返回null
     */
    public static String getLockFileInfo(Path projectBasedir) {
        LockFile lock = readLockFile(projectBasedir);
        if (lock == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Lock文件信息:\n");
        sb.append("  生成时间: ").append(lock.getGeneratedAt() != null
            ? lock.getGeneratedAt().format(DateTimeFormatter.ISO_ZONED_DATE_TIME) : "未知").append("\n");
        sb.append("  配置MD5: ").append(lock.getConfigMd5()).append("\n");
        sb.append("  生成器版本: ").append(lock.getGeneratorVersion()).append("\n");
        sb.append("  生成文件数量: ").append(lock.getGeneratedFiles() != null ? lock.getGeneratedFiles().size() : 0).append("\n");

        if (lock.getGeneratedFiles() != null && !lock.getGeneratedFiles().isEmpty()) {
            sb.append("  生成的文件列表:\n");
            for (String file : lock.getGeneratedFiles()) {
                sb.append("    - ").append(file).append("\n");
            }
        }

        return sb.toString();
    }
}
