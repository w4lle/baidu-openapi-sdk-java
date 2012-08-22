/**
 * Copyright (c) 2011 Baidu.com, Inc. All Rights Reserved
 */
package com.baidu.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.baidu.api.domain.FileItem;

/**
 * 进行http访问的基本类
 * 
 * @author chenhetong(chenhetong@baidu.com)
 * 
 */
public class HttpUtil {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private static final String METHOD_POST = "POST";

    private static final String METHOD_GET = "GET";

    private static HttpURLConnection getConnection(URL url, String method, String ctype)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("User-Agent", "baidu-restclient-java-1.0");
        conn.setRequestProperty("Content-Type", ctype);
        return conn;

    }

    /**
     * 通过get方法访问，默认编码为utf-8
     * 
     * @param url 访问的url地址
     * @param params 请求需要的参数
     * @return 返回请求响应的数据
     * @throws IOException
     */
    public static String doGet(String url, Map<String, String> params) throws IOException {
        return doGet(url, params, DEFAULT_CHARSET);
    }

    /**
     * 通过get方法访问
     * 
     * @param url 访问的url地址
     * @param params 请求需要的参数
     * @param charset 字符编码
     * @return 返回请求响应的数据
     * @throws IOException
     */
    public static String doGet(String url, Map<String, String> params, String charset)
            throws IOException {
        if (StringUtil.isEmpty(url) || params == null) {
            return null;
        }
        String response = "";
        url += "?" + buildQuery(params, charset);
        HttpURLConnection conn = null;
        String ctype = "application/x-www-form-urlencoded;charset=" + charset;
        try {
            conn = getConnection(new URL(url), METHOD_GET, ctype);
            response = getResponseAsString(conn);
            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }

    }

    /**
     * 
     * 通过post方法请求数据，默认字符编码为utf-8
     * 
     * @param url 请求的url地址
     * @param params 请求的参数
     * @param connectTimeOut 请求连接过期时间
     * @param readTimeOut 请求读取过期时间
     * @return 请求响应
     * @throws IOException
     */
    public static String doPost(String url, Map<String, String> params, int connectTimeOut,
            int readTimeOut) throws IOException {
        return doPost(url, params, DEFAULT_CHARSET, connectTimeOut, readTimeOut);
    }

    /**
     * 
     * 通过post方法请求数据
     * 
     * @param url 请求的url地址
     * @param params 请求的参数
     * @param charset 字符编码格式
     * @param connectTimeOut 请求连接过期时间
     * @param readTimeOut 请求读取过期时间
     * @return 请求响应
     * @throws IOException
     */
    public static String doPost(String url, Map<String, String> params, String charset,
            int connectTimeOut, int readTimeOut) throws IOException {
        HttpURLConnection conn = null;
        String response = "";
        try {
            String ctype = "application/x-www-form-urlencoded;charset=" + charset;
            conn = getConnection(new URL(url), METHOD_POST, ctype);
            conn.setConnectTimeout(connectTimeOut);
            conn.setReadTimeout(readTimeOut);
            conn.getOutputStream().write(buildQuery(params, charset).getBytes(charset));
            response = getResponseAsString(conn);
            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
    }

    /**
     * 文件上传操作，默认编码为utf-8
     * 
     * @param url 请求的url地址
     * @param params 请求的参数
     * @param fileParams 文件上传的参数信息， key为文件的参数名称， value为FileItem对象
     * @param connectTimeOut url连接过期时间
     * @param readTimeOut 请求读取过期时间
     * @return 放回请求的回应信息
     * @throws IOException
     */
    public static String uploadFile(String url, Map<String, String> params,
            Map<String, FileItem> fileParams, int connectTimeOut, int readTimeOut)
            throws IOException {
        return uploadFile(url, params, fileParams, DEFAULT_CHARSET, connectTimeOut, readTimeOut);
    }

    public static String uploadFile(String url, Map<String, String> params,
            Map<String, FileItem> fileParams, String charset, int connectTimeOut, int readTimeOut)
            throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        String boundary = System.currentTimeMillis() + "";
        String response = "";
        String ctype = "multipart/form-data;charset=" + charset + ";boundary=" + boundary;
        try {
            conn = getConnection(new URL(url), METHOD_POST, ctype);
            conn.setConnectTimeout(connectTimeOut);
            conn.setReadTimeout(readTimeOut);
            out = conn.getOutputStream();
            byte[] entryBoundaryBytes = ("\r\n--" + boundary + "\r\n").getBytes(charset);
            //组装params
            if (params != null && !params.isEmpty()) {
                for (Entry<String, String> entry : params.entrySet()) {
                    byte[] textBytes = getTextEntry(entry.getKey(), entry.getValue(), charset);
                    out.write(entryBoundaryBytes);
                    out.write(textBytes);
                }
            }
            //组装files
            if (fileParams != null && !fileParams.isEmpty()) {
                for (Entry<String, FileItem> fileEntry : fileParams.entrySet()) {
                    String fieldParam = fileEntry.getKey();
                    FileItem fileItem = fileEntry.getValue();
                    byte[] fileBytes = getFileEntry(fieldParam, fileItem.getFileName(),
                            fileItem.getMimeType(), charset);
                    out.write(entryBoundaryBytes);
                    out.write(fileBytes);
                    out.write(fileItem.getContent());
                }
            }
            byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes(charset);
            out.write(endBoundaryBytes);
            response = getResponseAsString(conn);
            return response;
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
    }

    /**
     * 
     * @param params 请求参数
     * @return 构建query
     */
    public static String buildQuery(Map<String, String> params, String charset) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtil.areNotEmpty(key, value)) {
                try {
                    sb.append(key).append("=").append(URLEncoder.encode(value, charset));
                } catch (UnsupportedEncodingException e) {}
            }
        }
        return sb.toString();

    }

    public static Map<String, String> splitQuery(String query, String charset) {
        Map<String, String> ret = new HashMap<String, String>();
        if (!StringUtil.isEmpty(query)) {
            String[] splits = query.split("\\&");
            for (String split : splits) {
                String[] keyAndValue = split.split("\\=");
                if (StringUtil.areNotEmpty(keyAndValue) && keyAndValue.length == 2) {
                    try {
                        ret.put(keyAndValue[0], URLDecoder.decode(keyAndValue[1], charset));
                    } catch (UnsupportedEncodingException e) {}
                }
            }
        }
        return ret;
    }

    private static byte[] getTextEntry(String fieldName, String fieldValue, String charset)
            throws IOException {
        StringBuilder entry = new StringBuilder();
        entry.append("Content-Disposition:form-data;name=\"");
        entry.append(fieldName);
        entry.append("\"\r\nContent-Type:text/plain\r\n\r\n");
        entry.append(fieldValue);
        return entry.toString().getBytes(charset);
    }

    private static byte[] getFileEntry(String fieldName, String fileName, String mimeType,
            String charset) throws IOException {
        StringBuilder entry = new StringBuilder();
        entry.append("Content-Disposition:form-data;name=\"");
        entry.append(fieldName);
        entry.append("\";filename=\"");
        entry.append(fileName);
        entry.append("\"\r\nContent-Type:");
        entry.append(mimeType);
        entry.append("\r\n\r\n");
        return entry.toString().getBytes(charset);
    }

    private static String getResponseAsString(HttpURLConnection conn) throws IOException {
        String charset = getResponseCharset(conn.getContentType());
        InputStream es = conn.getErrorStream();
        if (es == null) {
            return getStreamAsString(conn.getInputStream(), charset);
        } else {
            String msg = getStreamAsString(es, charset);
            if (StringUtil.isEmpty(msg)) {
                throw new IOException(conn.getResponseCode() + " : " + conn.getResponseMessage());
            } else {
                throw new IOException(msg);
            }
        }

    }

    private static String getStreamAsString(InputStream input, String charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new InputStreamReader(input, charset));
            String str;
            while ((str = bf.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } finally {
            if (bf != null) {
                bf.close();
                bf = null;
            }
        }

    }

    private static String getResponseCharset(String ctype) {
        String charset = DEFAULT_CHARSET;
        if (!StringUtil.isEmpty(ctype)) {
            String[] params = ctype.split("\\;");
            for (String param : params) {
                param = param.trim();
                if (param.startsWith("charset")) {
                    String[] pair = param.split("\\=");
                    if (pair.length == 2) {
                        charset = pair[1].trim();
                    }
                }
            }
        }
        return charset;
    }

}
