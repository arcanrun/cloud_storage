package utils;

import java.io.Serializable;
import java.nio.file.Path;

public class DirInfo implements Serializable {
    private static final long serialVersionUID = -3326907249936484622L;
    private String path;


    public DirInfo(Path path) {
        this.path = path.toString();
    }

    public String getPath() {
        return path;
    }
}
