package com.plantserver.Util;

import com.plantserver.entity.MPU6500;
import com.plantserver.entity.VAPE;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class PointUtil {

    @Value("${spring.influx.measurement.shake}")
    private String shakeMeasurement;

    @Value("${spring.influx.measurement.temperature}")
    private String tempMeasurement;

    @Value("${spring.influx.measurement.power}")
    private String powerMeasurement;

    public Point shakePoint(String uid, MPU6500 entryData) {
        return Point.measurement(shakeMeasurement)
                .tag("sn", uid)
                .time(entryData.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("ax", entryData.getAx())
                .addField("ay", entryData.getAy())
                .addField("az", entryData.getAz())
                .addField("px", entryData.getPx())
                .addField("py", entryData.getPy())
                .addField("pz", entryData.getPz())
                .build();
    }

    public Point tempPoint(String uid, MPU6500 entryData) {
        return Point.measurement(tempMeasurement)
                .tag("sn", uid)
                .time(entryData.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("temperature", entryData.getTemperature())
                .build();
    }

    public Point powerPoint(String uid, VAPE entryData) {
        return Point.measurement(powerMeasurement)
                .tag("sn", uid)
                .time(entryData.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("v", entryData.getV())
                .addField("a", entryData.getA())
                .addField("p", entryData.getP())
                .addField("e", entryData.getE())
                .build();
    }
}
