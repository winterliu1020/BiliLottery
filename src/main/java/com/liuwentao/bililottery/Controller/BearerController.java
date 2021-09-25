package com.liuwentao.bililottery.Controller;

import com.liuwentao.bililottery.Entity.Bearer;
import com.liuwentao.bililottery.Entity.BearerType;
import com.liuwentao.bililottery.Entity.BearerWrapper;
import com.liuwentao.bililottery.Entity.BiliWrapper;
import com.liuwentao.bililottery.Service.BearerInfo;
import com.liuwentao.bililottery.ServiceImpl.BearerInfoImpl;
import com.liuwentao.bililottery.util.ResultWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by liuwentao on 2021/5/11 15:32
 */

@Slf4j
@Controller
public class BearerController {

    @Autowired
    BearerInfo bearerInfo;

    @RequestMapping(value = "/api/bearer", method = {RequestMethod.POST})
    @ResponseBody
    public ResultWrapper<BearerWrapper> getBearer(@RequestParam(value = "pattern") String pattern) {
//        String pattern = "https://www.bilibili.com/video/BV1Dy4y177st";
        System.out.println("后端接收到请求：" + pattern);

        BearerWrapper bearerWrapper = bearerInfo.get(pattern);

//        Bearer bearer = bearerWrapper.getBearer();

        log.info(bearerWrapper.toString());
//        System.out.println(bearerWrapper.getBearer().toString() + bearerWrapper.getBearer().getPubTime());
        if (bearerWrapper.getType() == BearerType.ERROR) {
            return ResultWrapper.<BearerWrapper>builder().code(1).count(0).msg("id或者URL无效").build();
        } else {
            return ResultWrapper.<BearerWrapper>builder().code(0).count(1).data(bearerWrapper).build();
        }
    }
}
