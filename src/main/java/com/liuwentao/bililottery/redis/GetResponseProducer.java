package com.liuwentao.bililottery.redis;import com.liuwentao.bililottery.Service.IGlobalCache;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Component;/** * Created by liuwentao on 2021/9/27 13:01 */@Componentpublic class GetResponseProducer {    @Autowired    IGlobalCache iGlobalCache;    /**     * 发送消息     *     * @param queueName    队列名称     * @param redisQueueMessage 消息     */    public void sendMessage(String queueName, String redisQueueMessage) {        iGlobalCache.bLPush(queueName, redisQueueMessage);    }}