package com.plantserver.task;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
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

    private static int SAMPLE = 2048;

    private static int PEAK_OFFSET = 9;

    private static int RECMAX = 1;
    private static int RECSTD = 1100;
    private static int RECMIN = 0;

    private static InfluxDB influxDB = InfluxDBFactory.connect("http://60.205.207.115:8086", "root", "root");

    @Scheduled(cron = "0/5 * * * * *")
    public void caculFFT() throws IOException, ParseException {
        System.out.println("INFO:" + new Date() + "开始运行任务");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-ddHH:mm:ss");
        influxDB.setDatabase("plantsurv_web");
        List<List<Object>> rawdata = influxDB.query(new Query("SELECT \"Ax\"-Ax+abs(Ax-4950)+abs(Ay+312)+abs(Az-15730) " +
                "FROM \"pt_new_255\" " +
                "WHERE (\"Ay\" < 0.0 OR \"Ay\" > 0.0) " +
                "ORDER BY DESC LIMIT " + SAMPLE))
                .getResults().get(0)
                .getSeries().get(0)
                .getValues();
        Date latestFFTdata = format.parse((influxDB.query(new Query("SELECT \"Ay\"" +
                "FROM \"pt_test_fft\" "
                + "ORDER BY DESC LIMIT 1"))
                .getResults().get(0)
                .getSeries().get(0)
                .getValues().get(0).get(0)).toString().replace("T", ""));
        // 初始化2次幂容量的数组
        //        int j = 1;
        int size = rawdata.size();
        //        while (j < size) {
        //            j *= 2;
        //        }
        //        Complex[] data = new Complex[j];
        Complex[] data = new Complex[SAMPLE];
        // 填充数据和补0
        int i = 0;
        for (List tmp : rawdata) {
            data[i] = new Complex(Math.abs((double) tmp.get(1)), 0);
            i++;
        }
        //        while (j > i) {
        //            data[i] = new Complex(0, 0);
        //            i++;
        //        }
        // FFT
        Complex[] result = FFT.fft(data);
        // 梯度下降找局部最小，之后归0
        int ThriMaxIndex = getThriMaxItemIndex(result);
        System.out.println("局部最大点位置：" + ThriMaxIndex);
        int offsetLocalMinIndex = getLocalMinIndex(result, ThriMaxIndex, PEAK_OFFSET);
        System.out.println("局部最小点位置：" + offsetLocalMinIndex);
        for (int f = 0; f < result.length; f++) {
            if (f > 24) {
                result[f] = new Complex(0, 0);
            }
        }
        // IFFT
        Complex[] finalRes = FFT.ifft(result);
        BatchPoints batchPoints1 = BatchPoints.database("plantsurv_web").build();
        try {
            for (int p = 0; p < size; p++) {
                if (format.parse(rawdata.get(p).get(0).toString()
                        .replace("T", "")).compareTo(latestFFTdata) < 0) {
                    continue;
                }
                Point tmpPoint1 = Point.measurement("pt_test_fftpre")
                        .time(format.parse(rawdata.get(p).get(0).toString()
                                        .replace("T", ""))
                                        .getTime() + 8 * 60 * 60 * 1000L,
                                TimeUnit.MILLISECONDS)
                        .tag("device", "996")
                        .addField("Ay", finalRes[p].re())
                        .build();
                batchPoints1.point(tmpPoint1);
            }
            influxDB.write(batchPoints1);
        } catch (ParseException e) {
            InfluxDB influxDB3 = InfluxDBFactory.connect("http://172.17.0.2:8086", "root", "root");
            influxDB3.write(batchPoints1);
            influxDB3.close();
            e.getMessage();
        }
        // 先获得极值点,转成矩形波
        List<Integer> exPoints = getExPoints(finalRes, size);
        System.out.println("发现的极值点：" + exPoints);
        //Complex[] resultRec = turnRecWaves(finalRes, exPoints, size);

        for (int p = 0; p < finalRes.length; p++) {
            if (finalRes[p].abs() > 9500) {
                finalRes[p] = new Complex(RECMAX, 0);
            } else {
                try {
                    if (finalRes[p + 1].abs() > 9500 || finalRes[p + 2].abs() > 9500) {
                        finalRes[p] = new Complex(RECMAX, 0);
                        continue;
                    }
                    finalRes[p] = new Complex(RECMIN, 0);
                } catch (Exception e) {
                    e.getMessage();
                }
            }
        }

        // 写入INFLUX
        BatchPoints batchPoints2 = BatchPoints.database("plantsurv_web").build();
        try {
            for (int p = 0; p < size; p++) {
                if (format.parse(rawdata.get(p).get(0).toString()
                        .replace("T", "")).compareTo(latestFFTdata) < 0) {
                    continue;
                }
                Point tmpPoint = Point.measurement("pt_test_fft")
                        .time(format.parse(rawdata.get(p).get(0).toString()
                                        .replace("T", ""))
                                        .getTime() + 8 * 60 * 60 * 1000L,
                                TimeUnit.MILLISECONDS)
                        .tag("device", "996")
                        //resultRec->finalRes
                        .addField("Ay", finalRes[p].re())
                        .build();
                batchPoints2.point(tmpPoint);
            }
            influxDB.write(batchPoints2);
        } catch (
                ParseException e) {
            InfluxDB influxDB2 = InfluxDBFactory.connect("http://172.17.0.2:8086", "root", "root");
            influxDB2.write(batchPoints2);
            influxDB2.close();
            e.getMessage();
        }
        System.out.println("OK");
    }

    static void export2File(Complex[] result) throws IOException {
        File f = new File("/home/yue/Desktop/FFT/" + new Date().toString() + ".txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        BufferedWriter fw = new BufferedWriter(new FileWriter("/home/yue/Desktop/FFT/" + new Date().toString() + ".txt"));
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
        int max3_index = 0;
        Complex max = new Complex(0, 0);
        Complex max2 = new Complex(0, 0);
        Complex max3 = new Complex(0, 0);
        for (int k = 0; k < target.length / 2; k++) {
            if (target[k].abs() > max.abs()) {
                max = target[k];
            }
        }
        for (int l = 0; l < target.length / 2; l++) {
            if (target[l].abs() > max2.abs() && target[l].abs() != max.abs()) {
                max2 = target[l];
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
        int i = 0, j = offset, minIndex = 0;
        Complex min = new Complex(99999999, 99999999);
        startIndex += 1;
        for (; startIndex < target.length && j > 0; startIndex++) {
            if ((target[startIndex].abs() - target[startIndex - 1].abs()) > 0) {
                tmp[i] = startIndex;
                i++;
                j--;
            }
        }
        for (int index : tmp) {
            if (target[index].abs() < min.abs()) {
                min = target[index];
                minIndex = index;
            }
        }
        return minIndex;
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
