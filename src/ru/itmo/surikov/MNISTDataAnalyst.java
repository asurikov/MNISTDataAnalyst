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

    // int threadsAmount = 4;//todo перепесать хард значения на получаемые
    int kNeighbor;//Количесто соседей в методе К-ближайших соседей
    MNISTDataSet[] mnistDataTrainingSet; //Массив содержащий тренировочные данные
    int[][] resultSet;//массив с результатами распознования. Первый столбец лабел из набора данных MNIST. Второй и третий результат распознования
    private Object lock = new Object();//мишка среди алкоголиков

    public MNISTDataAnalyst(int kNeighbor) {
        this.kNeighbor = kNeighbor;
    }

    //класс соседего объекта содержит лабел и дистанцию до определяемого элемента
    class NeighborClassObject implements Comparable<NeighborClassObject> {
        int label;
        double distance;

        public NeighborClassObject(int label, double distance) {
            this.label = label;
            this.distance = distance;
        }

        //переогрузка для сортировки
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

    //класс с методами для вычисления расстояний различными способами
    public class DistanceMethod {
        public double EuclideanDistance(MNISTDataSet P, MNISTDataSet Q) {
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

    //класс данных из базы MNIST содержит лабел и массив байт с картинкой
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

        //метод для определения значения картинки методом К ближайших соседей
        public int analyst(Distance func) {
            SortedSet<NeighborClassObject> neighbours = new TreeSet();//сортированный список ближайших соседей
            //  System.out.println("\nДано " + this.label);

            for (int i = 0; i < mnistDataTrainingSet.length; i++) {


                // double distance = this.EuclideanDistance(mnistDataTrainingSet[i]);
                double distance = func.returnDistance(this, mnistDataTrainingSet[i]); //вызываем метод определения рассояние между объектами данных MNIST
                //первые К просто попадают в сортированную коллекцию
                if (i <= kNeighbor) {
                    neighbours.add(new NeighborClassObject(mnistDataTrainingSet[i].getLabel(), distance));
                } else {
//если меньше наибольшего, то удаляем его и добавляем нового соседа
                    if (Double.compare(distance, neighbours.last().getDistance()) < 0) {
                        neighbours.remove(neighbours.last());
                        neighbours.add(new NeighborClassObject(mnistDataTrainingSet[i].getLabel(), distance));
                    }
                }
            }
            //подсчитываем частоту с которой встречаються цифры среди соседей
            int[] array = new int[10];
            for (NeighborClassObject item : neighbours) {
                array[item.label]++;

            }
//узнаем индекс(он же и значение) наиболее часто встречающегося элемента)
            return MNISTDataTools.indexMaxValue(array);
        }
    }

    //Функциональный интерфейс для передачи ссылки на метод
    interface Distance {
        double returnDistance(MNISTDataSet p, MNISTDataSet q);
    }

    //класс для организации вычислений по потокам
    class calculateThread implements Runnable {
        MNISTDataSet mns;
        int iCount;

        public calculateThread(MNISTDataSet mns, int iCount) {
            this.mns = mns;
            this.iCount = iCount;
        }

        @Override
        public void run() {
            //создаем объект класса вычислений
            DistanceMethod distanceMethod = new DistanceMethod();
            //вызываем аналиста с методом вычислений по формуле Евклида
            int returnValueEvklid = mns.analyst(distanceMethod::EuclideanDistance);
            //вызываем аналиста с методом вычислений по формуле городских кварталов
            int taxicabGeometry = mns.analyst(distanceMethod::taxicabGeometry);
            int lenght = resultSet.length;
            synchronized (lock) {
                resultSet[iCount][0] = mns.label;//данные из базы МНИСТ на текущий элемент
                resultSet[iCount][1] = returnValueEvklid;//распознали формулой Евклида
                resultSet[iCount][2] = taxicabGeometry;//распознали формулой Городских кварталов
                System.out.print((String.format("\rЗавершено: %.1f", ((double) iCount / lenght * 100)) + "%"));
            }
        }

    }

    //метод проверки результата распознования
    public void checkResult() {
        int mistakesEvklid = 0;
        int mistakesTaxicab = 0;
        for (int i = 0; i < resultSet.length; i++) {
            if (resultSet[i][0] != resultSet[i][1]) mistakesEvklid++;//ошибки по  формуле Евклида
            if (resultSet[i][0] != resultSet[i][2]) mistakesTaxicab++;//ошибки по  формуле городских кварталов
        }
        System.out.println("--------------------------------------------------");
        System.out.println((String.format("Погрешность распознования Евклидовой метрикой: %.1f", (double) mistakesEvklid / resultSet.length * 100) + "%"));
        System.out.println((String.format("Погрешность распознования методам городских кварталов: %.1f", (double) mistakesTaxicab / resultSet.length * 100) + "%"));

    }

    //запускаем главные процесс MNISTDataAnalyst
    public void run(String pathDataFolder) {
        Timer tm = new Timer();//взводим таймер
        String trainsetLabelFile = pathDataFolder + "train-labels-idx1-ubyte.gz";
        String trainsetImageFile = pathDataFolder + "train-images-idx3-ubyte.gz";
        String testsetLabelFile = pathDataFolder + "t10k-labels-idx1-ubyte.gz";
        String testsetImageFile = pathDataFolder + "t10k-images-idx3-ubyte.gz";
        mnistDataTrainingSet = loadMNISTData("Training Set ", trainsetLabelFile, trainsetImageFile);//загружаем тренировочную базу
        MNISTDataSet[] proTestSet = loadMNISTData("Test Set ", testsetLabelFile, testsetImageFile);//загружаем тестовую базу
        resultSet = new int[proTestSet.length][3]; //иницилизируем массив с результатами
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//создаем пул потоков с возможным числом потоков равным числу доступных процессоров
        System.out.print("\nРаспознование цифр:");
        //побежали по тестовой базе по каждому элементу
        for (int i = 0; i < proTestSet.length; i++) {

            calculateThread c = new calculateThread(proTestSet[i], i);//формируем объект для запуска потока
            threadPool.execute(c); //запускаем пул

            // c.run(); //оказываеться не обязательно так делать поток и так запуститься

        }
        threadPool.shutdown();
        //прибиваем пул
        //ждем завершения всех потоков
        while (!threadPool.isTerminated()) {
            //System.out.print(".........\r");//печатаем бегунок что еще не умерли
        }

        System.out.println("\nВремя выполнения " + tm.getTime());

    }

    //метод загрузки данный из базы MNIST
    private MNISTDataSet[] loadMNISTData(String name, String labelFile, String imageFile) {
        try {


            try (DataInputStream imageInput = new DataInputStream(new GZIPInputStream(new FileInputStream(imageFile))); DataInputStream labelInput = new DataInputStream(new GZIPInputStream(new FileInputStream(labelFile)));) {
                int labelMagicNumber = labelInput.readInt();//"магическое число лайблов"
                int labelsNumber = labelInput.readInt();//количество лайблов
                int magicNumber = imageInput.readInt();////"магическое число картинок"
                int itemsNumber = imageInput.readInt();//количество картинок
                int nRows = imageInput.readInt();//строки
                int nCols = imageInput.readInt();//колонки
                System.out.println("");
                System.out.println(name);
                System.out.println("magic number is " + magicNumber);
                System.out.println("number of items is " + itemsNumber);
                System.out.println("number of rows is: " + nRows);
                System.out.println("number of cols is: " + nCols);
                System.out.println("labels magic number is: " + labelMagicNumber);
                System.out.println("number of labels is: " + labelsNumber);
                //инициллизируем массив с данными
                MNISTDataSet[] mnistDataSets = new MNISTDataSet[itemsNumber];
                //заполняем в одномерный массив(изначально читал в двухмерный)
                for (int i = 0; i < itemsNumber; i++) {
                    int imageSize = nRows * nCols;
                    int[] data = new int[imageSize];
                    int digit = labelInput.readUnsignedByte();
                    // System.out.println(digit);
                    for (int r = 0; r < imageSize; r++) {

                        data[r] = imageInput.readUnsignedByte();

                    }
                    MNISTDataSet mns = new MNISTDataSet(digit, data);
                    mnistDataSets[i] = mns;
                    //"прогресбар"
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