package utils;

public enum DataTypes {
    FILE((byte) 15),
    SERVER_ERROR((byte) 29),
    UI_UPDATE_BY_SERVER_CHANGE((byte) 25),
    FILE_REQUEST((byte)16),
    FILE_ACCEPT((byte)17),
    FILE_DELETE_REQUEST((byte)18),
    FILE_DELETE_RESPONSE((byte)19);
    byte signalByte;

    DataTypes(byte signalByte) {
        this.signalByte = signalByte;
    }

    public byte getByte() {
        return signalByte;
    }
}