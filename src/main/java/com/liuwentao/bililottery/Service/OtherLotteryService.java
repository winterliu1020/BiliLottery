package com.liuwentao.bililottery.Service;

import com.liuwentao.bililottery.Entity.OtherLottery;

import java.util.List;

public interface OtherLotteryService {
    List<OtherLottery> getAllOtherLottery(String order);
    List<OtherLottery> getNewOtherLottery(String order);
}
