package com.mzlhsc.orpl.common.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Http工具类
 */
public class HttpClientUtil {

    static final int TIMEOUT_MSEC = 5 * 1000;

    /**
     * 发送GET方式请求
     *
     * @param url      请求地址
     * @param paramMap 查询参数
     * @return 响应体，请求失败时返回空字符串
     */
    public static String doGet(String url, Map<String, String> paramMap) {
        String result = "";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URIBuilder builder = new URIBuilder(url);
            if (paramMap != null) {
                for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(builderRequestConfig());

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
                    result = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("发送GET请求失败: " + url, e);
        }
        return result;
    }

    /**
     * 发送POST方式请求（表单）
     *
     * @param url      请求地址
     * @param paramMap 表单参数
     * @return 响应体
     */
    public static String doPost(String url, Map<String, String> paramMap) throws IOException {
        String resultString = "";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            if (paramMap != null) {
                List<NameValuePair> paramList = new ArrayList<>();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            }
            httpPost.setConfig(builderRequestConfig());

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getEntity() != null) {
                    resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
            }
        }
        return resultString;
    }

    /**
     * 发送POST方式请求（JSON）
     *
     * @param url      请求地址
     * @param paramMap JSON 参数
     * @return 响应体
     */
    public static String doPost4Json(String url, Map<String, String> paramMap) throws IOException {
        String resultString = "";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            if (paramMap != null) {
                JSONObject jsonObject = new JSONObject();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    jsonObject.put(param.getKey(), param.getValue());
                }
                StringEntity entity = new StringEntity(jsonObject.toString(), "UTF-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }
            httpPost.setConfig(builderRequestConfig());

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getEntity() != null) {
                    resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
            }
        }
        return resultString;
    }

    private static RequestConfig builderRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MSEC)
                .setConnectionRequestTimeout(TIMEOUT_MSEC)
                .setSocketTimeout(TIMEOUT_MSEC).build();
    }
}
