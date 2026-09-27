/**
 * 客户自助下单平台报文 XML 模型。
 * 日期/时间按包级适配器统一规范：日期 yyyy-MM-dd，时间 yyyy-MM-dd HH:mm:ss。
 * BigDecimal 有金额（两位）与费率（四位）两种规范，按字段单独挂适配器。
 */
@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(value = LocalDateAdapter.class, type = java.time.LocalDate.class),
        @XmlJavaTypeAdapter(value = LocalDateTimeAdapter.class, type = java.time.LocalDateTime.class)
})
package com.evops.exchange.xml;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
