package cn.tim.xchat.utils;

import java.util.Random;

public class KeyUtil {
    static SnowflakeIdWorker3rd worker3rd = new SnowflakeIdWorker3rd();

    /**
     * 生成唯一的主键
     */
    public static synchronized String genUniqueKey() {
        Random random = new Random();
        Integer number = random.nextInt(900000) + 100000;

        return System.currentTimeMillis() + String.valueOf(number);
    }


    /**
     * 生成唯一的主键
     */
    public static synchronized String genUserKey() {
        return String.valueOf(worker3rd.nextId());
    }
}
