package com.liuwentao.bililottery.Entity;

import lombok.Builder;
import lombok.Data;

/**
 * Created by liuwentao on 2021/5/11 15:29
 *
 * B站用户实体
 */
@Data
public class User {
    private String uid;
    private String uName;
    private String faceUrl;
    private String userHomeUrl;
}
