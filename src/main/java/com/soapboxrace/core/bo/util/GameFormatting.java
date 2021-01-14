package com.soapboxrace.core.bo.util;

public class GameFormatting {
    private GameFormatting() {
    }

    public static String carClassHashToName(Integer hash) {
        switch (hash) {
            case -2142411446:
                return "S";
            case -405837480:
                return "A";
            case -406473455:
                return "B";
            case 1866825865:
                return "C";
            case 415909161:
                return "D";
            case 872416321:
                return "E";
            case 607077938:
                return "Open";
            default:
                return "UNKNOWN: " + hash;
        }
    }
}
