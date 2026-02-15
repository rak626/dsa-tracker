package com.rakesh.dsa.tracker.utils;

import java.net.URI;

public interface QuestionServiceUtil {
    static String detectPlatform(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost(); // e.g. "leetcode.com"

            if (host == null) return "UNKNOWN";

            // remove www.
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            // get first part before dot
            String platform = host.split("\\.")[0];

            return platform.toLowerCase();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    static String detectProblemName(String url) {
        try {
            URI uri = new URI(url);

            String path = uri.getPath(); // e.g. /problems/two-sum/
            if (path == null || path.isBlank()) return "UNKNOWN";

            // split by '/'
            String[] parts = path.split("/");

            // get last non-empty segment
            String slug = null;
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i] != null
                        && !parts[i].isBlank()
                        && !parts[i].matches("\\d+")
                        && !parts[i].equalsIgnoreCase("description")) {
                    slug = parts[i];
                    break;
                }
            }

            if (slug == null) return "UNKNOWN";

            // replace hyphens/underscores with space
            slug = slug.replace("-", " ").replace("_", " ");

            // convert to Title Case
            String[] words = slug.split("\\s+");
            StringBuilder title = new StringBuilder();

            for (String w : words) {
                if (w.isBlank()) continue;

                // Capitalize first letter
                title.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }

            return title.toString().trim();

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
