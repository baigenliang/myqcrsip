package  com.rsvmcs.qcrsip.test.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;


import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author LonelySnow
 * @classname FastJsonUtils
 * @description Json解析工具类
 * @date 2022/9/16 14:59
 */
public class JsonUtils {

    /**
     * 内容转json
     * @author LonelySnow
     * @param obj
     * @result java.lang.String
     * @date 2021/8/7 11:42 上午
     */
    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass() == String.class) {
            return (String) obj;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            throw new RuntimeException(MessageFormat.format("Json序列化出错，错误内容{0}", e));
        }
    }

    /**
     * json转实体类
     * @author LonelySnow
     * @param json
     * @param tClass
     * @result T
     * @date 2021/8/7 11:44 上午
     */
    public static <T> T toBean(String json, Class<T> tClass) {
        try {
            return JSON.parseObject(json, tClass);
        } catch (Exception e) {
            throw new RuntimeException(MessageFormat.format("Json解析出错，错误内容{0}", e));
        }
    }

    /**
     * json转List
     * @author LonelySnow
     * @param json
     * @param tClass
     * @result T
     * @date 2021/8/7 11:46 上午
     */
    public static <T> List<T> toList(String json, Class<T> tClass) {
        try {
            return JSON.parseArray(json, tClass);
        } catch (Exception e) {
            throw new RuntimeException(MessageFormat.format("Json解析出错，错误内容{0}", e));
        }
    }

    /**
     * json转Map
     * @author LonelySnow
     * @param json
     * @param kClass
     * @param vClass
     * @result java.util.Map<K,V>
     * @date 2021/8/7 11:49 上午
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> kClass, Class<V> vClass) {
        try {
            return (Map<K, V>) JSON.parseObject(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(MessageFormat.format("Json解析出错，错误内容{0}", e));
        }
    }

}
