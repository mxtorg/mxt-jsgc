package org.demo.maven.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LockFileManagerTest {

    @TempDir
    Path tempDir;

    private static final String TEST_CONFIG_CONTENT = "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Test API\"}}";
    private static final String TEST_VERSION = "1.0.0";

    @Test
    void testShouldRegenerate_WhenLockFileNotExists() {
        boolean result = LockFileManager.shouldRegenerate(tempDir, TEST_CONFIG_CONTENT, TEST_VERSION);
        assertTrue(result, "Lock文件不存在时应返回true");
    }

    @Test
    void testShouldRegenerate_WhenConfigMd5Matches() throws IOException {
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, Arrays.asList("file1.java"), TEST_VERSION);

        boolean result = LockFileManager.shouldRegenerate(tempDir, TEST_CONFIG_CONTENT, TEST_VERSION);
        assertFalse(result, "配置MD5一致时应返回false");
    }

    @Test
    void testShouldRegenerate_WhenConfigMd5Different() throws IOException {
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, Arrays.asList("file1.java"), TEST_VERSION);

        String modifiedConfig = "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Modified API\"}}";
        boolean result = LockFileManager.shouldRegenerate(tempDir, modifiedConfig, TEST_VERSION);
        assertTrue(result, "配置MD5不同时应返回true");
    }

    @Test
    void testShouldRegenerate_WhenVersionDifferent() throws IOException {
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, Arrays.asList("file1.java"), TEST_VERSION);

        boolean result = LockFileManager.shouldRegenerate(tempDir, TEST_CONFIG_CONTENT, "2.0.0");
        assertTrue(result, "版本不同时应返回true");
    }

    @Test
    void testCreateAndReadLockFile() throws IOException {
        List<String> generatedFiles = Arrays.asList("Controller.java", "Service.java", "Dto.java");
        boolean createResult = LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, generatedFiles, TEST_VERSION);

        assertTrue(createResult, "创建Lock文件应返回true");

        LockFileManager.LockFile lockFile = LockFileManager.readLockFile(tempDir);

        assertNotNull(lockFile, "读取的Lock文件不应为null");
        assertNotNull(lockFile.getGeneratedAt(), "生成时间不应为null");
        assertEquals(TEST_VERSION, lockFile.getGeneratorVersion(), "版本号应一致");
        assertEquals(generatedFiles.size(), lockFile.getGeneratedFiles().size(), "生成文件数量应一致");
        for (int i = 0; i < generatedFiles.size(); i++) {
            assertEquals(generatedFiles.get(i), lockFile.getGeneratedFiles().get(i), "生成文件列表应一致");
        }
    }

    @Test
    void testReadLockFile_WhenNotExists() {
        LockFileManager.LockFile result = LockFileManager.readLockFile(tempDir);
        assertNull(result, "文件不存在时应返回null");
    }

    @Test
    void testCalculateMd5() {
        String md5 = LockFileManager.calculateMd5(TEST_CONFIG_CONTENT);
        assertNotNull(md5, "MD5计算结果不应为null");
        assertEquals(32, md5.length(), "MD5长度应为32位");
        assertEquals(md5, LockFileManager.calculateMd5(TEST_CONFIG_CONTENT), "相同内容的MD5应一致");
    }

    @Test
    void testCalculateMd5_WithNullInput() {
        String md5 = LockFileManager.calculateMd5(null);
        assertNull(md5, "null输入应返回null");
    }

    @Test
    void testCalculateMd5_DifferentContent() {
        String md5_1 = LockFileManager.calculateMd5("content1");
        String md5_2 = LockFileManager.calculateMd5("content2");
        assertNotEquals(md5_1, md5_2, "不同内容的MD5应不同");
    }

    @Test
    void testGetLockFile() {
        File lockFile = LockFileManager.getLockFile(tempDir);
        assertNotNull(lockFile, "Lock文件路径不应为null");
        assertEquals(".api-generator.lock", lockFile.getName(), "Lock文件名应为.api-generator.lock");
    }

    @Test
    void testDeleteLockFile() throws IOException {
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, Arrays.asList("file.java"), TEST_VERSION);
        File lockFile = LockFileManager.getLockFile(tempDir);
        assertTrue(lockFile.exists(), "Lock文件应已创建");

        boolean deleteResult = LockFileManager.deleteLockFile(tempDir);
        assertTrue(deleteResult, "删除Lock文件应返回true");
        assertFalse(lockFile.exists(), "Lock文件应已被删除");
    }

    @Test
    void testDeleteLockFile_WhenNotExists() {
        boolean deleteResult = LockFileManager.deleteLockFile(tempDir);
        assertTrue(deleteResult, "删除不存在的Lock文件应返回true");
    }

    @Test
    void testUpdateLockFile() throws IOException {
        List<String> originalFiles = Arrays.asList("file1.java");
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, originalFiles, TEST_VERSION);

        LockFileManager.LockFile originalLock = LockFileManager.readLockFile(tempDir);
        assertNotNull(originalLock);

        List<String> updatedFiles = Arrays.asList("file1.java", "file2.java", "file3.java");
        LockFileManager.updateLockFile(tempDir, TEST_CONFIG_CONTENT, updatedFiles, TEST_VERSION);

        LockFileManager.LockFile updatedLock = LockFileManager.readLockFile(tempDir);
        assertNotNull(updatedLock);
        assertEquals(updatedFiles.size(), updatedLock.getGeneratedFiles().size(), "更新后的生成文件数量应正确");
    }

    @Test
    void testGetLockFileInfo() throws IOException {
        LockFileManager.createLockFile(tempDir, TEST_CONFIG_CONTENT, Arrays.asList("file1.java", "file2.java"), TEST_VERSION);

        String info = LockFileManager.getLockFileInfo(tempDir);
        assertNotNull(info, "Lock文件信息不应为null");
        assertTrue(info.contains("Lock文件信息"), "信息应包含标题");
        assertTrue(info.contains("生成时间"), "信息应包含生成时间");
        assertTrue(info.contains("配置MD5"), "信息应包含配置MD5");
        assertTrue(info.contains("生成器版本"), "信息应包含生成器版本");
        assertTrue(info.contains("生成文件数量"), "信息应包含文件数量");
    }

    @Test
    void testGetLockFileInfo_WhenNotExists() {
        String info = LockFileManager.getLockFileInfo(tempDir);
        assertNull(info, "文件不存在时应返回null");
    }

    @Test
    void testCalculateFileMd5() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content", StandardCharsets.UTF_8);

        String md5 = LockFileManager.calculateFileMd5(testFile.toFile());
        assertNotNull(md5, "文件MD5计算结果不应为null");
        assertEquals(32, md5.length(), "MD5长度应为32位");
    }

    @Test
    void testCalculateFileMd5_WhenFileNotExists() {
        File nonExistentFile = tempDir.resolve("non-existent.txt").toFile();
        String md5 = LockFileManager.calculateFileMd5(nonExistentFile);
        assertNull(md5, "不存在的文件应返回null");
    }

    @Test
    void testCalculateFileMd5_WithNullFile() {
        String md5 = LockFileManager.calculateFileMd5(null);
        assertNull(md5, "null文件应返回null");
    }
}