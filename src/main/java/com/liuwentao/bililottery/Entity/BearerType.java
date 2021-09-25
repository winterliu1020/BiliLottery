package com.liuwentao.bililottery.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by liuwentao on 2021/5/11 15:21
 */
@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum BearerType {
    VIDEO(0, "VIDEO"),
    ARTICLE(1, "ARTICLE"),
    DYNAMIC(2, "DYNAMIC"),
    ERROR(3, "ERROR");

    int code;
    String desc;
}
