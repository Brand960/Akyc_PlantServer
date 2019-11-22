package com.plantserver.Util;

import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class InfluxUtil {

    @Value("${spring.influx.measurement.shake}")
    private String shakeMeasurment;

    @Value("${spring.influx.measurement.temperature}")
    private String tempMeasurment;

    public Point shakePoint(String uid, long ts, Object[] entryData) {
        return Point.measurement(shakeMeasurment)
                .tag("sn", uid)
                .time(ts, TimeUnit.MILLISECONDS)
                .addField("Ax", (short) entryData[0])
                .addField("Ay", (short) entryData[1])
                .addField("Az", (short) entryData[2])
                .addField("Wx", (short) entryData[3])
                .addField("Wy", (short) entryData[4])
                .addField("Wz", (short) entryData[5])
                .build();
    }


    public Point tempPoint(String uid, long ts, Object[] entryData) {
        return Point.measurement(tempMeasurment)
                .tag("sn", uid)
                .time(ts, TimeUnit.MILLISECONDS)
                .addField("temp", (int) entryData[6])
                .build();
    }
}
