package ru.itmo.surikov;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class MNISTDataTools {


    public static int[][] intArrayIntoCellArray(int rows, int cols, int[] inputArray){
        if ((inputArray.length!=rows*cols)||(rows!=cols))return null;
        int[][] array = new int[rows][cols];
        for (int i = 0; i <rows ; i++) {
            for (int j = 0; j <cols ; j++) {
                array[j][i]=inputArray[i+j*cols];
            }


        }
        return array;
    }
    public static void printCells(int rows, int cols, int[][] array2) {

        for (int i = 0; i <rows ; i++) {
            for (int j = 0; j <cols ; j++) {
                if (array2[i][j]<10)
                    System.out.print(array2[i][j]+"  ");
                if ((array2[i][j]<100)&& (array2[i][j]>9))
                    System.out.print(array2[i][j]+" ");
                if ((array2[i][j]<1000)&& (array2[i][j]>99))
                    System.out.print(array2[i][j]+"");

            }
            System.out.print("\n");

        }
    }
    public static int indexMaxValue(int[] ind){
        int maxValue = ind[0];
        int indexMax = 0;
        for (int i = 1; i <ind.length ; i++) {
            if (maxValue<=ind[i]){
                maxValue = ind[i];
                indexMax = i;
            }
        }
        return indexMax;
    }
}
