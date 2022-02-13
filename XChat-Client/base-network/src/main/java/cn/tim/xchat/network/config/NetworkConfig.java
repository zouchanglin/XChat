package cn.tim.xchat.network.config;

import cn.tim.xchat.network.BuildConfig;

public interface NetworkConfig {
    String baseUrl = BuildConfig.SERVER_URL;

    String USER_LOGIN_URL = "/user/login";

    String USER_REGISTER_URL = "/user/register";
}
