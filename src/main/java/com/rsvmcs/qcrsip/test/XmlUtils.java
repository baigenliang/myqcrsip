package com.rsvmcs.qcrsip.test;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.rsvmcs.qcrsip.test.json.JsonUtils;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/**
 * XmlUtils
 *
 * @author LonelySnow
 * @version 1.0
 * @date 2023/2/6
 */
public class XmlUtils {

    /**
     * 属性名前缀
     */
    private String attributeNamePrefix;

    /**
     * 文本键名称
     */
    private String textKey;

    /**
     * 正则替换内容
     */
    private String reqEx = "<\\?xml .* ?\\?>";

    // // // // // // //  直接适用的静态接口  // // // // // // // // //

    /**
     * xml转json
     * @author LonelySnow
     * @param xml
     * @return java.lang.String
     * @date 2023/2/8 10:21
     */
    public static String xmlToJson(String xml) {
        XmlUtils xmlUtils = new XmlUtils();
        return xmlUtils.xmlToJsonSelf(xml);
    }

    /**
     * xml转json
     * @author LonelySnow
     * @param element
     * @return java.lang.String
     * @date 2023/2/8 10:21
     */
    public static String xmlToJson(Element element) {
        XmlUtils xmlUtils = new XmlUtils();
        return xmlUtils.xmlToJsonSelf(element);
    }

    /**
     * json转xml
     * list必须带有根节点，不然报错，比如：{"abc":["aa","bb"]},此种格式的json串不支持，无根节点； 格式应该为：{"root":{"abc":["aa","bb"]}}
     * @author LonelySnow
     * @param json
     * @return java.lang.String
     * @date 2023/2/7 14:45
     */
    public static String jsonToXml(String json) {
        XmlUtils xmlUtils = new XmlUtils();
        return xmlUtils.jsonToXmlSelf(json);
    }

    /**
     * xml转bean
     * @author LonelySnow
     * @param xml
     * @param tClass
     * @return T
     * @date 2023/2/7 14:55
     */
    public static <T> T xmlToBean(String xml, Class<T> tClass) {
        String json = xmlToJson(xml);
        return JsonUtils.toBean(json, tClass);
    }

    /**
     * xml转bean
     * @author LonelySnow
     * @param element
     * @param tClass
     * @return T
     * @date 2023/2/8 10:22
     */
    public static <T> T xmlToBean(Element element, Class<T> tClass) {
        String json = xmlToJson(element);
        return JsonUtils.toBean(json, tClass);
    }

    /**
     * Bean转xml
     * @author LonelySnow
     * @param obj
     * @return java.lang.String
     * @date 2023/2/7 14:56
     */
    public static String beanToXml(Object obj) {
        String json = JsonUtils.toString(obj);
        return jsonToXml(json);
    }

    /**
     * xml美化工具
     * @author LonelySnow
     * @param xml
     * @return java.lang.String
     * @date 2023/2/13 09:30
     */
    public static String prettyXml(String xml) {
        return prettyXml(xml, "gb2312");
    }

    /**
     * xml美化工具
     * @author LonelySnow
     * @param xml
     * @param encoding
     * @return java.lang.String
     * @date 2023/2/13 09:30
     */
    public static String prettyXml(String xml, String encoding) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(new StringReader(xml));
            StringWriter sw = new StringWriter();

            // 格式化操作
            OutputFormat xmlFormat = OutputFormat.createPrettyPrint();
            if (encoding == null) {
                xmlFormat.setOmitEncoding(true);
            }
            // 设置编码
            xmlFormat.setEncoding(encoding);
            // 是否换行
            xmlFormat.setNewlines(true);
            // 是否生成缩进
            xmlFormat.setIndent(true);
            // 缩进数量
            xmlFormat.setIndent("    ");

            // 生成
            XMLWriter xmlWriter = new XMLWriter(sw, xmlFormat);
            xmlWriter.write(document);
            xmlWriter.close();

            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xml, e);
        }
    }

    // // // // // // //  具体已实现方法  // // // // // // // // //

    /**
     * 移除xml头文件，<?xml version="1.0" **** ?>
     */
    private boolean removeHeader;

    public XmlUtils() {
        this.setAttributeNamePrefix("_");
        this.setTextKey("text");
        this.setRemoveHeader(true);
    }

    public XmlUtils(String attributeNamePrefix, String textKey) {
        this(attributeNamePrefix, textKey, true);
    }

    public XmlUtils(String attributeNamePrefix, String textKey, boolean removeHeader) {
        this.attributeNamePrefix = attributeNamePrefix;
        this.textKey = textKey;
        this.removeHeader = removeHeader;
    }

    private String jsonToXmlSelf(String json) {
        JSONObject jsonObject = JSON.parseObject(json);
        Set<String> keySet = jsonObject.keySet();
        if (!keySet.isEmpty() && keySet.size() == 1) {
            // 将第一个节点name，作为根节点
            String rootName = keySet.iterator().next();
            Element rootElement = DocumentHelper.createElement(rootName);
            Element element = processObject(jsonObject.get(rootName), rootElement);
            String xml = DocumentHelper.createDocument(element).asXML();
            if (isRemoveHeader()) {
                xml = removeHeader(xml);
            }
            return xml;
        } else {
            throw new RuntimeException("The json text is not formatted correctly");
        }
    }

    private Element processObject(Object object, Element element) {
        if (object instanceof JSONObject) {
            return processJSONObject((JSONObject) object, element);
        } else if (object instanceof JSONArray) {
            return processJSONArray((JSONArray) object, element, element.getName());
        } else {
            return processText(object.toString(), element);
        }
    }

    private static Element processText(String text, Element element) {
        element.setText(text);
        return element;
    }

    private Element processJSONObject(JSONObject jsonObject, Element element) {
        jsonObject.forEach((key, value) -> {
            if (key.startsWith(getAttributeNamePrefix())) {
                element.addAttribute(key.substring(getPrefixLength()), value.toString());
            } else if (key.equals(getTextKey())) {
                element.setText(value.toString());
            } else {
                processValue(element, key, value);
            }
        });
        return element;
    }

    private void processValue(Element element, String name, Object value) {
        if (value instanceof JSONObject) {
            Element tempElement = processJSONObject((JSONObject) value, DocumentHelper.createElement(name));
            element.add(tempElement);
        } else if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            int size = jsonArray.size();
            for (int i = 0; i < size; i++) {
                processValue(element, name, jsonArray.get(i));
            }
        } else {
            Element temp = processText(value.toString(), DocumentHelper.createElement(name));
            element.add(temp);
        }
    }

    private Element processJSONArray(JSONArray jsonArray, Element root, String name) {
        int size = jsonArray.size();
        for (int i = 0; i < size; ++i) {
            processValue(root, name, jsonArray.get(i));
        }
        return root;
    }

    private String xmlToJsonSelf(Element element) {
        JSONObject json =  processObjectElement(element);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(element.getName(), json);
        return jsonObject.toJSONString();
    }

    private String xmlToJsonSelf(String xml) {
        try {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(new StringReader(xml));
            String lastXml = doc.asXML();
            Document document = DocumentHelper.parseText(lastXml);
            Element rootElement = document.getRootElement();
            return xmlToJsonSelf(rootElement);
        } catch (DocumentException e) {
            throw new RuntimeException("The XML text is not formatted correctly", e);
        }
    }

    private JSONObject processObjectElement(Element element) {
        if (element == null) {
            return new JSONObject();
        }
        JSONObject jsonObject = new JSONObject();

        List<Attribute> attributes = element.attributes();
        for (Attribute attribute : attributes) {
            String attributeName = getAttributeNamePrefix() + attribute.getName();
            String attributeValue = attribute.getValue();
            setOrAccumulate(jsonObject, attributeName, attributeValue);
        }

        int nodeCount = element.nodeCount();
        for (int i = 0; i < nodeCount; i++) {
            Node node = element.node(i);
            if (node instanceof Text) {
                Text text = (Text) node;
//                setOrAccumulate(jsonObject, getTextKey(), text.getText());
            } else if (node instanceof Element) {
                setValue(jsonObject, (Element) node);
            }
        }

        return jsonObject;
    }

    private void setValue(JSONObject jsonObject, Element element) {
        if (isObject(element)) {
            JSONObject elementJsonObj = processObjectElement(element);
            setOrAccumulate(jsonObject, element.getName(), elementJsonObj);
        } else {
            setOrAccumulate(jsonObject, element.getName(), element.getStringValue());
        }
    }

    private boolean isObject(Element element) {
        int attributeCount = element.attributeCount();
        if (attributeCount > 0) {
            return true;
        }

        int attrs = element.nodeCount();
        if (attrs == 1 && element.node(0) instanceof Text) {
            return false;
        } else {
            return true;
        }
    }

    private void setOrAccumulate(JSONObject jsonObject, String key, Object value) {
        if (jsonObject.containsKey(key)) {
            Object obj = jsonObject.get(key);
            if (obj instanceof JSONArray) {
                // 若为数组直接添加
                ((JSONArray) obj).add(value);
            } else {
                // 若为非数组，创建数组（已存在的值obj,待添加的值value）
                JSONArray jsonArray = new JSONArray();
                jsonArray.add(obj);
                jsonArray.add(value);
                jsonObject.put(key, jsonArray);
            }
        } else {
            jsonObject.put(key, value);
        }
    }

    /**
     * 移除xml报文头
     *
     * @param source
     * @return
     */
    private String removeHeader(String source) {
        return source.replaceFirst(reqEx, "")
                .replaceAll("\r|\n", "");
    }

    public String getAttributeNamePrefix() {
        return attributeNamePrefix;
    }

    public void setAttributeNamePrefix(String attributeNamePrefix) {
        this.attributeNamePrefix = attributeNamePrefix;
    }

    public String getTextKey() {
        return textKey;
    }

    public void setTextKey(String textKey) {
        this.textKey = textKey;
    }

    public boolean isRemoveHeader() {
        return removeHeader;
    }

    public void setRemoveHeader(boolean removeHeader) {
        this.removeHeader = removeHeader;
    }

    private int getPrefixLength() {
        return this.attributeNamePrefix.length();
    }

}
