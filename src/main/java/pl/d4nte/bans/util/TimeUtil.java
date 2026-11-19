package pl.d4nte.bans.util;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static long parseDuration(String input) {
        // Supports 10s 30m 12h 1d 2w (case-insensitive)
        input = input.trim().toLowerCase(Locale.ROOT);
        long total = 0L;
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                long n = num.length() == 0 ? 0 : Long.parseLong(num.toString());
                switch (c) {
                    case 's': total += TimeUnit.SECONDS.toMillis(n); break;
                    case 'm': total += TimeUnit.MINUTES.toMillis(n); break;
                    case 'h': total += TimeUnit.HOURS.toMillis(n); break;
                    case 'd': total += TimeUnit.DAYS.toMillis(n); break;
                    case 'w': total += TimeUnit.DAYS.toMillis(7 * n); break;
                    default: throw new IllegalArgumentException("Unknown time suffix: " + c);
                }
                num = new StringBuilder();
            }
        }
        if (num.length() > 0) {
            // default seconds
            total += TimeUnit.SECONDS.toMillis(Long.parseLong(num.toString()));
        }
        return total;
    }

    public static String formatUntil(long millisEpoch) {
        java.time.ZonedDateTime zdt = java.time.Instant.ofEpochMilli(millisEpoch).atZone(java.time.ZoneId.systemDefault());
        return zdt.toString().replace('T', ' ');
    }

    public static long now() {
        return Instant.now().toEpochMilli();
    }
}