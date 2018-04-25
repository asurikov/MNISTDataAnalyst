package ru.itmo.surikov;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {

            int numberOfNeighbors = 20; //Количесто соседей в методе К-ближайших соседей
            String current = new java.io.File(".").getCanonicalPath() + File.separator + "Data" + File.separator;//путь к файлам с данными
            System.out.println(current);
            MNISTDataAnalyst mns = new MNISTDataAnalyst(numberOfNeighbors);//создаем экземпляр класса MNISTDataAnalyst
            //запускаем основной процесс
            mns.run(current);
            //выводим результаты
            mns.checkResult();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
