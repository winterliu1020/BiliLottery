package com.liuwentao.bililottery.Entity;import lombok.Builder;import lombok.Data;import java.util.List;/** * Created by liuwentao on 2021/9/30 11:59 */@Data@Builderpublic class LotteryResultAndPrizeName {    private String prizeName;    private List<LotteryResult> lotteryResult; // 该奖项对应的中奖名单}