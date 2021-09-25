package com.liuwentao.bililottery.Controller;

import com.liuwentao.bililottery.Entity.LotteryResult;
import com.liuwentao.bililottery.Entity.PostLottery;
import com.liuwentao.bililottery.Entity.Reply;
import com.liuwentao.bililottery.Service.IGlobalCache;
import com.liuwentao.bililottery.Service.LotteryResultService;
import com.liuwentao.bililottery.Service.PostLotteryService;
import com.liuwentao.bililottery.factory.ScheduledThreadPoolFactory;
import com.liuwentao.bililottery.util.Help;
import com.liuwentao.bililottery.util.Lottery;
import com.liuwentao.bililottery.util.NextTimeSingleTon;
import com.liuwentao.bililottery.util.ResultWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuwentao on 2021/5/11 11:12
 */

@Slf4j
@Controller
@ResponseBody
public class lotteryController {

    @Autowired
    LotteryResultService lotteryResultService;

    @Autowired
    PostLotteryService postLotteryService;

    @Autowired
    IGlobalCache iGlobalCache;

    // id是某条动态的id   bvid|1Dy4y177st
    @RequestMapping(value = "/api/Lottery/{id}", method = RequestMethod.POST) // upId 对 id这个作品 抽 count个人
    public ResultWrapper getLotteryResult(@RequestParam("upId") String upId, @PathVariable("id") String id, @RequestParam(value = "Count") int count,
                                             @RequestParam(value = "PrizeNotes") String prizeNotes, // 奖品备注
                                             @RequestParam(value = "LotteryType") int lotteryType, // 抽奖范围：0-仅评论；1-评论+关注；2-支持者模式
                                             @RequestParam(value = "UnlimitedStart") boolean unlimitedStart,
                                             @RequestParam(value = "UnlimitedEnd") boolean unlimitedEnd,
                                             @RequestParam(value = "Start") String startStr,
                                             @RequestParam(value = "End") String endStr,
                                             @RequestParam(value = "GETStart") boolean getStart,
                                             @RequestParam(value = "LETEnd") boolean letEnd,
                                             @RequestParam(value = "DuplicatedUID") boolean duplicatedUid,
                                             @RequestParam(value = "OnlySpecified") boolean onlySpecified,
                                             @RequestParam(value = "ContentSpecified", defaultValue = "") String contentSpecified,
                                             @RequestParam(value = "timeLottery") boolean timeLottery,
                                             @RequestParam(value = "lotteryDateAndTime") String lotteryDateAndTimeStr,
                                             @RequestParam(value = "session") String session
                                             ) {


        log.info("upId 为：" + upId + " 发起的抽奖限制的时间范围及抽奖筛选类型：" + startStr + ":" + endStr + "：" + lotteryType);
        if (!iGlobalCache.hasKey(session)) {
            return ResultWrapper.builder().code(3).count(0).msg("session过期，请先登录").build();
        }

        String openId = iGlobalCache.get(session).toString();

        System.out.println("preparing");
        // 期望中奖评论数count
        if (count == 0) {
            return ResultWrapper.builder().code(1).count(0).msg("期望中奖评论数需大于0，发起抽奖失败").build();
        }

        // 判断评论承载者标准标识符（"id": "bvid|1Dy4y177st"）是否合法
        boolean isFormalId = Help.checkIdHead(id);
        if (!isFormalId) {
            return ResultWrapper.builder().code(1).count(0).msg("非法评论承载者标识符").build();
        }

        // 检查是否是有效标识符
        boolean isValid = Help.isValidId(id);
        if (!isValid) {
            return ResultWrapper.builder().code(1).count(0).msg("无效稿件/动态").build();
        }



        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date start;
        Date end;
        Date lotteryDateAndTime;
        try {
            start = format.parse(startStr);
            end = format.parse(endStr);
            if (timeLottery) {
                lotteryDateAndTime = format.parse(lotteryDateAndTimeStr);
            } else {
                String currentDateTimeStr = format.format(new Date());
                lotteryDateAndTime = format.parse(currentDateTimeStr);
                log.info("非定时抽奖，开奖时间设定为当前时间：" + lotteryDateAndTime.toString());

            }
        } catch (ParseException e) {
            e.printStackTrace();
            return ResultWrapper.builder().code(1).count(0).msg("日期格式错误").build();
        }

        // 以上校验都通过后，才真正向B站请求评论数据

        // 如果发布的是定时抽奖
        log.info("发起的是否是定时抽奖：" + timeLottery);
        // 以上校验都通过之后，不管什么抽奖我都会把数据放到数据库；


        PostLottery postLottery = PostLottery.builder().openId(openId).upId(upId).id(id).count(count).prizeNotes(prizeNotes).lotteryType(lotteryType).unlimitedStart(unlimitedStart).unlimitedEnd(unlimitedEnd).start(start).end(end).getStart(getStart)
                .letEnd(letEnd).duplicatedUid(duplicatedUid).onlySpecified(onlySpecified).contentSpecified(contentSpecified).lotteryDateAndTime(lotteryDateAndTime).build();

        if (timeLottery) {
            // 该定时抽奖距离当前时间的秒数
            Date now = new Date();
            long secondGap = (lotteryDateAndTime.getTime() - now.getTime()) / 1000; // 秒数
            log.info("当前postLottery的id是：" + postLottery.getId() + "距离当前开奖的秒数：" + secondGap);

            // 需要将抽奖设置数据存到数据库，如果这个抽奖的开奖时间 <= 整个系统下次读取数据库任务的时间，则还需要把当前任务手动放到开奖任务执行线程池；
            // 如果当前抽奖的开奖时间 > 整个系统下次读取数据库任务的时间，不需要放到线程池了，直接在后面执行放到数据库的操作就行；因为到「整个系统下次读取数据库任务的时间」时，会把这个任务拿出来放到线程池按时执行
            if (lotteryDateAndTime.compareTo(NextTimeSingleTon.getFlagSingleTon().getFlag()) <= 0) {
                // 可以精确到秒  2017-4-16 12:43:37
                DateFormat df = DateFormat.getDateTimeInstance();
                log.info("小于等于下次时间戳Flag，直接放入线程池延时执行，下次时间戳Flag：" + df.format(NextTimeSingleTon.getFlagSingleTon().getFlag()));
                // 放线程池
                ScheduledExecutorService scheduledExecutorService = ScheduledThreadPoolFactory.createDefaultScheduledThreadPool("LotteryScheduledThreadPool:");
                scheduledExecutorService.schedule(()->{
                    // 任务具体干什么；其实就是跟下面的立即抽奖一样了。。。
                    log.info("线程池开始执行一个开奖任务，开奖id是：" + id);
                    Lottery.getSingleTonLottery().lottery(id, count, unlimitedStart, unlimitedEnd, start, end, getStart, letEnd, duplicatedUid,
                            onlySpecified, contentSpecified, postLottery, true);
                }, secondGap, TimeUnit.SECONDS);
            }
            // 如果这次定时抽奖的开奖时间大于下次时间戳Flag，就要「直接」放到数据库了，不需要放到线程池了

            postLottery.setOut("还未开奖");
            postLottery.setTimeLottery(true); // 定时抽奖out先不管，数据库中为null就行
            int timeLotteryRes = postLotteryService.replacePostLottery(postLottery);
            // 发起定时抽奖也需要把之前这个稿件的中奖记录删除
            int deleteLotteryResult = lotteryResultService.deleteLotteryResult(openId, id);
            if (deleteLotteryResult >= 0) {
                log.info("已发起定时抽奖，并成功把这个稿件之前的中奖记录删除");
            } else {
                log.info("删除该定时抽奖失败之前所对应的中奖记录失败");
                return ResultWrapper.builder().code(1).count(0).msg("删除该定时抽奖失败之前所对应的中奖记录失败").build();
            }

            if (timeLotteryRes > 0) {
                // code为2：成功存到数据库发起定时抽奖
                log.info("已成功发起定时抽奖");
                return ResultWrapper.builder().code(2).count(0).msg("已成功发起定时抽奖").build();
            } else {
                log.info("发起定时抽奖失败");
                return ResultWrapper.builder().code(1).count(0).msg("发起定时抽奖失败").build();
            }
        } else {
            // 如果是即刻抽奖，执行抽奖把结果返回给前端，并且把这次抽奖都数据存到数据库
            // 真正调用B站接口之后，用返回数据做抽奖，才知道这次抽奖的真正结果；所以不管是定时还是即刻抽奖 都需要把结果存到数据库out字段
            ResultWrapper resultWrapper = Lottery.getSingleTonLottery().lottery(id, count, unlimitedStart, unlimitedEnd, start, end, getStart, letEnd, duplicatedUid,
                    onlySpecified, contentSpecified, postLottery, false);

            return resultWrapper;





//            StringBuilder out = new StringBuilder();
//            ArrayList<Reply> replyList = lotteryResultService.getList(id, count, unlimitedStart, unlimitedEnd, start, end, getStart, letEnd, duplicatedUid,
//                    onlySpecified, contentSpecified, out);
//
//            if (replyList.size() > 0) {
//                // 下面插入数据库
//                postLottery.setOut(out.toString());
//                postLottery.setTimeLottery(false);
//                int notTimeLotteryRes = postLotteryService.replacePostLottery(postLottery);
//                if (notTimeLotteryRes > 0) {
//                    log.info("发起立即抽奖成功");
//                } else {
//                    log.info("发起立即抽奖失败");
//                    return ResultWrapper.builder().code(1).count(0).msg("保存抽奖设置信息失败").build();
//                }
//                // 成功抽奖
//                // 将该次抽奖的中奖名单放到数据库LotteryResult表；如果后面查找某次抽奖的抽奖名单记录是0的话，说明该次抽奖没有成功
//                List<LotteryResult> lotteryResultList = new ArrayList<>();
//
//                for (Reply reply : replyList) {
//                    LotteryResult lotteryResult = LotteryResult.builder().patternId(id).openId(openId).build();
//                    lotteryResult.setContent(reply.getContent());
//                    lotteryResult.setLikeCount(reply.getLikeCount());
//                    lotteryResult.setId(reply.getId());
//                    lotteryResult.setPubTime(reply.getPubTime());
//                    lotteryResult.setUrl(reply.getUrl());
//                    lotteryResult.setUid(reply.getUid());
//                    lotteryResult.setUName(reply.getUName());
//                    lotteryResult.setFaceUrl(reply.getFaceUrl());
//                    lotteryResult.setUserHomeUrl(reply.getUserHomeUrl());
//                    lotteryResultList.add(lotteryResult);
//                }
//                // 首先需要将数据库中原来对这条视频抽奖的结果删掉
//                int deleteLotteryResult = lotteryResultService.deleteLotteryResult(openId, id);
//                if (deleteLotteryResult >= 0) {
//                    log.info("删除原来抽奖结果数据成功");
//                } else {
//                    log.info("删除原来抽奖结果数据失败");
//                    return ResultWrapper.builder().code(1).count(0).msg("删除原来抽奖结果数据失败").build();
//                }
//
//                int insertLotteryResult = lotteryResultService.insertLotteryResult(lotteryResultList);
//                if (insertLotteryResult >= 0) {
//                    log.info("插入新的抽奖结果数据成功");
//                    return ResultWrapper.builder().code(0).count(0).data("").msg("抽奖成功").build();
//                } else {
//                    log.info("插入新的抽奖结果数据失败");
//                    return ResultWrapper.builder().code(1).count(0).msg("插入新的抽奖结果数据失败").build();
//                }
//            } else {
//                log.info("立即抽奖失败" + out);
//                return ResultWrapper.builder().code(1).count(0).msg(out.toString()).build();
//            }
        }
    }
}
