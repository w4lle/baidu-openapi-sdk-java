/**
 * Copyright (c) 2011 Baidu.com, Inc. All Rights Reserved
 */
package com.baidu.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import com.baidu.api.domain.BatchRunRequest;
import com.baidu.api.utils.BaiduUtil;
import com.baidu.api.utils.HttpUtil;
import com.baidu.api.utils.StringUtil;

/**
 * openapi调用客户端类， 封装了进行调用的基本操作
 * 
 * @author chenhetong(chenhetong@baidu.com)
 * 
 */
public class BaiduApiClient {

    //get请求常量
    public static final String METHOD_GET = "GET";

    //post请求常量
    public static final String METHOD_POST = "POST";

    //请求链接过期时间
    public static final int CONNECTTIMEOUT = 5000;

    //请求读取时间超时
    public static final int READTIMEOUT = 5000;

    //rest类型的api调用
    public static final String REST = "rest";

    //file类型的api调用
    public static final String FILE = "file";

    //public类型的api调用
    public static final String PUBLIC = "public";

    //BatchRun的请求将按照串行顺序执行
    public static final int BATCH_MODE_SERIAL_ONLY = 1;

    //BatchRun的请求将按照并行顺序执行
    public static final int BATCH_MODE_SERVER_PARALLEL = 0;

    private static final Map<String, String> httpRequestBaseMap = new HashMap<String, String>();

    private static final Map<String, String> httpsRequestBaseMap = new HashMap<String, String>();

    static {

        httpsRequestBaseMap.put(REST, "https://openapi.baidu.com/rest/2.0/");
        httpsRequestBaseMap.put(FILE, "https://openapi.baidu.com/file/2.0/");
        httpsRequestBaseMap.put(PUBLIC, "https://openapi.baidu.com/public/2.0/");
        httpRequestBaseMap.put(REST, "http://openapi.baidu.com/rest/2.0/");
        httpRequestBaseMap.put(FILE, "http://openapi.baidu.com/file/2.0/");
        httpRequestBaseMap.put(PUBLIC, "http://openapi.baidu.com/public/2.0/");

    }

    private String httpRequestBase;

    private String httpsRequestBase;

    private String accessToken;

    private String sessionKey;

    private String sessionSecret;

    private boolean isHttpsRequest;

    /**
     * 创建openapi的调用实例，使用Http的方式访问api
     * 
     * @param sessionSecret 基于http调用Open API时所需要的访问授权码
     * @param sessionSey 基于http调用Open API时计算参数签名用的签名密钥。
     */
    public BaiduApiClient(String sessionSecret, String sessionKey) {
        this.sessionKey = sessionKey;
        this.sessionSecret = sessionSecret;
        this.isHttpsRequest = false;
    }

    /**
     * 创建openapi的调用实例，使用Https的方法访问api
     * 
     * @param accessToken 基于https调用Open API时所需要的访问授权码
     */
    public BaiduApiClient(String accessToken) {
        this.accessToken = accessToken;
        this.isHttpsRequest = true;
    }

    /**
     * 批量处理用户的请求信息，减少网络请求，方法执行结束之后，会遍历
     * queue队列中的各个Request对象,将各个api请求的返回值回填到response属性中
     * 
     * @param queue BatchRunRequest对象集合，集合上限为10
     * @param serialOnly batchRun请求的执行方式，串行或者并行(
     *        BATCH_MODE_SERIAL_ONLY常量或BATCH_MODE_SERVER_PARALLEL常量)
     * @throws BaiduApiException
     * @throws IOException 请求api获取response时发生IOException
     */
    @SuppressWarnings("unchecked")
    public void batchRun(List<BatchRunRequest> queue, int serialOnly) throws BaiduApiException,
            IOException {
        if (queue.size() > 10) {
            return;
        }
        String url = "https://openapi.baidu.com/batch/run";
        JSONArray array = new JSONArray();
        Map<String, String> params = new HashMap<String, String>();
        for (BatchRunRequest batchRunRequest : queue) {
            array.add(batchRunRequest.getBatchRunParam());
        }
        if (serialOnly != 1) {
            serialOnly = 0;
        }
        params.put("serial_only", String.valueOf(serialOnly));
        params.put("access_token", this.accessToken);
        params.put("method", array.toJSONString());
        String response = null;
        response = HttpUtil.doPost(url, params, CONNECTTIMEOUT, READTIMEOUT);
        if (response.contains("error")) {
            throw new BaiduApiException(response);
        }
        //修改batchlist中的请求
        JSONArray responseJson = (JSONArray) JSONValue.parse(response);
        for (int i = 0; i < responseJson.size(); i++) {
            queue.get(i).setResponse(responseJson.get(i).toString());
        }
    }

    /**
     * 调用OpenApi的接口方法
     * 
     * @param apiPath 需要调用api的url路径 参考developer.baidu.com 上的关于OpenApi文档
     * @param apiType api调用的类型，包含rest、file、public三种类型
     * @param params 只需传递系统级非必须参数（format和callback）和应用级参数
     *        详细参考developer.baidu.com
     * @return Json形式的响应数据
     */
    public String requestRest(String apiPath, String apiType, Map<String, String> params,
            String method) throws BaiduApiException {
        if (this.isHttpsRequest) {
            return httpsRequestRest(apiPath, apiType, params, method);
        } else {
            return httpRequestRest(apiPath, apiType, params, method);
        }
    }

    /**
     * 通过https方式调用openApi，
     * 
     * @param apiPath 需要调用api的url路径 参考developer.baidu.com 上的关于OpenApi文档。
     * @param apiType api调用的类型，包含rest、file、public三种类型
     * @param params 只需传递系统级非必须参数和应用级参数 详细参考developer.baidu.com
     * @param method api调用的方法 get、post
     * @return Json格式的响应数据。
     */
    private String httpsRequestRest(String apiPath, String apiType,
            Map<String, String> requestParams, String method) throws BaiduApiException {
        Map<String, String> params = new HashMap<String, String>();
        httpsRequestBase = httpsRequestBaseMap.get(apiType);
        String url = httpsRequestBase + apiPath;
        params.put("access_token", this.accessToken);
        if (requestParams != null && !requestParams.isEmpty()) {
            params.putAll(requestParams);
        }
        String response = "";
        if (StringUtil.isEmpty(method) || method.equalsIgnoreCase(METHOD_GET)) {
            try {
                response = HttpUtil.doGet(url, params);
            } catch (IOException e) {}
        } else {
            try {
                response = HttpUtil.doPost(url, params, CONNECTTIMEOUT, READTIMEOUT);
            } catch (IOException e) {}
        }
        if (response.contains("error")) {
            throw new BaiduApiException(response);
        }
        return response;
    }

    /**
     * 通过http方式调用openApi，
     * 
     * @param apiPath 需要调用api的url路径 参考developer.baidu.com 上的关于OpenApi文档。
     * @param apiType api调用的类型，包含rest、file、public三种类型
     * @param params 只需传递系统级非必须参数（format和callback）和应用级参数
     *        详细参考developer.baidu.com
     * @param method api调用的方法 get、post
     * @return Json格式的数据。
     */
    private String httpRequestRest(String apiPath, String apiType,
            Map<String, String> requestParams, String method) throws BaiduApiException {
        httpRequestBase = httpRequestBaseMap.get(apiType);
        String url = httpRequestBase + apiPath;
        String sign = "";
        String response = "";
        Map<String, String> params = new HashMap<String, String>();
        params.put("session_key", this.sessionKey);
        params.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar
                .getInstance().getTime()));
        try {
            sign = BaiduUtil.getSignature(params, this.sessionSecret);
        } catch (IOException e) {}
        params.put("sign", sign);
        if (requestParams != null && !requestParams.isEmpty()) {
            params.putAll(requestParams);
        }
        if (StringUtil.isEmpty(method) || method.equalsIgnoreCase(METHOD_GET)) {
            try {
                response = HttpUtil.doGet(url, params);
            } catch (IOException e) {}
        } else {
            try {
                response = HttpUtil.doPost(url, params, CONNECTTIMEOUT, READTIMEOUT);
            } catch (IOException e) {}
        }
        if (response.contains("error")) {
            throw new BaiduApiException(response);
        }
        return response;
    }

}
