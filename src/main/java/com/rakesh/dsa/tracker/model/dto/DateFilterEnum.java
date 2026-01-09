package com.rakesh.dsa.tracker.model.dto;

import java.time.*;
import java.time.temporal.ChronoUnit;

public enum DateFilterEnum {

    TODAY {
        @Override
        public Instant cutoff(Clock clock) {
            return startOfToday(clock);
        }
    },

    YESTERDAY {
        @Override
        public Instant cutoff(Clock clock) {
            return startOfToday(clock).minus(1, ChronoUnit.DAYS);
        }
    },

    LAST_2_DAYS {
        @Override
        public Instant cutoff(Clock clock) {
            return Instant.now(clock).minus(2, ChronoUnit.DAYS);
        }
    },

    LAST_5_DAYS {
        @Override
        public Instant cutoff(Clock clock) {
            return Instant.now(clock).minus(5, ChronoUnit.DAYS);
        }
    },

    THIS_WEEK {
        @Override
        public Instant cutoff(Clock clock) {
            ZonedDateTime now = ZonedDateTime.now(clock);
            return now
                    .with(DayOfWeek.MONDAY)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
    },

    THIS_MONTH {
        @Override
        public Instant cutoff(Clock clock) {
            ZonedDateTime now = ZonedDateTime.now(clock);
            return now
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
    },

    THIS_YEAR {
        @Override
        public Instant cutoff(Clock clock) {
            ZonedDateTime now = ZonedDateTime.now(clock);
            return now
                    .withDayOfYear(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toInstant();
        }
    },

    ALL_TIME {
        @Override
        public Instant cutoff(Clock clock) {
            return null;
        }
    };

    public abstract Instant cutoff(Clock clock);

    public Instant cutoff() {
        return cutoff(Clock.systemDefaultZone());
    }

    private static Instant startOfToday(Clock clock) {
        return ZonedDateTime.now(clock)
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    public static DateFilterEnum fromString(String value) {
        for (DateFilterEnum filter : values()) {
            if (filter.name().equalsIgnoreCase(value)) {
                return filter;
            }
        }
        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}
