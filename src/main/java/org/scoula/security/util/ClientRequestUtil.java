package org.scoula.security.util;

import javax.servlet.http.HttpServletRequest;

public final class ClientRequestUtil {
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String APP_CLIENT_TYPE = "APP";

    private ClientRequestUtil() {
    }

    public static boolean isApp(HttpServletRequest request) {
        return APP_CLIENT_TYPE.equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER));
    }
}
