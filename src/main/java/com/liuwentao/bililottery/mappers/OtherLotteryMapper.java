package com.liuwentao.bililottery.mappers;

import com.liuwentao.bililottery.Entity.OtherLottery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtherLotteryMapper {
    //根据order排序, 返回所有
    List<OtherLottery> findAllOtherLottery(String order);

    List<OtherLottery> findNewOtherLottery(String order);
}
