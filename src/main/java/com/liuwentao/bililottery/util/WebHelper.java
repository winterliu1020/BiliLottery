package com.liuwentao.bililottery.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liuwentao.bililottery.Configuration.SpringApplicationContextHolder;
import com.liuwentao.bililottery.Service.IGlobalCache;
import com.liuwentao.bililottery.mappers.LotteryResultMapper;
import com.liuwentao.bililottery.redis.GetResponseConsumer;
import com.liuwentao.bililottery.redis.GetResponseProducer;
import com.liuwentao.bililottery.redis.RedisQueueMessage;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.beans.factory.annotation.Autowired;
import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuwentao on 2021/5/11 17:08
 */

@Slf4j
public class WebHelper {

    private static OkHttpClient biliJumpRequestClient = new OkHttpClient().newBuilder().followRedirects(false).connectTimeout(5, TimeUnit.SECONDS).build();



    private static Object proxyListLock = new Object();


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

        String key = new Date().getTime() + ":" + url;

        // 加入redis后，需要修改逻辑：调用getResponse方法会把该url请求放到消息队列中，然后<key, 初始化值>放到redis的map中，然后不断的去请求map中key对应的value，看初始值有没有发生变化，变化了则说明已经调用b站api并得到了一个返回
        // 用生产者去生产；消费者在程序运行起来后就不断地去消息队列中取消息
        // 注入生产者、消费者
        GetResponseProducer getResponseProducer = SpringApplicationContextHolder.getBean(GetResponseProducer.class);
        GetResponseConsumer getResponseConsumer = SpringApplicationContextHolder.getBean(GetResponseConsumer.class);
        IGlobalCache iGlobalCache = SpringApplicationContextHolder.getBean(IGlobalCache.class);

        // 生产
        getResponseProducer.sendMessage(getResponseConsumer.getQueueName(), key); // 将一个代表url请求的key值放到消息队列中

        // 将一个键值对放到redis的map中，然后去轮询这个map，看初始值有无变化，有变化说明B站接口返回了值
        // 用请求url+时间戳 构造key

        iGlobalCache.set(key, new String("initValue")); // 消费者消费完之后会将消费结果放到redis中，接下来我通过不断轮询看是否已经拿到结果

        int reTransportTimes = 1; // 设置重传次数1次
        while (reTransportTimes++ <= 1) {
            int tryTime = 1;
            while (tryTime++ <= 20) {
                // 每秒查看一次redis
                String value = iGlobalCache.get(key).toString();
                if (!"initValue".equals(value)) {
                    // 说明B站已经返回了数据，但是还要看是否是返回正常数据
                    log.info("对于当前url" + url + " ,b站返回了结果");
//                    log.info("对于当前url" + url + " ,b站返回了结果：" + value);

                    // 如果当前是判断关注者的url，且返回的code是22115，说明该用户设置了隐私，所以也不需要往下面用别的查看数据了
                    if (url.startsWith("https://api.bilibili.com/x/relation/followings") && "PrivacyIsSet".equals(value)) {
                        return "PrivacyIsSet";
                    }

                    // 否则的话看是否返回code:0开头的数据，是的话说明是我们想要的数据
                    if (value.startsWith(chkPrefix)) {
//                        log.info("当前url" + url + " ,b站返回了正常的数据结果，getResponse请求正常结束：" + value);
                        log.info("当前url" + url + " ,b站返回了正常的数据结果，getResponse请求正常结束：");
                        return value;
                    }
                }

                log.info("当前url请求是：" + url + "，尝试第 " + reTransportTimes + " 次超时重传，第 " + tryTime + " 次查看redis结果集没有得到处理结果，准备休眠一秒后再试");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            log.info("当前url" + url + " ，20次查看redis都没有得到b站返回的结果，开始执行第" + reTransportTimes + " 次重传（重新放到消息队列）");

        }
        log.info("当前url" + url + " 一次重传失败，返回网络错误提示，让用户稍后重试");
        return "";

    }




}
