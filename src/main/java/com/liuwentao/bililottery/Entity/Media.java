package com.liuwentao.bililottery.Entity;

import lombok.Data;

/**
 * Created by liuwentao on 2021/5/11 15:24
 */
@Data
public class Media extends Bearer{
    private String title;
    private int collectCount;

    // Media包括视频和文章；多增加封面
    private String pic;
}
