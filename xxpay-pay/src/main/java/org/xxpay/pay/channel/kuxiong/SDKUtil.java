package org.xxpay.pay.channel.kuxiong;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;


public class SDKUtil {

    private static String toHex(byte[] bytes) {

        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (int i=0; i<bytes.length; i++) {
            ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
        }
        return ret.toString();
    }


    public static String MD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes("utf-8"));
            return toHex(bytes).toLowerCase();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSign(Map<String,Object> post,String key) {
        String returnstr="";
        Map<String, Object> map = sortMapByKey(post);
        Iterator<Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();
            returnstr+="&"+entry.getKey() + "=" + entry.getValue();
        }
        returnstr = returnstr.substring(1);
        return MD5(returnstr+"&key="+key).toLowerCase();
    }

    public static Map<String, Object> sortMapByKey(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        Map<String, Object> sortMap = new TreeMap<String, Object>(
                new MapKeyComparator());

        sortMap.putAll(map);

        return sortMap;
    }

    /**
     * 将Map中的数据转换成按照Key的ascii码排序后的key1=value1&key2=value2的形式 不包含签名域signature
     * 
     * @param data 待拼接的Map数据
     * @return 拼接好后的字符串
     */
    public static String coverMap2String(Map<String, String> data, String signKey) {
        TreeMap<String, String> tree = new TreeMap<String, String>();
        Iterator<Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> en = it.next();
            if (signKey.equals(en.getKey().trim())) {
                continue;
            }
            tree.put(en.getKey(), en.getValue());
        }
        it = tree.entrySet().iterator();
        StringBuffer sf = new StringBuffer();
        while (it.hasNext()) {
            Entry<String, String> en = it.next();
            sf.append(en.getKey() + "=" + en.getValue() + "&");
        }
        return sf.substring(0, sf.length() - 1);
    }

    /**
     * 兼容老方法 将形如key=value&key=value的字符串转换为相应的Map对象
     * 
     * @param result
     * @return
     */
    public static Map<String, String> coverResultString2Map(String result) {
        return convertResultStringToMap(result);
    }

    /**
     * 将形如key=value&key=value的字符串转换为相应的Map对象
     * 
     * @param result
     * @return
     */
    public static Map<String, String> convertResultStringToMap(String result) {
        Map<String, String> map = null;
        try {

            if (StringUtils.isNotBlank(result)) {
                if (result.startsWith("{") && result.endsWith("}")) {
                    System.out.println(result.length());
                    result = result.substring(1, result.length() - 1);
                }
                map = parseQString(result);
            }

        } catch (UnsupportedEncodingException e) {
        }
        return map;
    }

    /**
     * 解析应答字符串，生成应答要素
     * 
     * @param str 需要解析的字符串
     * @return 解析的结果map
     * @throws UnsupportedEncodingException
     */
    public static Map<String, String> parseQString(String str) throws UnsupportedEncodingException {

        Map<String, String> map = new HashMap<String, String>();
        int len = str.length();
        StringBuilder temp = new StringBuilder();
        char curChar;
        String key = null;
        boolean isKey = true;
        boolean isOpen = false;// 值里有嵌套
        char openName = 0;
        if (len > 0) {
            for (int i = 0; i < len; i++) {// 遍历整个带解析的字符串
                curChar = str.charAt(i);// 取当前字符
                if (isKey) {// 如果当前生成的是key

                    if (curChar == '=') {// 如果读取到=分隔符
                        key = temp.toString();
                        temp.setLength(0);
                        isKey = false;
                    } else {
                        temp.append(curChar);
                    }
                } else {// 如果当前生成的是value
                    if (isOpen) {
                        if (curChar == openName) {
                            isOpen = false;
                        }

                    } else {// 如果没开启嵌套
                        if (curChar == '{') {// 如果碰到，就开启嵌套
                            isOpen = true;
                            openName = '}';
                        }
                        if (curChar == '[') {
                            isOpen = true;
                            openName = ']';
                        }
                    }
                    if (curChar == '&' && !isOpen) {// 如果读取到&分割符,同时这个分割符不是值域，这时将map里添加
                        putKeyValueToMap(temp, isKey, key, map);
                        temp.setLength(0);
                        isKey = true;
                    } else {
                        temp.append(curChar);
                    }
                }

            }
            putKeyValueToMap(temp, isKey, key, map);
        }
        return map;
    }

    private static void putKeyValueToMap(StringBuilder temp, boolean isKey, String key,
                                         Map<String, String> map) throws UnsupportedEncodingException {
        if (isKey) {
            key = temp.toString();
            if (key.length() == 0) {
                throw new RuntimeException("QString format illegal");
            }
            map.put(key, "");
        } else {
            if (key.length() == 0) {
                throw new RuntimeException("QString format illegal");
            }
            map.put(key, temp.toString());
        }
    }

    /**
     * 判断字符串是否为NULL或空
     * 
     * @param s 待判断的字符串数据
     * @return 判断结果 true-是 false-否
     */
    public static boolean isEmpty(String s) {
        return null == s || "".equals(s.trim());
    }

    /**
     * 过滤请求报文中的空字符串或者空字符串
     * 
     * @param contentData
     * @return
     */
    public static Map<String, String> filterBlank(Map<String, String> contentData) {
        Map<String, String> submitFromData = new HashMap<String, String>();
        Set<String> keyset = contentData.keySet();

        for (String key : keyset) {
            String value = contentData.get(key);
            if (StringUtils.isNotBlank(value)) {
                // 对value值进行去除前后空处理
                submitFromData.put(key, value.trim());
            }
        }
        return submitFromData;
    }
}

class MapKeyComparator implements Comparator<String> {

    public int compare(String str1, String str2) {
        return str1.compareTo(str2);
    }
}