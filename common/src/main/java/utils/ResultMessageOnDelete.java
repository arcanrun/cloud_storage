package utils;

import java.io.Serializable;

public class ResultMessageOnDelete implements Serializable {
    private static final long serialVersionUID = 7486810478106891699L;
    private String message;
    private String type;

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }
}
