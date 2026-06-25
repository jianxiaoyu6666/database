package com.myproject.util;

//一个工具类，不要细究太多，只要理解CURRENT_ID，CURRENT_USERTYPE分别是用来获取当前登录的这个用户的ID和他的UserType即可
//
public class CurreetHolder {
    private static final ThreadLocal<Long> CURRENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERTYPE = new ThreadLocal<>();
    public static void setCurrentId(Long ID){
        CURRENT_ID.set(ID);
    }
    public static void setCurrentUsertype(String userType){
        CURRENT_USERTYPE.set(userType);
    }
    public static Long getCurrentId(){
        return CURRENT_ID.get();
    }
    public static String getCurrentUserType(){
        return CURRENT_USERTYPE.get();
    }

    public static void remove(){
        CURRENT_ID.remove();
        CURRENT_USERTYPE.remove();
    }

}
