package ru.itmo.surikov;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class MNISTDataAnalyst {

    int threadsAmount = 4;//todo перепесать хард значения на получаемые
    int kNeighbor = 20;
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
    public class DistanceMethod {
        public  double EuclideanDistance(MNISTDataSet P, MNISTDataSet Q) {
            int[] p = P.getImage();
            int[] q = Q.getImage();
            if (p.length == q.length) {
                double sum = 0;
                for (int i = 0; i < p.length; i++) {
                    sum += Math.pow((q[i] - p[i]), 2);
                }
                return Math.sqrt(sum);
            } else return -1;

        }

        public double taxicabGeometry(MNISTDataSet P, MNISTDataSet Q) {
            int[] p = P.getImage();
            int[] q = Q.getImage();
            if (p.length == q.length) {
                double sum = 0;
                for (int i = 0; i < p.length; i++) {
                    sum += Math.abs((q[i] - p[i]));
                }
                return sum;
            } else return -1;
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

        public int analyst(Distance func) {
            SortedSet<NeighborClassObject> neighbours = new TreeSet();
            //  System.out.println("\nДано " + this.label);

            for (int i = 0; i < mnistDataTrainingSet.length; i++) {


               // double distance = this.EuclideanDistance(mnistDataTrainingSet[i]);
                double distance = func.returnDistance(this,mnistDataTrainingSet[i]);
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
//Функциональный интерфейс для лямбда выражения
interface Distance{
        double returnDistance(MNISTDataSet p, MNISTDataSet q);
}

    class calculateEuclideanDistanceThread implements Runnable {
        MNISTDataSet mns;
        int iCount;

        public calculateEuclideanDistanceThread(MNISTDataSet mns, int iCount) {
            this.mns = mns;
            this.iCount = iCount;
        }

        @Override
        public void run() {
            DistanceMethod distanceMethod = new DistanceMethod();
            int returnValueEvklid = mns.analyst(distanceMethod::EuclideanDistance);
            int taxicabGeometry = mns.analyst(distanceMethod::taxicabGeometry);
            int progressBar = resultSet.length;
            synchronized (lock) {
                resultSet[iCount][0] = mns.label;
                resultSet[iCount][1] = returnValueEvklid;
                resultSet[iCount][2] = taxicabGeometry;
                System.out.print((String.format("\rЗавершено: %.1f", ((double) iCount / progressBar * 100)) + "%"));
            }
        }

    }

    //метод проверки результата распознования
    public void checkResult() {
        int mistakesEvklid = 0;
        int mistakesTaxicab = 0;
        for (int i = 0; i < resultSet.length; i++) {
            if (resultSet[i][0] != resultSet[i][1]) mistakesEvklid++;
            if (resultSet[i][0] != resultSet[i][2]) mistakesTaxicab++;
        }
        System.out.println("--------------------------------------------------");
        System.out.println((String.format("Погрешность распознования Евклидовой метрикой: %.1f", (double) mistakesEvklid / resultSet.length * 100) + "%"));
        System.out.println((String.format("Погрешность распознования методам городских кварталов: %.1f", (double) mistakesTaxicab / resultSet.length * 100) + "%"));

    }

    public void run(String pathDataFolder) {
        Timer tm = new Timer();
        String trainsetLabelFile = pathDataFolder + "train-labels-idx1-ubyte.gz";
        String trainsetImageFile = pathDataFolder + "train-images-idx3-ubyte.gz";
        String testsetLabelFile = pathDataFolder + "t10k-labels-idx1-ubyte.gz";
        String testsetImageFile = pathDataFolder + "t10k-images-idx3-ubyte.gz";
        mnistDataTrainingSet = loadMNISTData("Training Set ", trainsetLabelFile, trainsetImageFile);
        MNISTDataSet[] proTestSet = loadMNISTData("Test Set ", testsetLabelFile, testsetImageFile);
        resultSet = new int[proTestSet.length][3];
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.print("\nРаспознование цифр:");
        for (int i = 0; i < proTestSet.length; i++) {

            calculateEuclideanDistanceThread c = new calculateEuclideanDistanceThread(proTestSet[i], i);
            threadPool.execute(c);

            // resultSet[i][0] = proTestSet[i].label;
            //resultSet[i][1] = proTestSet[i].analyst();

            //System.out.print((String.format("\кЗавершено: %.1f", ((double) i / proTestSet.length * 100)) + "%"));
            // c.run();

        }


        threadPool.shutdown();
        while (!threadPool.isTerminated()) {
            //System.out.print(".........\r");//печатаем бегунок что еще не умерли
        }

        System.out.println("\nВремя выполнения " + tm.getTime());

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