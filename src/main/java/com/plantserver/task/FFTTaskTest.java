package com.plantserver.task;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FFTTaskTest {

    private static int OFFSET = 3;
    private static int RECMAX = 8000;
    private static int RECSTD = 1000;
    private static int RECMIN = 0;

    private static InfluxDB influxDB = InfluxDBFactory.connect("http://60.205.207.115:8086", "root", "root");

    @Scheduled(cron = "* 18 * * * *")
    public void Rec() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-ddHH:mm:ss");
        influxDB.setDatabase("plantsurv_web");
        List<List<Object>> rawdata = influxDB.query(new Query("SELECT \"Ay\" FROM \"pt_new_255\" " +
                "WHERE (\"Ay\" < 0.0 OR \"Ay\" > 0.0) " +
                "AND time >= now() - 15m"))
                .getResults().get(0)
                .getSeries().get(0)
                .getValues();
        // 初始化2次幂容量的数组
        int j = 1;
        int size = rawdata.size();
        while (j < size) {
            j *= 2;
        }
        Complex[] data = new Complex[j];
        // 填充数据和补0
        int i = 0;
        for (List tmp : rawdata) {
            data[i] = new Complex(Math.abs((double) tmp.get(1)), 0);
            i++;
        }
        while (j > i) {
            data[i] = new Complex(0, 0);
            i++;
        }
        // FFT
        Complex[] result = FFT.fft(data);
        // 梯度下降找局部最小，之后归0
        int ThriMaxIndex = getThriMaxItemIndex(result);
        int offsetLocalMinIndex = getLocalMinIndex(result, ThriMaxIndex, OFFSET);
        for (int f = 0; f < result.length; f++) {
            if (f > offsetLocalMinIndex) {
                result[f] = new Complex(0, 0);
            }
        }
        // IFFT
        Complex[] finalRes = FFT.ifft(result);
        // 先获得极值点,转成矩形波
        List<Integer> exPoints = getExPoints(finalRes, size);
        Complex[] resultRec = turnRecWaves(finalRes, exPoints, size);
        // 写入INFLUX
        BatchPoints batchPoints = BatchPoints.database("plantsurv_web").build();
        try {
            for (int p = 0; p < size; p++) {
                Point tmpPoint = null;
                tmpPoint = Point.measurement("pt_test_fft")
                        .time(format.parse(rawdata.get(p).get(0).toString().replace("T", " ")).getTime()+8*60*60*1000L, TimeUnit.MILLISECONDS)
                        .tag("device", "996")
                        .addField("Ay", resultRec[p].re())
                        .build();
                batchPoints.point(tmpPoint);
            }
            influxDB.write(batchPoints);
        } catch (ParseException e) {
            e.printStackTrace();
        }
//        try {
//            expot2File(resultRec);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.print("OK");
    }

    static void expot2File(Complex[] result) throws IOException {
        BufferedWriter fw = new BufferedWriter(new FileWriter("/home/yue/Desktop/txt5.txt"));

        for (int b = 0; b < result.length; b++) {
            try {
                fw.write(String.valueOf(result[b].abs()));
            } catch (Exception e) {
                e.getMessage();
            }
            fw.newLine();
        }

        fw.flush();
        fw.close();
    }

    static int getThriMaxItemIndex(Complex[] target) {
        int max_index = 0, max2_index = 0, max3_index = 0;
        Complex max = new Complex(0, 0);
        Complex max2 = new Complex(0, 0);
        Complex max3 = new Complex(0, 0);
        int fre = 0;
        for (int k = 0; k < target.length / 2; k++) {
            if (target[k].abs() > max.abs()) {
                max = target[k];
                max_index = k;
            }
        }
        for (int l = 0; l < target.length / 2; l++) {
            if (target[l].abs() > max2.abs() && target[l].abs() != max.abs()) {
                max2 = target[l];
                max2_index = l;
            }
        }
        for (int m = 0; m < target.length / 2; m++) {
            if (target[m].abs() > max3.abs()
                    && target[m].abs() != max.abs()
                    && target[m].abs() != max2.abs()) {
                max3 = target[m];
                max3_index = m;
            }
        }
        return max3_index;
    }

    static int getLocalMinIndex(Complex[] target, int startIndex, int offset) {
        int[] tmp = new int[offset];
        int i = 0, j = 0;
        Complex min = new Complex(999999, 999999);
        startIndex += 1;
        for (; startIndex < target.length && i < offset; startIndex++) {
            if ((target[startIndex].abs() - target[startIndex - 1].abs()) > 0) {
                tmp[i] = startIndex;
                i++;
            }
        }
        for (int index : tmp) {
            if (target[index].abs() < min.abs()) {
                min = target[index];
                j = index;
            }
        }
        return j;
    }

    static List<Integer> getExPoints(Complex[] target, int totalNum) {
        List<Integer> tmp = new ArrayList<>();
        for (int i = 1; i < totalNum - 1; i++) {
            if ((target[i].re() - target[i - 1].re()) * (target[i + 1].re() - target[i].re()) < 0) {
                tmp.add(i);
            }
        }
        return tmp;
    }

    static Complex[] turnRecWaves(Complex[] target, List<Integer> exPoints, int size) {
        int j = 1, i = 0;
        Complex[] finRes = new Complex[size];
        while (j < exPoints.size()) {
            double midVal = (target[exPoints.get(j)].re() + target[exPoints.get(j - 1)].re()) / 2;
            for (; i < exPoints.get(j); i++) {
                if (Math.abs(target[exPoints.get(j)].re() - target[exPoints.get(j - 1)].re()) > RECSTD) {
                    if (target[i].re() >= midVal) {
                        finRes[i] = new Complex(RECMAX, 0);
                    } else {
                        finRes[i] = new Complex(RECMIN, 0);
                    }
                } else {
                    finRes[i] = new Complex(RECMIN, 0);
                }
            }
            i = exPoints.get(j);
            j++;
        }
        int last = exPoints.get(j - 1) - 1;
        while (last < size) {
            if (Math.abs(target[exPoints.get(j - 1)].re() - target[exPoints.get(j - 2)].re()) > RECSTD) {
                double midVal = (target[exPoints.get(j - 1)].re() + target[exPoints.get(j - 2)].re()) / 2;
                if (target[last].re() >= midVal) {
                    finRes[last] = new Complex(RECMAX, 0);
                } else {
                    finRes[last] = new Complex(RECMIN, 0);
                }
            } else {
                finRes[last] = new Complex(RECMIN, 0);
            }
            last++;
        }
        return finRes;
    }
}
