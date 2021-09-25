package com.liuwentao.bililottery.ServiceImpl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.liuwentao.bililottery.Entity.LotteryResult;
import com.liuwentao.bililottery.Entity.Reply;
import com.liuwentao.bililottery.Service.LotteryResultService;
import com.liuwentao.bililottery.mappers.LotteryResultMapper;
import com.liuwentao.bililottery.util.Help;
import com.liuwentao.bililottery.util.WebHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by liuwentao on 2021/5/16 12:08
 */

@Slf4j
@Service
public class LotteryResultServiceImpl implements LotteryResultService {

    @Autowired
    LotteryResultMapper lotteryResultMapper;



    @Override
    public int insertLotteryResult(List<LotteryResult> lotteryResultList) {
        return lotteryResultMapper.insertLotteryResult(lotteryResultList);
    }

    @Override
    public List<LotteryResult> findLotteryResultByOpenIdAndPatternId(String openId, String patternId) {
        return lotteryResultMapper.findLotteryResultByOpenIdAndPatternId(openId, patternId);
    }

    @Override
    public int deleteLotteryResult(String openId, String patternId) {
        return lotteryResultMapper.deleteLotteryResult(openId, patternId);
    }


}
