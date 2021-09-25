package com.liuwentao.bililottery;

import com.liuwentao.bililottery.Entity.Reply;
import com.liuwentao.bililottery.Service.IGlobalCache;
import com.liuwentao.bililottery.util.Help;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
class BiliLotteryApplicationTests {

    @Autowired
    private IGlobalCache globalCache;

    @Test
    public void test() {
        globalCache.set("key2", "value3");
        globalCache.lSetAll("list", Arrays.asList("hello", "redis"));
        List<Object> list = globalCache.lGet("list", 0, -1);
        System.out.println(globalCache.get("key2"));
    }

    @Test
    void contextLoads() {
    }

    @Test
    public void testRandomLotteryWithWeight() {
        Reply reply1 = new Reply();
        reply1.setWeight(2);
        reply1.setContent("weight is 2");

        Reply reply2 = new Reply();
        reply2.setWeight(4);
        reply2.setContent("weight is 4");

        Reply reply3 = new Reply();
        reply3.setWeight(6);
        reply3.setContent("weight is 6");

        ArrayList<Reply> replyArrayList = new ArrayList<>();
        replyArrayList.add(reply1);
        replyArrayList.add(reply2);
        replyArrayList.add(reply3);

        ArrayList<Reply> result = new ArrayList<>();
        Help.getRandomResultListWithWeight(result, replyArrayList, 2);
    }

}
