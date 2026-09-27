package com.evops.exchange.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式规范：yyyy-MM-dd HH:mm:ss（不带时区，本地时区 Asia/Shanghai）。
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime unmarshal(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(v.trim(), FMT);
    }

    @Override
    public String marshal(LocalDateTime v) {
        return v == null ? null : v.format(FMT);
    }
}
