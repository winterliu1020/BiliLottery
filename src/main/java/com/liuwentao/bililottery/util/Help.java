package com.liuwentao.bililottery.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liuwentao.bililottery.Entity.BearerType;
import com.liuwentao.bililottery.Entity.Reply;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by liuwentao on 2021/5/11 16:18
 */

@Slf4j
public class Help {

    // 根据ID或者URL来获取承载者标识符
    public static String getFormalIdOrPattern(String pattern) {
        if (pattern.length() <= 0) {
            return "";
        }
        // 去除可能包含的锚定
        int index = pattern.indexOf("#");
        if (index != -1) {
            pattern = pattern.substring(0, index);
        }

        // 如果是URL
        if (pattern.startsWith("http")) {
            // 跳转解析
            if (pattern.contains("b23.tv")) {
                String realUrl = getRealUrl(pattern);
                if (!"".equals(realUrl)) {
                    return getFormalIdOrPattern(realUrl);
                }
            } else { // 只需要提取rawId
                String lower = pattern.toLowerCase();
                String rawId = "";
                if (lower.contains("/av")) {
                    rawId = pattern.substring(lower.indexOf("/av") + 1);
                } else if (lower.contains("/bv")) {
                    rawId = pattern.substring(lower.indexOf("/bv") + 1);
                } else if (lower.contains("/cv")) {
                    rawId = pattern.substring(lower.indexOf("/cv") + 1);
                } else if (lower.contains("t.bilibili.com/")) {
                    rawId = pattern.substring(lower.indexOf("t.bilibili.com/") + 15);
                } else {
                    rawId = "";
                }
                // 去除后面的参数
                int index1 = rawId.indexOf("?");
                if (index1 != -1) {
                    rawId = rawId.substring(0, index1);
                }
                int index2 = rawId.indexOf("/");
                if (index2 != -1) {
                    rawId = rawId.substring(0, index2);
                }
                if (rawId.length() > 0) {
                    return getFormalIdFromRawId(rawId);
                }
            }
        } else // rawId
        {
            return getFormalIdFromRawId(pattern);
        }
        return "";
    }

    // 获取b站分享链接指向的realUrl
    public static String getRealUrl(String shareUrl) {
        String realUrl = WebHelper.getRedirect(shareUrl);
        if (!"".equals(realUrl) && !realUrl.contains("b23.tv")) {
            return realUrl;
        }
        return "";
    }

    // 根据rawId获取格式化的评论承载者标识符
    public static String getFormalIdFromRawId(String rawId) {
        if (rawId.length() == 0) return "";
        // Id由数字开头：动态
        if (rawId.charAt(0) >= '0' && rawId.charAt(0) <= '9') {
            return "did|" + rawId;
        } else if (rawId.length() >= 2) {
            String prefix = rawId.substring(0, 2).toLowerCase();
            String body = rawId.substring(2);
            if ("av".equals(prefix)) {
                return "aid|" + body;
            }
            if ("bv".equals(prefix)) {
                return "bvid|" + body;
            }
            if ("cv".equals(prefix)) {
                return "cv|" + body;
            }
            return "";
        }
        return "";
    }

    public static String getBearerDetailApiUrl(String id) {
        String[] parts = getIdHeadBody(id);
        if (parts != null && parts.length == 2) {
            String head = parts[0]; // 评论承载者标识符前缀
            String body = parts[1]; // 真实的ID
            String infoApiUrl = "";

            if ("aid".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/web-interface/view/detail?aid=" + body;
            } else if ("bvid".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/web-interface/view/detail?bvid=" + body;
            } else if ("cv".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/article/viewinfo?id=" + body;
            } else if ("did".equals(head)) {
                infoApiUrl = "http://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/get_dynamic_detail?dynamic_id=" + body;
            }
            return infoApiUrl;
        }
        return "";
    }

    // 获取评论承载者标识符前缀和本体
    public static String[] getIdHeadBody(String id) {
        int posSplit = id.indexOf('|');
        if (posSplit == -1 || posSplit + 1 >= id.length()) {
            return null;
        }
        return new String[]{id.substring(0, posSplit), id.substring(posSplit + 1)};
    }

    // 检查评论承载者Id是否对应有效的媒体稿件/动态
    public static boolean isValidId(String id) {
        String infoApiUrl = getInfoApiUrl(id);
        if (!"".equals(infoApiUrl)) {
            String content = WebHelper.getResponse(infoApiUrl, "{\"code\":0,"); // 判断
            if (!"".equals(content) && content.startsWith("{\"code\":0,")) {
                Map mapTypes = JSON.parseObject(content);
                if (mapTypes.containsKey("data")) {
                    JSONObject firstPageData = (JSONObject) mapTypes.get("data");
                    if (firstPageData.size() > 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getInfoApiUrl(String id) {
        String[] parts = getIdHeadBody(id);
        if (parts != null && parts.length == 2) {
            String head = parts[0]; // 评论承载者标识符前缀
            String body = parts[1]; // 真实的ID
            String infoApiUrl = "";

            if ("aid".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/web-interface/archive/stat?aid=" + body;
            } else if ("bvid".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/web-interface/archive/stat?bvid=" + body;
            } else if ("cv".equals(head)) {
                infoApiUrl = "http://api.bilibili.com/x/article/viewinfo?id=" + body;
            } else if ("did".equals(head)) {
                infoApiUrl = "http://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/get_dynamic_detail?dynamic_id=" + body;
            }
            return infoApiUrl;
        }
        return "";
    }

    // 根据评论承载者标识符获得评论承载者类型
    public static BearerType getBearerTypeById(String id) {
        String[] parts = getIdHeadBody(id); // 例如：aid|873231 对应的parts[aid, 873231]
        BearerType bType = BearerType.ERROR;
        if (parts == null) return bType;
        if ("aid".equals(parts[0]) || "bvid".equals(parts[0])) {
            bType = BearerType.VIDEO;
        } else if ("cv".equals(parts[0])) {
            bType = BearerType.ARTICLE;
        } else if ("did".equals(parts[0])) {
            bType = BearerType.DYNAMIC;
        }
        return bType;
    }

    // 检查评论承载者标识符是否符合规定
    public static boolean checkIdHead(String id) {
        String[] parts = getIdHeadBody(id);
        if (parts != null && parts.length == 2) {
            String head = parts[0];
            if ("aid".equals(head) || "bvid".equals(head) || "cv".equals(head) || "did".equals(head)) {
                return true;
            }
        }
        return false;
    }

    // 获取评论区信息接口URL
    public static String getReplyApiUrl(String id) {
        String[] parts = getIdHeadBody(id);
        if (parts != null && parts.length == 2) {
            String head = parts[0];
            String body = parts[1];
            String oid = body;
            int workType = 0;
            if ("aid".equals(head) || "bvid".equals(head)) {
                workType = 1;
            } else if ("cv".equals(head)) {
                workType = 12;
                return "http://api.bilibili.com/x/v2/reply?oid=" + oid + "&type=" + workType + "&sort=0&ps=49&pn=";
            } else if ("did".equals(head)) {
                workType = 17;
            } else {
                return "";
            }
            String infoApiUrl = getInfoApiUrl(id); // 视频/稿件信息URL
            if (!"".equals(infoApiUrl)) {
                String content = WebHelper.getResponse(infoApiUrl, "{\"code\":0,");
                if (!"".equals(content)) {
                    Map top = JSON.parseObject(content);
                    Map data = JSON.parseObject(top.get("data").toString());
                    // 动态需要判断是否存在rid
                    if (workType == 17 && data.containsKey("card")) {
                        Map card = JSON.parseObject(data.get("card").toString());
                        Map desc = JSON.parseObject(card.get("desc").toString());
                        if ("2".equals(desc.get("type").toString())) {
                            workType = 11;
                            oid = desc.get("rid").toString();
                        }
                    } else if (workType == 1) {
                        // 视频稿件使用aid作为oid
                        oid = data.get("aid").toString();
                    }
                    return "http://api.bilibili.com/x/v2/reply?oid=" + oid + "&type=" + workType + "&sort=0&ps=49&pn=";
                }
            }
        }
        return "";
    }

    public static String getReplyUrl(String id) {
        String[] parts = getIdHeadBody(id);
        String res  = "";
        if (parts != null && parts.length == 2) {
            String head = parts[0];
            String body = parts[1];
            if ("aid".equals(head)) {
                res = "http://www.bilibili.com/video/av" + body + "#reply";
            } else if ("bvid".equals(head)) {
                res = "http://www.bilibili.com/video/BV" + body + "#reply";
            } else if ("cv".equals(head)) {
                res = "http://www.bilibili.com/read/cv" + body + "#reply";
            } else if ("did".equals(head)) {
                res = "http://t.bilibili.com/" + body +"#reply";
            }
        }
        return res;
    }

    public static void getRandomResultList(ArrayList<Reply> result, ArrayList<Reply> totalList, int count) {
        for (int i = totalList.size(); i > totalList.size() - count; i--) {
            int pRandom = (int)(Math.random() * i);
            result.add(totalList.get(pRandom));
            // 把最后一个元素更新当前选中的位置
            totalList.set(pRandom, totalList.get(i - 1));
        }
    }

    // 只获取指定数量的随机索引
    public static List<Integer> getRandomIndexList(int[] source, int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = source.length; i > source.length - count; i--) {
            int pRandom = (int)(Math.random() * i);
            result.add(source[pRandom]);
            // 把最后一个元素更新当前选中的位置
            source[pRandom] = source[i - 1];
        }
        return result;
    }


    // 先随机出一个人，再判断是否关注，关注了则放到结果集合，没关注则继续随机抽下一个；注意：这里抽出来的result中评论个数可能少于count
    public static void getRandomResultListWithFollowing(ArrayList<Reply> result, ArrayList<Reply> totalList, int count, String upId) {

        for (int i = totalList.size(); i > 0; i--) {
            int pRandom = (int)(Math.random() * i);
            // 判断当前随机到的评论是否关注了up主
            if (Lottery.judgeFollowedAndGetWeight(upId, totalList.get(pRandom).getUid()) > 0) {
                // 大于0表示关注了，则符合条件加入到result
                result.add(totalList.get(pRandom));
                if (result.size() >= count) {
                    break;
                }
            }
            // 把最后一个元素更新当前选中的位置
            totalList.set(pRandom, totalList.get(i - 1));
        }
    }

    // 根据每个reply的权重属性随机抽取；利用前缀和+二分
    public static void getRandomResultListWithWeight(ArrayList<Reply> result, ArrayList<Reply> totalList, int count) {

        // 两种方式，因为要抽count个，但是我每一次二分只能抽出一个，抽完这一个我可以选择：1. 继续随机数，如果还抽到这个就直接再抽，直到抽满count个；2. 这样如果某个节点的权重占比很大的话，那每次抽可能都是这个人，所以还可以选择抽完一个把它剔除，然后构造一个新的权重数组
        // 这里用第一种方式，因为整个totalList的范围是非常大的，而每一个人的占比相对很小很小，而count也相对很小，所以出现重复抽一个人的情况机率很小
        int[] weight = new int[totalList.size() + 1];

        // 构造权重(前缀和)数组
        for (int i = 0; i < totalList.size(); i++) {
            weight[i + 1] = weight[i] + totalList.get(i).getWeight();
        }

        HashSet<Integer> lotteryReplyIndexSet = new HashSet<>(); // 已抽中的reply在totalList中的索引
        int luckyReplyIndex = 0;
        int tryTime = 0;
        while (true) {
            if (lotteryReplyIndexSet.size() >= count) {
                break;
            }
            log.info("进行第" + (++tryTime) + "次尝试按权重抽奖");

            // 随机数范围：[0, weight[weight.length - 1])
            int random = (int) (Math.random() * (weight[weight.length - 1])); // 向下取整
            luckyReplyIndex = binarySearchWithWeight(weight, random);

            if (lotteryReplyIndexSet.contains(luckyReplyIndex)) {
                continue;
            }
            lotteryReplyIndexSet.add(luckyReplyIndex);
            result.add(totalList.get(luckyReplyIndex));
            log.info("抽选到一条评论：" + totalList.get(luckyReplyIndex).toString() + ", 它所在的权重数组总的权重值是：" + weight[weight.length - 1]);
        }

        // 第二种方式
        /*
        for (int times = 0; times < count; times++) {

            int[] weight = new int[totalList.size() + 1 - times]; // 容量得减去已经抽出来的那部分

            // 动态构造权重(前缀和)数组
            for (int i = 0; i < totalList.size() - times; i++) {
                weight[i + 1] = weight[i] + totalList.get(i).getWeight();
            }
            // 随机数范围：[0, weight[weight.length - 1])
            int random = (int) (Math.random() * (weight[weight.length - 1])); // 向下取整

            // 再用二分去前缀和数组中查找这个随机数所属的索引
            int index = binarySearchWithWeight (weight, random); // 这个就是抽到的评论在totalList中的索引

            result.add(totalList.get(index));
            log.info("抽选到一条评论：" + totalList.get(index).toString() + ", 它所在的权重数组总的权重值是：" + weight[weight.length - 1]);


            // 把totalList的最后一个赋值到index
            totalList.set(index, totalList.get(totalList.size() - 1));
        }
         */

        log.info("最终选出的抽奖评论结果集合是：" + result.toString());
    }

    private static int binarySearchWithWeight(int[] weight, int random) {
        int l = 0, r = weight.length - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            if (random == weight[mid]) {
                return mid + 1 - 1; // 应该抽选的reply在totalList中的索引
            } else if (random < weight[mid]) {
                r = mid - 1;
            } else if (random > weight[mid]){
                l = mid + 1;
            }
        }
        return l - 1;
    }

}
