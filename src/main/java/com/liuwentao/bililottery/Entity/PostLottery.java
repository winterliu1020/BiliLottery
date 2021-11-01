package com.liuwentao.bililottery.Entity;import lombok.AllArgsConstructor;import lombok.Builder;import lombok.Data;import java.util.Date;/** * Created by liuwentao on 2021/9/17 23:32 */@Data@Builder@AllArgsConstructorpublic class PostLottery {    private String openId; // 谁发布的这条抽奖    private String upId; // up主的uid    private String id; // bvid|777    private Integer count; // 该条抽奖抽几个人    private String prizeNotes; // 奖品备注    private Integer lotteryType; // 0-仅评论；1-评论+关注；2-支持者模式    private boolean unlimitedStart; // 是否限制开始时间    private boolean unlimitedEnd; // 是否限制截止时间    private Date start;    private Date end;    private boolean getStart;    private boolean letEnd;    private boolean duplicatedUid;    private boolean onlySpecified;    private String contentSpecified;    private String out; // 记录这次抽奖最终的结果    private boolean timeLottery; // 是否是定时抽奖    private Date lotteryDateAndTime; // 开奖时间；即刻抽奖也有开奖时间，都得存起来//    public PostLottery(){}}