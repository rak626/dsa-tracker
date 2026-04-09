package com.rakesh.dsa.tracker.unit;

import com.rakesh.dsa.tracker.model.dto.DateFilterEnum;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class DateFilterEnumTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    
    @Test
    void today_cutoff_returnsStartOfToday() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.TODAY.cutoff(clock);
        LocalDate expected = LocalDate.now(clock);
        
        assertEquals(expected.atStartOfDay(UTC).toInstant(), cutoff);
    }

    @Test
    void yesterday_cutoff_returnsStartOfYesterday() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.YESTERDAY.cutoff(clock);
        LocalDate expected = LocalDate.now(clock).minusDays(1);
        
        assertEquals(expected.atStartOfDay(UTC).toInstant(), cutoff);
    }

    @Test
    void lastTwoDays_cutoff_returnsTwoDaysAgo() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.LAST_2_DAYS.cutoff(clock);
        Instant expected = Instant.parse("2024-01-13T14:30:00Z");
        
        assertEquals(expected, cutoff);
    }

    @Test
    void lastFiveDays_cutoff_returnsFiveDaysAgo() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.LAST_5_DAYS.cutoff(clock);
        Instant expected = Instant.parse("2024-01-10T14:30:00Z");
        
        assertEquals(expected, cutoff);
    }

    @Test
    void thisWeek_cutoff_returnsStartOfWeek() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.THIS_WEEK.cutoff(clock);
        Instant expected = Instant.parse("2024-01-15T00:00:00Z");
        
        assertEquals(expected, cutoff);
    }

    @Test
    void thisMonth_cutoff_returnsFirstOfMonth() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.THIS_MONTH.cutoff(clock);
        LocalDate expected = LocalDate.of(2024, 1, 1);
        
        assertEquals(expected.atStartOfDay(UTC).toInstant(), cutoff);
    }

    @Test
    void thisYear_cutoff_returnsFirstOfYear() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.THIS_YEAR.cutoff(clock);
        LocalDate expected = LocalDate.of(2024, 1, 1);
        
        assertEquals(expected.atStartOfDay(UTC).toInstant(), cutoff);
    }

    @Test
    void allTime_cutoff_returnsMinInstant() {
        Clock clock = Clock.fixed(
            Instant.parse("2024-01-15T14:30:00Z"), UTC
        );
        
        Instant cutoff = DateFilterEnum.ALL_TIME.cutoff(clock);
        
        assertEquals(Instant.MIN, cutoff);
    }

    @Test
    void valueOf_validName_returnsEnum() {
        assertEquals(DateFilterEnum.TODAY, DateFilterEnum.valueOf("TODAY"));
        assertEquals(DateFilterEnum.THIS_WEEK, DateFilterEnum.valueOf("THIS_WEEK"));
        assertEquals(DateFilterEnum.THIS_MONTH, DateFilterEnum.valueOf("THIS_MONTH"));
    }

    @Test
    void valueOf_invalidName_throwsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> DateFilterEnum.valueOf("INVALID"));
    }
}
