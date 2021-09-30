package com.liuwentao.bililottery.ServiceImpl;

import com.liuwentao.bililottery.Entity.OtherLottery;
import com.liuwentao.bililottery.Service.OtherLotteryService;
import com.liuwentao.bililottery.mappers.OtherLotteryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class OtherLotteryServiceImpl implements OtherLotteryService {
    @Autowired
    private OtherLotteryMapper otherLotteryMapper;
    @Override
    public List<OtherLottery> getAllOtherLottery(String order) {
        return otherLotteryMapper.findAllOtherLottery(order);
    }

    @Override
    public List<OtherLottery> getNewOtherLottery(String order) {
        return otherLotteryMapper.findNewOtherLottery(order);
    }
}
