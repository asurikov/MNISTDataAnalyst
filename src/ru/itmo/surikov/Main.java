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
mns.checkResult();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
