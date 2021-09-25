package com.liuwentao.bililottery.util;

import com.alibaba.fastjson.JSON;
import com.liuwentao.bililottery.Entity.BearerWrapper;
import com.liuwentao.bililottery.Entity.UserModel;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;

/**
 * Created by liuwentao on 2021/5/11 15:52
 */
@Data
@Builder
public class ResultWrapper<T> {
    private int code;
    private String msg;
    private T data;
    private long count;

    public static ResultWrapper error(int errorCode, String errorMsg) {
        return ResultWrapper.builder().code(errorCode).msg(errorMsg).build();
    }

    public static <T> ResultWrapper success(T data, String msg) {
        ResultWrapper res = ResultWrapper.builder().code(0).msg(msg).data(data).build();
        System.out.println("返回：" + res.toString());
        return res;
    }
}
