package com.liuwentao.bililottery.Service;

import com.liuwentao.bililottery.Entity.LotteryResult;
import com.liuwentao.bililottery.Entity.Reply;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by liuwentao on 2021/5/16 11:41
 */
public interface LotteryResultService {
//    ArrayList<Reply> getList(String id, int count, boolean unlimitedStart, boolean unlimitedEnd, Date start, Date end,
//                                    boolean getStart, boolean letEnd, boolean duplicatedUid, boolean onlySpecified, String contentSpecified,
//                                    StringBuilder out);

    // 将抽奖结果插入数据库
    int insertLotteryResult(List<LotteryResult> lotteryResultList);



    List<LotteryResult> findLotteryResultByOpenIdAndPatternId(String openId, String patternId);

    int deleteLotteryResult(String openId, String patternId);
}
