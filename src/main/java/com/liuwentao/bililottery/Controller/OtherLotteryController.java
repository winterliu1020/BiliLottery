package com.liuwentao.bililottery.Controller;

import com.liuwentao.bililottery.Entity.OtherLottery;
import com.liuwentao.bililottery.ServiceImpl.OtherLotteryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OtherLotteryController {
    @Autowired
    private OtherLotteryServiceImpl otherLotteryService;
    @GetMapping("/AllOtherLottery")
    List<OtherLottery> getAllOtherLottery(@RequestParam("order") String order) {
        System.out.println(order);
        return otherLotteryService.getAllOtherLottery(order);
    }

    @GetMapping("/NewOtherLottery")
    List<OtherLottery> getNewOtherLottery(@RequestParam("order") String order) {
        System.out.println(order);
        return otherLotteryService.getNewOtherLottery(order);
    }
}
