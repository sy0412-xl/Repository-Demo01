import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;

public class Vtt {

    public static void main(String[] args) {
        // 直接在代码中设置要处理的根目录路径
        String rootPath = "D:/111game/vtt/";  // 修改为您的实际路径

        Path rootDir = Paths.get(rootPath);
        if (!Files.isDirectory(rootDir)) {
            System.out.println("错误: 路径不是目录或不存在");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            int convertedCount = convertVttToLrc(rootDir);
            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n转换完成!");
            System.out.println("已转换文件数: " + convertedCount);
            System.out.printf("耗时: %.2f 秒%n", duration / 1000.0);
        } catch (IOException e) {
            System.err.println("转换出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int convertVttToLrc(Path dir) throws IOException {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    // 递归处理子目录并累加计数
                    count += convertVttToLrc(path);
                } else if (isVttFile(path)) {
                    // 处理VTT文件
                    if (convertFile(path)) {
                        count++;
                        System.out.println("已转换: " + path.getFileName());
                    }
                }
            }
        }
        return count;
    }

    private static boolean isVttFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".wav.vtt") || fileName.endsWith(".vtt");
    }

    private static boolean convertFile(Path vttPath) throws IOException {
        // 生成新的LRC文件名
        String originalName = vttPath.getFileName().toString();
        String baseName = originalName.replaceAll("(?i)\\.wav\\.vtt$", "")
                .replaceAll("(?i)\\.vtt$", "");
        Path lrcPath = vttPath.resolveSibling(baseName + ".lrc");

        // 转换文件内容
        try (BufferedReader reader = Files.newBufferedReader(vttPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(lrcPath, StandardCharsets.UTF_8)) {

            Pattern timePattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // 跳过文件头、空行和注释
                if (firstLine && (line.startsWith("WEBVTT") || line.startsWith("NOTE"))) {
                    firstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                // 处理时间行
                if (line.contains("-->")) {
                    Matcher matcher = timePattern.matcher(line);
                    if (matcher.find()) {
                        // 转换时间格式
                        String convertedTime = convertTime(matcher.group(1));
                        writer.write(convertedTime);

                        // 写入字幕文本（下一行）
                        String text = reader.readLine();
                        if (text != null && !text.trim().isEmpty()) {
                            writer.write(text);
                        }
                        writer.newLine();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("转换文件出错: " + vttPath);
            e.printStackTrace();
            return false;
        }
    }

    private static String convertTime(String vttTime) {
        String[] parts = vttTime.split("[:.]");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        int millis = Integer.parseInt(parts[3]);

        // 计算总分钟数
        int totalMinutes = hours * 60 + minutes;
        // 毫秒转百分秒（四舍五入）
        int centiseconds = Math.round(millis / 10f);

        // 处理边界情况（如999毫秒 → 100分秒）
        if (centiseconds >= 100) {
            seconds += 1;
            centiseconds = 0;
        }

        // 格式化为LRC时间标签
        return String.format("[%02d:%02d.%02d]", totalMinutes, seconds, centiseconds);
    }
}