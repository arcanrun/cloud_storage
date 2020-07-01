package fxclient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

public class FileInfo {
    private String name;
    private String type;
    private Long size;
    private Path path;

    public FileInfo(String name, String type, Long size) {
        this.name = name;
        this.type = type;
        this.size = size;

    }

    public FileInfo(Path path) {
        if (path.toFile().isFile()) {
            this.path = path;
            String fileNameWithType = path.getFileName().toString();
            this.name = fileNameWithType.substring(0, fileNameWithType.lastIndexOf("."));
            this.type = fileNameWithType.substring(fileNameWithType.lastIndexOf(".") + 1);
            this.size = path.toFile().length();
        }
        if (path.toFile().isDirectory()) {
            this.name = path.getFileName().toString();
            this.type = "DIR";
            this.size = path.toFile().length();

        }

    }

    public FileInputStream getFileInputStream() throws FileNotFoundException {
        return new FileInputStream(path.toFile());
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", size=" + size +
                '}';
    }
}
