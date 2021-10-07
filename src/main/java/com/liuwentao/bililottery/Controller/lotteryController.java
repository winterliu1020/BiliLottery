package com.liuwentao.bililottery.Controller;

import com.liuwentao.bililottery.Entity.BearerWrapper;
import com.liuwentao.bililottery.Entity.LotteryResult;
import com.liuwentao.bililottery.Entity.PostLottery;
import com.liuwentao.bililottery.Entity.Reply;
import com.liuwentao.bililottery.Service.BearerInfo;
import com.liuwentao.bililottery.Service.IGlobalCache;
import com.liuwentao.bililottery.Service.LotteryResultService;
import com.liuwentao.bililottery.Service.PostLotteryService;
import com.liuwentao.bililottery.factory.ScheduledThreadPoolFactory;
import com.liuwentao.bililottery.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

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

    @Autowired
    BearerInfo bearerInfo;

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

        ResultWrapper resultWrapper = null;
        Date start = null;
        Date end = null;
        Date lotteryDateAndTime = null;
        String openId = "";
        log.info("upId 为：" + upId + " 发起的抽奖限制的时间范围及抽奖筛选类型：" + startStr + ":" + endStr + "：" + lotteryType);
        if (!iGlobalCache.hasKey(session)) {
            resultWrapper = ResultWrapper.builder().code(3).count(0).msg("session过期，请先登录").build();
        } else {
            log.info("奖品备注：" + prizeNotes);
            String[] lines = prizeNotes.split("\\r?\\n");
            if (lines.length <= 0) {
                resultWrapper = ResultWrapper.builder().code(1).count(0).msg("请至少填写一行奖品备注，发起抽奖失败").build();
            } else {
                int allPrizeCount = 0;
                try {
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        if (!line.contains("，")) {
                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("第" + (i + 1) + "行奖品备注中请用中文「，」分割，发起抽奖失败").build();
                            break;
                        }
                        String[] prizeNameAndCount = line.split("，");
                        if (prizeNameAndCount.length != 2) {
                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("第" + (i + 1) + "行奖品备注中出现多个「，」分割，发起抽奖失败").build();
                            break;
                        }
                        int countForOnePrize = Integer.parseInt(prizeNameAndCount[1]);
                        if (countForOnePrize > 50 || countForOnePrize <= 0) {
                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("奖项对应人数应在50以内，发起抽奖失败").build();
                            break;
                        }
                        if (allPrizeCount > 50) {
                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("期望中奖评论数需大于0，发起抽奖失败").build();
                            break;
                        }
                        log.info("各行数据：" + prizeNameAndCount[0] + " " + countForOnePrize);
                        allPrizeCount += countForOnePrize;
                    }
                } catch (Exception e) {
                    // 有数字转换异常
                    log.info("用户输入奖项名称及对应数量格式转换异常");
                    resultWrapper = ResultWrapper.builder().code(1).count(0).msg("每一行的奖品数量请用整数数字表示，发起抽奖失败").build();
                    return resultWrapper;
                }
                if (allPrizeCount != count) {
                    resultWrapper = ResultWrapper.builder().code(1).count(0).msg("各项奖品数量和抽奖总人数不相等，发起抽奖失败").build();
                    return resultWrapper;
                }

                openId = iGlobalCache.get(session).toString();
                if (!timeLottery && iGlobalCache.hasKey(openId)) { // 对于不是定时任务的，2分钟之内不能重复发起抽奖；定时任务可以重复发起
                    log.info("当前用户正在抽奖...");
                    resultWrapper = ResultWrapper.builder().code(1).count(0).msg("你当前正在抽奖，请稍后再试").build();
                    return resultWrapper;
                } else {
                    iGlobalCache.set(openId, "openId:" + openId, 2 * 60); // 如果当前正在抽奖，限制用户2分钟之内不能再次发起抽奖

                    ScheduledExecutorService scheduledExecutorService;

                    // 如果又是这个openId对这个id号发起抽奖，需要先判断在ScheduledTaskMapSingleTon的hashmap中是否存在该openId对该id发起的定时任务，如果有，则需要删除hashmap中的该定时任务
                    String key = openId + "+" + id;
                    HashMap<String, RunnableScheduledFuture<?>> map = ScheduledTaskMapSingleTon.getScheduledTaskMapSingleTon().getScheduledFutureHashMap();



                    if (map.containsKey(key)) {
                        // 在scheduledThreadPoolExecutor删除定时任务对应的schedule
                        scheduledExecutorService = ScheduledThreadPoolFactory.createDefaultScheduledThreadPool("LotteryScheduledThreadPool:");
                        RunnableScheduledFuture<?> runnableScheduledFuture = ScheduledTaskMapSingleTon.getScheduledTaskMapSingleTon().getScheduledFutureHashMap().get(key);

                        // ScheduledThreadPoolExecutor这个类继承了ThreadPoolExecutor这个类，说明它可以执行与线程池相关的方法，同时，它还实现了ScheduledExecutorService这个接口中的schedule、scheduleAtFixedRate、scheduleWithFixedDelay方法，
                        // 所以对于定时线程池来说，没有ScheduledThreadPoolExecutorService这个类，直接用ScheduledThreadPoolExecutor即可
                        log.info("任务对象的地址：" + map.get(key));
                        boolean removeScheduledTask = ((ScheduledThreadPoolExecutor) scheduledExecutorService).remove(map.get(key));
                        if (removeScheduledTask) {
                            log.info("线程池删除定时任务" + key + "成功");
                        } else {
                            log.info("线程池删除定时任务" + key + "失败");
                        }

                        log.info("当前" + key + " 的任务在定时任务hashmap中已经存在，执行map.remove(key)删除定时任务");
                        map.remove(key);
                    }

                    log.info("preparing");
                    // 期望中奖评论数count
                    if (count <= 0) {
                        resultWrapper = ResultWrapper.builder().code(1).count(0).msg("期望中奖评论数需大于0，发起抽奖失败").build();
                    } else if (count > 50) {
                        resultWrapper = ResultWrapper.builder().code(1).count(0).msg("抽奖人数超过50，发起抽奖失败").build();
                    } else {
                        // 判断评论承载者标准标识符（"id": "bvid|1Dy4y177st"）是否合法
                        boolean isFormalId = Help.checkIdHead(id);
                        if (!isFormalId) {
                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("非法评论承载者标识符").build();
                            log.info("非法评论承载者标识符");
                        } else {
                            // 检查是否是有效标识符
                            boolean isValid = Help.isValidId(id);
                            if (!isValid) {
                                resultWrapper = ResultWrapper.builder().code(1).count(0).msg("无效稿件/动态").build();
                                log.info("无效稿件/动态");
                            } else {
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                                boolean timeIsValid = true;  // 判断时间是否合法
                                try {
                                    start = format.parse(startStr);
                                    end = format.parse(endStr);
                                    if (timeLottery) {
                                        lotteryDateAndTime = format.parse(lotteryDateAndTimeStr);

                                        // 还需要检测设定的开奖时间是否合法（要求必须是将来的某个时刻，不能在过去或者现在）
                                        Date now = new Date();
                                        if (now.compareTo(lotteryDateAndTime) > 0) {
                                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("日期不能设置在过去").build();
                                            log.info("日期不能设置在过去");
                                            timeIsValid = false;
                                        } else {
                                            log.info("定时抽奖设置的时间合法");
                                        }
                                    } else {
                                        String currentDateTimeStr = format.format(new Date());
                                        lotteryDateAndTime = format.parse(currentDateTimeStr);
                                        log.info("非定时抽奖，开奖时间设定为当前时间：" + lotteryDateAndTime.toString());
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    resultWrapper = ResultWrapper.builder().code(1).count(0).msg("日期格式错误").build();
                                    log.info("日期格式错误");
                                    timeIsValid = false;
                                }
                                if (timeIsValid) {
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
                                            scheduledExecutorService = ScheduledThreadPoolFactory.createDefaultScheduledThreadPool("LotteryScheduledThreadPool:");
                                            Date finalStart = start;
                                            Date finalEnd = end;
                                            RunnableScheduledFuture<?> schedule = (RunnableScheduledFuture<?>) scheduledExecutorService.schedule(() -> {
                                                // 任务具体干什么；其实就是跟下面的立即抽奖一样了。。。
                                                log.info("线程池开始执行一个开奖任务，开奖id是：" + id);
                                                Lottery.getSingleTonLottery().lottery(id, count, unlimitedStart, unlimitedEnd, finalStart, finalEnd, getStart, letEnd, duplicatedUid,
                                                        onlySpecified, contentSpecified, postLottery, true);
                                            }, secondGap, TimeUnit.SECONDS);
                                            // 后面如果要删除这个定时任务，就需要删掉这个schedule，这个schedule其实就是你传入到定时线程池中runnable对象的封装类，你想删掉你传入的定时任务，只需要把这个封装类删掉即可
                                            // 所以这里发起一个定时任务，得把这个runnableScheduleFuture对象放到ScheduledTaskMapSingleTon这个单例的hashmap中
                                            ScheduledTaskMapSingleTon.getScheduledTaskMapSingleTon().getScheduledFutureHashMap().put(key, schedule);

                                            RunnableScheduledFuture<?> runnableScheduledFuture = ScheduledTaskMapSingleTon.getScheduledTaskMapSingleTon().getScheduledFutureHashMap().get(key);
                                            log.info("任务333对象的地址：" + runnableScheduledFuture.toString());
                                        }
                                        // 如果这次定时抽奖的开奖时间大于下次时间戳Flag，就要「直接」放到数据库了，不需要放到线程池了

                                        postLottery.setOut("还未开奖");
                                        postLottery.setTimeLottery(true); // 定时抽奖out先不管，数据库中为null就行
                                        int timeLotteryRes = postLotteryService.replacePostLottery(postLottery);
                                        // 发起定时抽奖也需要把之前这个稿件的中奖记录删除
                                        int deleteLotteryResult = lotteryResultService.deleteLotteryResult(openId, id);
                                        if (deleteLotteryResult >= 0) {
                                            log.info("已发起定时抽奖，并成功把这个稿件之前的中奖记录删除");
                                            if (timeLotteryRes > 0) {
                                                // code为2：成功存到数据库发起定时抽奖
                                                log.info("已成功发起定时抽奖");
                                                resultWrapper = ResultWrapper.builder().code(2).count(0).msg("已成功发起定时抽奖").build();
                                            } else {
                                                log.info("发起定时抽奖失败");
                                                resultWrapper = ResultWrapper.builder().code(1).count(0).msg("发起定时抽奖失败").build();
                                            }
                                        } else if (deleteLotteryResult < 0) {
                                            log.info("删除该定时抽奖失败之前所对应的中奖记录失败");
                                            resultWrapper = ResultWrapper.builder().code(1).count(0).msg("删除该定时抽奖失败之前所对应的中奖记录失败").build();
                                        }
                                    } else {
                                        // 如果是即刻抽奖，执行抽奖把结果返回给前端，并且把这次抽奖都数据存到数据库
                                        // 真正调用B站接口之后，用返回数据做抽奖，才知道这次抽奖的真正结果；所以不管是定时还是即刻抽奖 都需要把结果存到数据库out字段
                                        log.info("执行立即抽奖");
                                        resultWrapper = Lottery.getSingleTonLottery().lottery(id, count, unlimitedStart, unlimitedEnd, start, end, getStart, letEnd, duplicatedUid,
                                                onlySpecified, contentSpecified, postLottery, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        iGlobalCache.del(openId);
        return resultWrapper;
    }
}
