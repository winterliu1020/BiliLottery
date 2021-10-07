package com.liuwentao.bililottery.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuwentao on 2021/5/11 17:08
 */

@Slf4j
public class WebHelper {

    private static OkHttpClient biliJumpRequestClient = new OkHttpClient().newBuilder().followRedirects(false).connectTimeout(5, TimeUnit.SECONDS).build();
    private static OkHttpClient biliRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).build();
    private static OkHttpClient proxyRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).build();

    private static Object biliRequestLock = new Object();
    private static Object proxyListLock = new Object();
    private static String proxyApiUrl = "https://ip.jiangxianli.com/api/proxy_ips?order_by=validated_at&country=中国&order_rule=DESC";
    // 获取b站分享链接重定向跳转的目标URL
    public static String getRedirect(String url) {
        // 循环（获取代理列表、判断是否可用、prefix是正确）
        // 完成正确的响应则返回response body，否则返回""
        int tryTimes = 5;
        while (tryTimes-- > 0) {
            // 创建request请求
            Request request = new Request.Builder().url(url).build();
            Response myResponse = null;
            // 创建连接请求，调用同步
            Call call = biliJumpRequestClient.newCall(request);
            try {
                Response response = call.execute();
                if (response.isRedirect()) {
                    return response.header("Location");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    // 获取b站API的响应内容直到响应首字符串与ChkPrefix相同
    public static String getResponse(String url, String chkPrefix) {


        String biliResponse = "";
        // 对整个请求过程加锁，防止重复实例化httpclient
        synchronized (biliRequestLock) {
            biliResponse = rawGetResponse(url, true); // 不变，如果当前网络环境

            // 如果当前是判断关注者的url，且返回的code是22115，说明该用户设置了隐私，所以也不需要往下面用别的查看数据了
            if (url.startsWith("https://api.bilibili.com/x/relation/followings") && biliResponse.startsWith("{\"code\":22115,")) {
                return "PrivacyIsSet";
            }


            // B站接口无法返回数据通过校验
            if (!biliResponse.startsWith(chkPrefix)) {
                // 先尝试无代理的httpclient
                log.info("先尝试无代理的httpclient去请求：" + url);
                biliRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).build();
                biliResponse = rawGetResponse(url, true);
//                log.info("尝试无代理的httpclient得到的结果：" + biliResponse);

                // 如果当前是判断关注者的url，且返回的code是22115，说明该用户设置了隐私，所以也不需要往下面用别的查看数据了
                if (url.startsWith("https://api.bilibili.com/x/relation/followings") && biliResponse.startsWith("{\"code\":22115,")) {
                    return "PrivacyIsSet";
                }

                // 无代理的httpclient无法请求到数据，使用代理池
                if (!biliResponse.startsWith(chkPrefix)) {
                    log.info("无代理的httpclient无法请求到数据，开始使用代理池");
                    int page = 1;
                    while (true) {
                        ArrayList<Proxy> proxyList = getProxyList(page++);
                        if (proxyList.size() == 0) {
                            biliRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).build();
                            biliResponse = rawGetResponse(url, true);

                            // 如果当前是判断关注者的url，且返回的code是22115，说明该用户设置了隐私，所以也不需要往下面用别的查看数据了
                            if (url.startsWith("https://api.bilibili.com/x/relation/followings") && biliResponse.startsWith("{\"code\":22115,")) {
                                return "PrivacyIsSet";
                            }

                        } else {
                            boolean availableProxy = false;
                            for (Proxy proxy : proxyList) {
                                // 替换biliRequestClient
                                log.info("当前使用的代理池：" + proxy.toString());
                                biliRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).proxy(proxy).build();
                                // 用替换后的替换biliRequestClient请求B站接口
                                biliResponse = rawGetResponse(url, true);

                                // 如果当前是判断关注者的url，且返回的code是22115，说明该用户设置了隐私，所以也不需要往下面用别的查看数据了
                                if (url.startsWith("https://api.bilibili.com/x/relation/followings") && biliResponse.startsWith("{\"code\":22115,")) {
                                    return "PrivacyIsSet";
                                }

                                if (biliResponse.startsWith(chkPrefix)) {
                                    log.info("当前代理成功获取到B站数据");
                                    availableProxy = true;
                                    break;
                                } else {
                                    log.info("当前代理获取到B站数据失败，尝试代理池中下一个代理");
                                }
                            }
                            if (availableProxy) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return biliResponse;
    }

    // 获取代理池代理：这里用的是别人汇总的一些节点；也可以采用爬取的方式：https://www.cnblogs.com/xinxihua/p/14541247.html
    public static ArrayList<Proxy> getProxyList(int page) {
        ArrayList<Proxy> proxyArrayList = new ArrayList<>();
        // 代理池API首页，获取最大页数
        String firstPageContent = rawGetResponse(proxyApiUrl, false);
        if (!"".equals(firstPageContent)) {
            Map mapTypes = JSON.parseObject(firstPageContent);
//            System.out.println("这个是用JSON类的parseObject来解析JSON字符串!!!");
//            for (Object obj : mapTypes.keySet()){
//                System.out.println("key为："+obj+"值为："+mapTypes.get(obj).getClass().getName());
//            }
            JSONObject firstPageData = (JSONObject) mapTypes.get("data");
            int lastPage = (Integer) firstPageData.get("last_page");
            if (page <= lastPage) {
                String pageContent = rawGetResponse(proxyApiUrl + "&page=" + page, false);
                if (!"".equals(pageContent)) {
                    Map contentParsed = JSON.parseObject(firstPageContent);
                    JSONObject pageData = (JSONObject) contentParsed.get("data");
                    JSONArray proxyJsonArray = (JSONArray) pageData.get("data");
                    for (Object jsonObject : proxyJsonArray) {
                        Map tempMap = JSON.parseObject(jsonObject.toString());
                        log.info("获取到的代理：" + tempMap.get("protocol") + "  " + tempMap.get("ip") + "  " + tempMap.get("port"));
                        if (tempMap.get("protocol").toString().equals("http")) { // 只会把http代理加入到proxyArrayList中
                            proxyArrayList.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(tempMap.get("ip").toString(), Integer.parseInt(tempMap.get("port").toString()))));
                        }
                    }
                }
            }
        }
        return proxyArrayList;
    }

    // 返回response中整个body字符串；通过传入biliApi的true or false来控制发送哪一个request
    public static String rawGetResponse(String url, boolean biliApi) {
        // 创建request请求
        Request request = new Request.Builder().url(url).build();
        Response response = null;

        if (biliApi) {
            // 创建连接，调用同步
            Call callBili = biliRequestClient.newCall(request);
            try {
                response = callBili.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // 加锁防止并发访问代理服务器
            synchronized (proxyListLock) {
                // 创建连接，调用同步
                Call callProxy = proxyRequestClient.newCall(request);
                try {
                    response = callProxy.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (response != null && response.isSuccessful()) {
            try {
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
