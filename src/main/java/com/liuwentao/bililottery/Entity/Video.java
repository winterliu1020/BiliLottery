package com.liuwentao.bililottery.Entity;

import lombok.Data;

/**
 * Created by liuwentao on 2021/5/11 15:26
 */
@Data
public class Video extends Media{
    private int likeCount;
    private int coinCount;
    private int viewCount;

    // 视频：描述属性
    private String desc;
}
