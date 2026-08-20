package com.zhiyuan.college.security;

import com.zhiyuan.college.model.entity.UserAccount;

public final class UserContext {

    private static final ThreadLocal<UserAccount> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserAccount user) {
        HOLDER.set(user);
    }

    public static UserAccount get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

