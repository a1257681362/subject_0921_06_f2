package com.evops.exchange.message;

import com.evops.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * 付印报文编解码：对象 &lt;-&gt; XML 报文原文。
 * 序列化输出即发送与归档的原文（UTF-8、格式化、元素顺序固定），
 * 同一内容多次序列化文本恒定，往返转换内容不丢失。
 */
@Component
public class PrintOrderMessageCodec {

    /** JAXBContext 线程安全，全局一份；Marshaller/Unmarshaller 非线程安全，随用随建 */
    private final JAXBContext jaxbContext;

    public PrintOrderMessageCodec() {
        try {
            this.jaxbContext = JAXBContext.newInstance(PrintOrderMessage.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("初始化付印报文编解码器失败", e);
        }
    }

    /** 序列化为 XML 报文原文 */
    public String toXml(PrintOrderMessage message) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(message, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new BusinessException("付印报文序列化失败：" + e.getMessage());
        }
    }

    /**
     * 反序列化平台来文。
     * 关闭 DOCTYPE 与外部实体（防 XXE），来文即外部不可信输入。
     */
    public PrintOrderMessage parse(String xml) {
        if (!StringUtils.hasText(xml)) {
            throw new BusinessException("报文内容为空");
        }
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
            SAXSource source = new SAXSource(xmlReader, new InputSource(new StringReader(xml)));

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (PrintOrderMessage) unmarshaller.unmarshal(source);
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            throw new BusinessException("报文解析失败，请核对报文格式：" + firstMeaningfulMessage(e));
        }
    }

    /** 异常链上第一条非空描述（JAXB 常把 SAX 具体错误包在 cause 里），兜底用异常类名 */
    private String firstMeaningfulMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return throwable.getClass().getSimpleName();
    }
}
