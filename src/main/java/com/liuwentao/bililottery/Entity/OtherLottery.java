package com.liuwentao.bililottery.Entity;

import lombok.Data;

import java.util.Date;

@Data
public class OtherLottery {
    private int id;
    private String src; // 抽奖地址
    private Date lotteryDateAndTime; // 开奖时间；
    private String price;
}
