package com.liuwentao.bililottery.Entity;

import lombok.Data;

/**
 * Created by liuwentao on 2021/5/11 15:25
 */
@Data
public class Article extends Media{
    private int likeCount;
    private int coinCount;
    private int viewCount;
}
