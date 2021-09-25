package com.liuwentao.bililottery.Entity;

import lombok.Builder;
import lombok.Data;

/**
 * Created by liuwentao on 2021/5/11 15:28
 */
@Data
public class Reply extends ItemBase{
    private String content;
    private int likeCount;

    // 权重：用于在支持者模式下根据权重随机抽奖
    private int weight;
}
