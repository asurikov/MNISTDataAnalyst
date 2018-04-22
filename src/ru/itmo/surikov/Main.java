package ru.itmo.surikov;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
//            String current = new java.io.File(".").getCanonicalPath()+ "\\" + "Data" + "\\" ;
            String current = new java.io.File(".").getCanonicalPath()+ File.separator + "Data" + File.separator ;
            System.out.println(current);
            MNISTDataAnalyst mns = new MNISTDataAnalyst();
            mns.run(current);
            System.out.println("--------------------------------------------------");
            System.out.println((String.format("Погрешность распознования: %.1f", mns.checkResult()) + "%"));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
