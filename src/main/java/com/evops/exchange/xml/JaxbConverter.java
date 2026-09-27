package com.evops.exchange.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JAXB 序列化/反序列化。统一 UTF-8、头声明、不带 XML 独立 standalone 声明以外的属性。
 * 入站反序列化只基于调用方给的原文，留档原文与解析对象一一对应。
 */
public final class JaxbConverter {

    private static final ConcurrentMap<Class<?>, JAXBContext> CONTEXTS = new ConcurrentHashMap<>();

    private JaxbConverter() {
    }

    public static String toXml(Object bean) {
        try {
            StringWriter writer = new StringWriter(1024);
            Marshaller marshaller = context(bean.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
            marshaller.marshal(bean, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalArgumentException("XML 序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> T fromXml(String xml, Class<T> type) {
        return fromXml(new StringReader(xml), type);
    }

    public static <T> T fromXml(Reader reader, Class<T> type) {
        try {
            Unmarshaller unmarshaller = context(type).createUnmarshaller();
            return type.cast(unmarshaller.unmarshal(reader));
        } catch (JAXBException e) {
            throw new IllegalArgumentException("XML 反序列化失败: " + e.getMessage(), e);
        }
    }

    private static JAXBContext context(Class<?> type) {
        return CONTEXTS.computeIfAbsent(type, c -> {
            try {
                return JAXBContext.newInstance(c);
            } catch (JAXBException e) {
                throw new IllegalStateException("JAXBContext 初始化失败: " + c.getName(), e);
            }
        });
    }
}
