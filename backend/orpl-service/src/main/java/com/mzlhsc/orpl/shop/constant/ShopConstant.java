package com.mzlhsc.orpl.shop.constant;

/**
 * 店铺常量
 */
public class ShopConstant {

    //店铺营业状态在 Redis 中的 key
    public static final String KEY = "SHOP_STATUS";

    //外卖自动接单开关，1 开启 0 关闭
    public static final String AUTO_ACCEPT_TAKEOUT_KEY = "SHOP_AUTO_ACCEPT_TAKEOUT";

    //堂食自动接单开关，1 开启 0 关闭
    public static final String AUTO_ACCEPT_DINING_KEY = "SHOP_AUTO_ACCEPT_DINING";
}
