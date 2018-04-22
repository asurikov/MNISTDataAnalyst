package ru.itmo.surikov;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class MNISTDataAnalyst {

    int threadsAmount = 2;//todo перепесать хард значения на получаемые
    int kNeighbor = 10;
    MNISTDataSet[] mnistDataTrainingSet;
    int[][] resultSet;
    private Object lock = new Object();

    class NeighborClassObject implements Comparable<NeighborClassObject> {
        int label;
        double distance;

        public NeighborClassObject(int label, double distance) {
            this.label = label;
            this.distance = distance;
        }

        @Override
        public int compareTo(NeighborClassObject o) {

            if (this.getDistance() > o.getDistance())
                return 1;
            else if (this.getDistance() < o.getDistance())
                return -1;
            else
                return 0;
        }


        public int getLabel() {
            return label;
        }

        public void setLabel(int label) {
            this.label = label;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }
    }

    class MNISTDataSet {
        int label;
        int[] image;

        public int getLabel() {
            return label;
        }

        public void setLabel(int label) {
            this.label = label;
        }

        public int[] getImage() {
            return image;
        }

        public void setImage(int[] image) {
            this.image = image;
        }

        public MNISTDataSet() {
        }

        public MNISTDataSet(int label, int[] image) {
            this.label = label;
            this.image = image;
        }

        public double calculateEuclideanDistance(MNISTDataSet mns) {
            int[] p = this.getImage();
            int[] q = mns.getImage();
            if (p.length == q.length) {
                double sum = 0;
                for (int i = 0; i < p.length; i++) {
                    sum += Math.pow((q[i] - p[i]), 2);
                }
                return Math.sqrt(sum);
            } else return -1;

        }

        public int analyst() {
            SortedSet<NeighborClassObject> neighbours = new TreeSet();
            //  System.out.println("\nДано " + this.label);

            for (int i = 0; i < mnistDataTrainingSet.length; i++) {


                double distance = this.calculateEuclideanDistance(mnistDataTrainingSet[i]);
                //    NeighborClassObject neighbor = new NeighborClassObject(MNISTDataTrainingSet[i].getLabel(), distance);
                if (i <= kNeighbor) {
                    neighbours.add(new NeighborClassObject(mnistDataTrainingSet[i].getLabel(), distance));
                } else {

                    if (Double.compare(distance, neighbours.first().getDistance()) < 0) {
                        neighbours.remove(neighbours.last());
                        neighbours.add(new NeighborClassObject(mnistDataTrainingSet[i].getLabel(), distance));
                    }
                }
            }
            int[] array = new int[10];
            for (NeighborClassObject item : neighbours) {
                array[item.label]++;
                //  System.out.println("Сосед " + item.label);
            }

            return MNISTDataTools.indexMaxValue(array);
        }
    }


    class calculateEuclideanDistanceThread implements Runnable {
        MNISTDataSet mns;

        public calculateEuclideanDistanceThread(MNISTDataSet mns) {
            this.mns = mns;
        }

        @Override
        public void run() {
            SortedSet<NeighborClassObject> kSet = new TreeSet<NeighborClassObject>(); //Коллекция с к эелементами
            for (int i = 0; i < mnistDataTrainingSet.length; i++) {
                double distance = mns.calculateEuclideanDistance(mnistDataTrainingSet[i]);
                NeighborClassObject neighbor = new NeighborClassObject(mnistDataTrainingSet[i].getLabel(), distance);
                if (i <= kNeighbor) {
                    kSet.add(neighbor);
                } else {

                    if (Double.compare(neighbor.getDistance(), kSet.first().getDistance()) < 0) {
                        kSet.remove(kSet.last());
                        kSet.add(neighbor);
                    }
                }

            }
            for (MNISTDataSet item : mnistDataTrainingSet) {
                mns.calculateEuclideanDistance(item);

            }
            synchronized (lock) {
            }
        }

    }

    public double checkResult() {
        int mistakes = 0;
        for (int i = 0; i < resultSet.length; i++) {
            if (resultSet[i][0] != resultSet[i][1]) mistakes++;
        }
return (double)mistakes/resultSet.length*100;
    }

    public void run(String pathDataFolder) {
        Timer tm = new Timer();
        String trainsetLabelFile = pathDataFolder + "train-labels-idx1-ubyte.gz";
        String trainsetImageFile = pathDataFolder + "train-images-idx3-ubyte.gz";
        String testsetLabelFile = pathDataFolder + "t10k-labels-idx1-ubyte.gz";
        String testsetImageFile = pathDataFolder + "t10k-images-idx3-ubyte.gz";
        mnistDataTrainingSet = loadMNISTData("Training Set ", trainsetLabelFile, trainsetImageFile);
        MNISTDataSet[] proTestSet = loadMNISTData("Test Set ", testsetLabelFile, testsetImageFile);
        resultSet = new int[proTestSet.length][2];
        // ExecutorService threadPool = Executors.newFixedThreadPool(kNeighbor);
        System.out.print("\nРаспознование цифр:");
        for (int i = 0; i < proTestSet.length; i++) {
            //for (int i = 0; i < 2; i++) {
            //}

            //for (MNISTDataTrainingSetObject trainItem : proTestSet) {
            // for (MNISTDataTrainingSetObject trainItem : MNISTDataTrainingSet) {
            // calculateEuclideanDistanceThread c = new calculateEuclideanDistanceThread(testItem);
            //  threadPool.execute(c);
            // System.out.print(proTestSet[i].calculateEuclideanDistance(testItem));
            resultSet[i][0] = proTestSet[i].label;
            resultSet[i][1] = proTestSet[i].analyst();

            System.out.print("\r");
            System.out.print((String.format("Завершено: %.1f", ((double) i / proTestSet.length * 100)) + "%"));
            //  System.out.println("Дано " + proTestSet[i].label + "  " + proTestSet[i].analyst());
            // c.run();
            // System.out.print(".........\r");
        }


        // threadPool.shutdown();
        // while (!threadPool.isTerminated()) {
        //  System.out.print(".........\r");//печатаем бегунок что еще не умерли
        //  }

        System.out.println("\nвремя выполнения "+tm.getTime());

    }


    private MNISTDataSet[] loadMNISTData(String name, String labelFile, String imageFile) {
        try {


            try (DataInputStream imageInput = new DataInputStream(new GZIPInputStream(new FileInputStream(imageFile))); DataInputStream labelInput = new DataInputStream(new GZIPInputStream(new FileInputStream(labelFile)));) {
                int labelMagicNumber = labelInput.readInt();
                int labelsNumber = labelInput.readInt();
                int magicNumber = imageInput.readInt();
                int itemsNumber = imageInput.readInt();
                int nRows = imageInput.readInt();
                int nCols = imageInput.readInt();
                System.out.println("");
                System.out.println(name);
                System.out.println("magic number is " + magicNumber);
                System.out.println("number of items is " + itemsNumber);
                System.out.println("number of rows is: " + nRows);
                System.out.println("number of cols is: " + nCols);
                System.out.println("labels magic number is: " + labelMagicNumber);
                System.out.println("number of labels is: " + labelsNumber);
                MNISTDataSet[] mnistDataSets = new MNISTDataSet[itemsNumber];
                for (int i = 0; i < itemsNumber; i++) {
                    int imageSize = nRows * nCols;
                    int[] data = new int[imageSize];
                    int digit = labelInput.readUnsignedByte();
                    // System.out.println(digit);
                    for (int r = 0; r < imageSize; r++) {

                        data[r] = imageInput.readUnsignedByte();

                    }
                    MNISTDataSet mns = new MNISTDataSet(digit, data);
                    //mnistDataSets.add(mns);
                    mnistDataSets[i] = mns;
                    System.out.print("\r");

                    System.out.print(name + (String.format("Загрузка данных: %.1f", ((double) i / itemsNumber * 100)) + "%"));

                }
                return mnistDataSets;

                //    MNISTData.printCells(nRows, nCols, data);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }


    }


}