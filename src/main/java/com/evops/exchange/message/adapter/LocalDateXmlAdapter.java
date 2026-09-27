package com.evops.exchange.message.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 日期格式规范：yyyy-MM-dd */
public class LocalDateXmlAdapter extends XmlAdapter<String, LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate unmarshal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim(), FORMATTER);
    }

    @Override
    public String marshal(LocalDate value) {
        return value == null ? null : FORMATTER.format(value);
    }
}
