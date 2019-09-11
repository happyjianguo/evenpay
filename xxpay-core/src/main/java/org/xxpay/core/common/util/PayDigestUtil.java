package org.xxpay.core.common.util;

import com.alibaba.fastjson.JSONObject;
import sun.misc.BASE64Decoder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * @Description:
 * @author dingzhiwei jmdhappy@126.com
 * @date 2017-07-05
 * @version V1.0
 * @Copyright: www.xxpay.org
 */
public class PayDigestUtil {

	private static final MyLog _log = MyLog.getLog(PayDigestUtil.class);
	private static String encodingCharset = "UTF-8";
	
	/**
	 * @param aValue
	 * @param aKey
	 * @return
	 */
	public static String hmacSign(String aValue, String aKey) {
		byte k_ipad[] = new byte[64];
		byte k_opad[] = new byte[64];
		byte keyb[];
		byte value[];
		try {
			keyb = aKey.getBytes(encodingCharset);
			value = aValue.getBytes(encodingCharset);
		} catch (UnsupportedEncodingException e) {
			keyb = aKey.getBytes();
			value = aValue.getBytes();
		}

		Arrays.fill(k_ipad, keyb.length, 64, (byte) 54);
		Arrays.fill(k_opad, keyb.length, 64, (byte) 92);
		for (int i = 0; i < keyb.length; i++) {
			k_ipad[i] = (byte) (keyb[i] ^ 0x36);
			k_opad[i] = (byte) (keyb[i] ^ 0x5c);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {

			return null;
		}
		md.update(k_ipad);
		md.update(value);
		byte dg[] = md.digest();
		md.reset();
		md.update(k_opad);
		md.update(dg, 0, 16);
		dg = md.digest();
		return toHex(dg);
	}

	public static String toHex(byte input[]) {
		if (input == null)
			return null;
		StringBuffer output = new StringBuffer(input.length * 2);
		for (int i = 0; i < input.length; i++) {
			int current = input[i] & 0xff;
			if (current < 16)
				output.append("0");
			output.append(Integer.toString(current, 16));
		}

		return output.toString();
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

	public static String getRSASign(Map<String, Object> post, String key) throws Exception {
		String returnstr = "";
		Map<String, Object> map = sortMapByKey(post);
		Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			returnstr += "&" + entry.getKey() + "=" + entry.getValue();
		}
		returnstr = returnstr.substring(1);

		return getMD5withRSASign(returnstr,key);
	}

	// 用md5生成内容摘要，再用RSA的私钥加密，进而生成数字签名
	public static String getMD5withRSASign(String content, String key) throws Exception {
		PrivateKey privateKey = getPrivateKey(key);
		byte[] contentBytes = content.getBytes("utf-8");
		// 返回MD5withRSA签名算法的 Signature对象
		Signature signature = Signature.getInstance("MD5withRSA");
		signature.initSign(privateKey);
		signature.update(contentBytes);
		byte[] signs = signature.sign();
		return org.apache.commons.codec.binary.Base64.encodeBase64String(signs);
	}

	public static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = (new BASE64Decoder()).decodeBuffer(key);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}

	/**
	 * 
	 * @param args
	 * @param key
	 * @return
	 */
	public static String getHmac(String[] args, String key) {
		if (args == null || args.length == 0) {
			return (null);
		}
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < args.length; i++) {
			str.append(args[i]);
		}
		return (hmacSign(str.toString(), key));
	}

	/**
	 * @param aValue
	 * @return
	 */
	public static String digest(String aValue) {
		aValue = aValue.trim();
		byte value[];
		try {
			value = aValue.getBytes(encodingCharset);
		} catch (UnsupportedEncodingException e) {
			value = aValue.getBytes();
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return toHex(md.digest(value));

	}
	
	public static String md5(String value, String charset) {
		MessageDigest md = null;
		try {
			byte[] data = value.getBytes(charset);
			md = MessageDigest.getInstance("MD5");
			byte[] digestData = md.digest(data);
			return toHex(digestData);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getSign(Object o, String key) throws IllegalAccessException {
		if(o instanceof Map) {
			return getSign((Map<String, Object>)o, key);
		}
		ArrayList<String> list = new ArrayList<String>();
		Class cls = o.getClass();
		Field[] fields = cls.getDeclaredFields();
		for (Field f : fields) {
			f.setAccessible(true);
			if (f.get(o) != null && f.get(o) != "") {
				list.add(f.getName() + "=" + f.get(o) + "&");
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		result += "key=" + key;
		_log.debug("Sign Before MD5:" + result);
		result = md5(result, encodingCharset).toUpperCase();
		_log.debug("Sign Result:" + result);
		return result;
	}

	public static String getSortJson(JSONObject obj){
		SortedMap map = new TreeMap();
		Set<String> keySet = obj.keySet();
		Iterator<String> it = keySet.iterator();
		while (it.hasNext()) {
			String key = it.next().toString();
			Object vlaue = obj.get(key);
			map.put(key, vlaue);
		}
		return JSONObject.toJSONString(map);
	}

	public static String getSign(Map<String,Object> map, String key){
		ArrayList<String> list = new ArrayList<String>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			if(null != entry.getValue() && !"".equals(entry.getValue())){
				if(entry.getValue() instanceof JSONObject) {
					list.add(entry.getKey() + "=" + getSortJson((JSONObject) entry.getValue()) + "&");
				}else {
					list.add(entry.getKey() + "=" + entry.getValue() + "&");
				}
			}
		}
		int size = list.size();
		String [] arrayToSort = list.toArray(new String[size]);
		Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i ++) {
			sb.append(arrayToSort[i]);
		}
		String result = sb.toString();
		result += "key=" + key;
		_log.info("Sign Before MD5:" + result);
		result = md5(result, encodingCharset).toUpperCase();
		_log.info("Sign Result:" + result);
		return result;
	}

	/**
	 *
	 * @param map
	 * @param key
	 * @param notContains 不包含的签名字段
     * @return
     */
	public static String getSign(Map<String,Object> map, String key, String... notContains){
		Map<String,Object> newMap = new HashMap<String,Object>();
		for(Map.Entry<String,Object> entry:map.entrySet()){
			boolean isContain = false;
			for(int i=0; i<notContains.length; i++) {
				if(entry.getKey().equals(notContains[i])) {
					isContain = true;
					break;
				}
			}
			if(!isContain) {
				newMap.put(entry.getKey(), entry.getValue());
			}
		}
		return getSign(newMap, key);
	}

	public static void main(String[] args) {
		String key = "8UPp0KE8sq73zVP370vko7C39403rtK1YwX40Td6irH216036H27Eb12792t";
		String dataStr = "AnnulCard1000043252120080620160450.0http://localhost/SZXpro/callback.asp这4564868265473632445648682654736324511";
		System.out.println(hmacSign(dataStr, key));
		
		System.out.println(md5(dataStr, "UTF-8"));
		System.out.println(md5(dataStr, "GBK"));
	}
}

class MapKeyComparator implements Comparator<String> {

	public int compare(String str1, String str2) {
		return str1.compareTo(str2);
	}
}