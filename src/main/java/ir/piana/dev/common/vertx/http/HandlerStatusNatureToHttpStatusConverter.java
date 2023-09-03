package ir.piana.dev.common.vertx.http;

import ir.piana.dev.common.handler.HandlerStatusNature;
import org.springframework.stereotype.Component;

public class HandlerStatusNatureToHttpStatusConverter {
    public static int toHttpStatus(HandlerStatusNature handlerStatusNature) {
        switch (handlerStatusNature) {
            case UNKNOWN -> {
                return 500;
            }
            case INFORMATION -> {
                return 100;
            }
            case SUCCESS -> {
                return 200;
            }
            case REDIRECTION -> {
                return 300;
            }
            case CLIENT_ERROR -> {
                return 400;
            }
            case SERVER_ERROR -> {
                return 500;
            }
        }
        return 500;
    }
}
