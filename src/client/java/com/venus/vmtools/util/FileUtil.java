package com.venus.vmtools.util;

import com.venus.vmtools.VMToolsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件工具类
 */
public class FileUtil {

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("vmtools");

    /**
     * 确保配置目录存在
     */
    public static void ensureConfigDir() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("创建配置目录失败", e);
        }
    }

    /**
     * 获取配置目录路径
     */
    public static Path getConfigDir() {
        ensureConfigDir();
        return CONFIG_DIR;
    }

    /**
     * 获取导出目录路径
     */
    public static Path getExportDir() {
        Path exportDir = CONFIG_DIR.resolve("exports");
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("创建导出目录失败", e);
        }
        return exportDir;
    }

    /**
     * 生成带时间戳的文件名
     */
    public static String generateTimestampFilename(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", prefix, timestamp, extension);
    }

    /**
     * 备份文件
     */
    public static boolean backupFile(Path source) {
        if (!Files.exists(source)) {
            return false;
        }

        try {
            Path backupDir = CONFIG_DIR.resolve("backups");
            Files.createDirectories(backupDir);

            String filename = source.getFileName().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = filename.replace(".json", "_" + timestamp + ".json");

            Path backupPath = backupDir.resolve(backupName);
            Files.copy(source, backupPath, StandardCopyOption.REPLACE_EXISTING);

            VMToolsClient.LOGGER.info("已备份文件到: {}", backupPath);
            return true;
        } catch (IOException e) {
            VMToolsClient.LOGGER.error("备份文件失败", e);
            return false;
        }
    }

    /**
     * 检查文件是否可读
     */
    public static boolean isReadable(Path path) {
        return Files.exists(path) && Files.isReadable(path);
    }

    /**
     * 检查文件是否可写
     */
    public static boolean isWritable(Path path) {
        if (Files.exists(path)) {
            return Files.isWritable(path);
        }
        // 检查父目录是否可写
        Path parent = path.getParent();
        return parent != null && Files.isWritable(parent);
    }
}
