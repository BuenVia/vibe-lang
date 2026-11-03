package com.vibelang.review;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

final class Zones {
    private Zones() {}

    /** Parses an IANA zone id such as "Europe/Madrid"; blank means UTC. */
    static ZoneId parse(String zone) {
        if (zone == null || zone.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(zone.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown time zone: " + zone);
        }
    }
}
