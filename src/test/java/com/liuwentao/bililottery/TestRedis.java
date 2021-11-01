package com.liuwentao.bililottery;/** * Created by liuwentao on 2021/9/19 19:58 */import com.alibaba.fastjson.JSONObject;import com.liuwentao.bililottery.Service.IGlobalCache;import com.liuwentao.bililottery.redis.GetResponseConsumer;import com.liuwentao.bililottery.redis.GetResponseProducer;import com.liuwentao.bililottery.redis.RedisQueueMessage;import lombok.extern.slf4j.Slf4j;import okhttp3.Call;import okhttp3.OkHttpClient;import okhttp3.Request;import okhttp3.Response;import org.junit.Test;import org.junit.runner.RunWith;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.junit4.SpringRunner;import java.io.IOException;import java.net.InetSocketAddress;import java.net.Proxy;import java.util.concurrent.TimeUnit;@Slf4j@RunWith(SpringRunner.class)@SpringBootTestpublic class TestRedis {    @Autowired    private IGlobalCache globalCache;    @Autowired    GetResponseConsumer getResponseConsumer;    @Autowired    GetResponseProducer getResponseProducer;    @Test    public void test() {        globalCache.set("key2", new RedisQueueMessage("helooo"));//        globalCache.lSetAll("list", Arrays.asList("hello", "redis"));//        List<Object> list = globalCache.lGet("list", 0, -1);        log.info(((RedisQueueMessage)globalCache.get("key2")).getContent());        try {            Thread.sleep(5000);        } catch (InterruptedException e) {            e.printStackTrace();        }        log.info("获取代理结束，当前本地代理池中代理个数：" + globalCache.sGetSetSize("proxyPoolSet"));//        System.out.println(JSONObject.toJSONString(globalCache.get("key2")) + "jjjj");    }    @Test    public void testProvideAndCustomer() throws InterruptedException {        int x = 0;//        while (x++ < 10) {//        }        log.info("发送消息");        getResponseProducer.sendMessage(getResponseConsumer.getQueueName(), ("this is a url..."));        log.info("redis over....");        Thread.sleep(5000);    }    @Test    public void test3() {        String res = rawGetResponse("jj");        log.info("请求结果：" + res);        try {            Thread.sleep(6000);        } catch (InterruptedException e) {            e.printStackTrace();        }    }    public static String rawGetResponse(String url) {        url = "http://api.bilibili.com/x/web-interface/archive/stat?bvid=1Dy4y177st";        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("175.155.141.123", 1133));        OkHttpClient proxyRequestClient = new OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.SECONDS).proxy(proxy).build();        // 创建request请求        Request request = new Request.Builder().url(url).build();        Response response = null;        // 创建连接，调用同步        Call callProxy = proxyRequestClient.newCall(request);        try {            response = callProxy.execute();        } catch (IOException e) {            e.printStackTrace();            log.info("当前请求代理的url:" + url + "  rawGetResponse调用捕捉到异常：" + e.getMessage());        }        log.info("response:" + response);        if (response != null && response.isSuccessful()) {            try {                return response.body().string();            } catch (IOException e) {                e.printStackTrace();            }        }        return "";    }}