package utils;

import javax.swing.plaf.nimbus.State;

public enum DataTypes {
    FILE((byte) 15),
    UI_UPDATE_BY_SERVER_CHANGE((byte) 25),
    FILE_REQUEST((byte)16),
    FILE_ACCEPT((byte)17),
    FILE_DELETE_REQUEST((byte)18),
    FILE_DELETE_RESPONSE((byte)19),
    AUTH_USER_REQUEST((byte)12),
    FILE_READY_TO_ACCEPT((byte)20),
    AUTH_OK((byte)11);
    byte signalByte;

    DataTypes(byte signalByte) {
        this.signalByte = signalByte;
    }

    public byte getByte() {
        return signalByte;
    }

}