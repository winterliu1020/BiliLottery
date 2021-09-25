package com.liuwentao.bililottery.ServiceImpl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liuwentao.bililottery.Entity.*;
import com.liuwentao.bililottery.Service.BearerInfo;
import com.liuwentao.bililottery.Service.BiliWrapperService;
import com.liuwentao.bililottery.util.Help;
import com.liuwentao.bililottery.util.WebHelper;
import com.liuwentao.bililottery.Entity.BearerWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by liuwentao on 2021/5/11 16:14
 */

@Slf4j
@Service
public class BearerInfoImpl implements BearerInfo {

    @Autowired
    BiliWrapperService biliWrapperService;

    @Override
    public BearerWrapper get(String pattern) {
        // 获取评论承载者标识符
        String id = Help.getFormalIdOrPattern(pattern);
        BearerWrapper bearerWrapper = new BearerWrapper();
        bearerWrapper.setType(BearerType.ERROR);
        if (!"".equals(id) && Help.isValidId(id)) {
            // 获取评论承载者详细信息接口URL
            String detailApiUrl = Help.getBearerDetailApiUrl(id);
            if (!"".equals(detailApiUrl)) {
                // 针对每种评论承载者做对应处理
                String content = WebHelper.getResponse(detailApiUrl, "{\"code\":0");

                System.out.println(content);

                if (!"".equals(content)) {
                    Map top = JSON.parseObject(content); // 请求返回的JSON数据的最外层
                    JSONObject data = (JSONObject) top.get("data"); // 需要用到的数据
                    BearerType type = Help.getBearerTypeById(id);

                    if (type == BearerType.VIDEO) {
                        Map View = JSON.parseObject(data.get("View").toString());
                        String bvId = View.get("bvid").toString();
                        // 视频的封面和描述
                        String pic = View.get("pic").toString();
                        String desc = View.get("desc").toString();

                        Map owner = JSON.parseObject(View.get("owner").toString());
                        Map stat = JSON.parseObject(View.get("stat").toString());

                        // 设置bearerWrapper对象的类型
                        bearerWrapper.setType(BearerType.VIDEO);
                        Video video = new Video();
                        // 将通过API获取的数据放到bearer对象
                        video.setCommentCount(Integer.parseInt(stat.get("reply").toString()));
                        video.setFaceUrl(owner.get("face").toString());
                        video.setId(id);


                        // 接口返回的距离1970年的秒数，这里得转成Date对象
                        String timeStr = "";
                        String dateStr="1970-1-1 08:00:00";
                        String time = View.get("pubdate").toString();
                        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        Date miDate;
                        try {
                            miDate = sdf.parse(dateStr);
                            Object t1=miDate.getTime();
                            long h1=Long.parseLong(time)*1000+Long.parseLong(t1.toString());
                            timeStr=sdf.format(h1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        try {
                            video.setPubTime(sdf.parse(timeStr));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        video.setShareCount(Integer.parseInt(stat.get("share").toString()));
                        video.setUid(owner.get("mid").toString());
                        video.setUName(owner.get("name").toString());
                        video.setUserHomeUrl("https://space.bilibili.com/" + owner.get("mid").toString());
                        video.setUrl("https://www.bilibili.com/video/" + bvId);
                        video.setCoinCount(Integer.parseInt(stat.get("coin").toString()));
                        video.setCollectCount(Integer.parseInt(stat.get("favorite").toString()));
                        video.setLikeCount(Integer.parseInt(stat.get("like").toString()));
                        video.setTitle(View.get("title").toString());
                        video.setViewCount(Integer.parseInt(stat.get("view").toString()));

                        // 视频的封面和描述
                        video.setPic(pic);
                        video.setDesc(desc);

                        // 将视频信息存到BiliWrapper表
                        BiliWrapper biliWrapper = BiliWrapper.builder().id(video.getId())
                                .likeCount(video.getLikeCount()).pubTime(video.getPubTime())
                                .url(video.getUrl()).commentCount(video.getCommentCount())
                                .shareCount(video.getShareCount()).pic(video.getPic())
                                .title(video.getTitle()).coinCount(video.getCoinCount())
                                .viewCount(video.getViewCount()).desc(video.getDesc()).build();

                        log.info("获取到视频数据：" + video);
                        biliWrapperService.replaceBiliWrapper(biliWrapper);

                        bearerWrapper.setBearer(video);
                    } else if (type == BearerType.ARTICLE) {


                        Map stats = JSON.parseObject(data.get("stats").toString());
                        bearerWrapper.setType(BearerType.ARTICLE);
                        Article article = new Article();
                        article.setCoinCount(Integer.parseInt(stats.get("coin").toString()));
                        article.setCollectCount(Integer.parseInt(stats.get("favorite").toString()));
                        article.setCommentCount(Integer.parseInt(stats.get("reply").toString()));
                        article.setLikeCount(Integer.parseInt(stats.get("like").toString()));
                        article.setId(id);
                        article.setShareCount(Integer.parseInt(stats.get("share").toString()));
                        article.setTitle(data.get("title").toString());
                        article.setViewCount(Integer.parseInt(stats.get("view").toString()));
                        article.setUid(data.get("mid").toString());
                        article.setUName(data.get("author_name").toString());
                        article.setUserHomeUrl("https://space.bilibili.com/" + data.get("mid"));
                        article.setUrl("https://www.bilibili.com/read/" + id.substring(id.indexOf("|") + 1));

                        // b站接口，文章没有封面数据
//                        article.setPic();

                        // 将文章信息存到BiliWrapper表
                        BiliWrapper biliWrapper = BiliWrapper.builder().id(article.getId())
                                .likeCount(article.getLikeCount()).pubTime(article.getPubTime())
                                .url(article.getUrl()).commentCount(article.getCommentCount())
                                .shareCount(article.getShareCount()).pic(article.getPic())
                                .title(article.getTitle()).coinCount(article.getCoinCount())
                                .viewCount(article.getViewCount()).build();
                        biliWrapperService.replaceBiliWrapper(biliWrapper);

                        log.info("获取到文章数据：" + article);
                        bearerWrapper.setBearer(article);
                    } else if (type == BearerType.DYNAMIC) {


                        Map card = JSON.parseObject(data.get("card").toString());
                        Map desc = JSON.parseObject(card.get("desc").toString());
                        Map user_profile = JSON.parseObject(desc.get("user_profile").toString());
                        Map info = JSON.parseObject(user_profile.get("info").toString());
                        bearerWrapper.setType(BearerType.DYNAMIC);
                        Dynamic dynamic = new Dynamic();

                        // 接口返回的距离1970年的秒数，这里得转成Date对象
                        String timeStr = "";
                        String dateStr="1970-1-1 08:00:00";
                        String time = desc.get("timestamp").toString();
                        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        Date miDate;
                        try {
                            miDate = sdf.parse(dateStr);
                            Object t1=miDate.getTime();
                            long h1=Long.parseLong(time)*1000+Long.parseLong(t1.toString());
                            timeStr=sdf.format(h1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        try {
                            dynamic.setPubTime(sdf.parse(timeStr));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        dynamic.setCommentCount(Integer.parseInt(desc.get("comment").toString()));
                        dynamic.setFaceUrl(info.get("face").toString());
//                        dynamic.setPubTime(new Date(Long.parseLong(desc.get("timestamp").toString())));
                        dynamic.setId(id);
                        dynamic.setLikeCount(Integer.parseInt(desc.get("like").toString()));
                        dynamic.setShareCount(Integer.parseInt(desc.get("repost").toString()));
                        dynamic.setUid(desc.get("uid").toString());
                        dynamic.setUName(info.get("uname").toString());
                        dynamic.setUserHomeUrl("https://space.bilibili.com/" + desc.get("uid"));
                        dynamic.setUrl("https://t.bilibili.com/" + desc.get("dynamic_id").toString());

                        // 将动态信息存到BiliWrapper表
                        BiliWrapper biliWrapper = BiliWrapper.builder().id(dynamic.getId())
                                .likeCount(dynamic.getLikeCount()).pubTime(dynamic.getPubTime())
                                .url(dynamic.getUrl()).commentCount(dynamic.getCommentCount())
                                .shareCount(dynamic.getShareCount()).build();

                        biliWrapperService.replaceBiliWrapper(biliWrapper);

                        log.info("获取到动态数据：" + dynamic);
                        bearerWrapper.setBearer(dynamic);
                    }
                }
            }
        }
        return bearerWrapper;
    }
}
