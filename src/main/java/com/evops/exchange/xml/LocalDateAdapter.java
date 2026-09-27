package com.evops.exchange.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日期格式规范：yyyy-MM-dd。
 */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate unmarshal(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(v.trim(), FMT);
    }

    @Override
    public String marshal(LocalDate v) {
        return v == null ? null : v.format(FMT);
    }
}
