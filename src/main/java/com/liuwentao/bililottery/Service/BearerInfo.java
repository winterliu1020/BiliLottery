package com.liuwentao.bililottery.Service;

import com.liuwentao.bililottery.Entity.BearerWrapper;
/**
 * Created by liuwentao on 2021/5/11 16:13
 */
public interface BearerInfo {
    BearerWrapper get(String pattern);
}
