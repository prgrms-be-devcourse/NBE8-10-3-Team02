package com.back.standard.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class TimeUt {
    public static class epoch {

        public static LocalDate toLocalDate(Long epochSeconds) {
            if (epochSeconds == null) return null;
            return Instant.ofEpochSecond(epochSeconds)
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDate();
        }
    }
}