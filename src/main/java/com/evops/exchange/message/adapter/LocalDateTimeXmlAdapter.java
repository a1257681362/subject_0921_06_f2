package com.evops.exchange.message.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 时间格式规范：yyyy-MM-dd HH:mm:ss */
public class LocalDateTimeXmlAdapter extends XmlAdapter<String, LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime unmarshal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), FORMATTER);
    }

    @Override
    public String marshal(LocalDateTime value) {
        return value == null ? null : FORMATTER.format(value);
    }
}
