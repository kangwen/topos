package com.topos.admin.common.utils;

import java.util.UUID;

public final class IdUtils {

    private IdUtils() {
    }

    public static String fastUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
