package fxclient;

public class Questions {
    //Как и в каком виде передавать файлы?
    // -- В виде простого потока байтов, использовать потоки ввода/вывода

    //Как пересылать большие файлы?
    //использовать буфер, например использовать в качестве буффера байт массив
    //                    byte [] buffer = new byte[1024];
    //                    File file = new File(dir + "\\" + fileName);
    //                    FileOutputStream fos = new FileOutputStream(file);
    //                    for (long i = 0; i <= fileLength / 1024; i++) {
    //                        int read = in.read(buffer);
    //                        System.out.println("bytes readed!");
    //                        fos.write(buffer, 0, read);
    //                        fos.flush();
    //

    //Как пересылать служебные команды?
    // --  Например, строка со специальным префиксом: /Send

    //Что хранить в базе данных?
    // -- Данные касаемые пользователей. Например, таблица Users с атрибутами login, password

    //Как передавать структуру каталогов/файлов?
    //Передавать объект класса File и запрашивать структур каталога относительного него, а также структуру самого файла
}
