package com.liuwentao.bililottery.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Created by liuwentao on 2021/5/11 15:16
 */
@Data
public class ItemBase extends User{
    private String id; // 根据不同的继承者表示不同的属性，比如reply继承就是某条评论的id，如果是video继承，就是视频的bvid号
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date pubTime;
    private String url;
}
