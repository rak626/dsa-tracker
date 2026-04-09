package com.rakesh.dsa.tracker.unit;

import com.rakesh.dsa.tracker.utils.QuestionServiceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class QuestionServiceUtilTest {

    @Test
    void detectPlatform_withLeetCodeUrl_returnsLeetCode() {
        String url = "https://leetcode.com/problems/two-sum/";
        assertEquals("leetcode", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withWwwLeetCodeUrl_returnsLeetCode() {
        String url = "https://www.leetcode.com/problems/two-sum/";
        assertEquals("leetcode", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withGeekForGeeksUrl_returnsGeeksForGeeks() {
        String url = "https://practice.geeksforgeeks.org/problems/two-sum/";
        assertEquals("practice", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withWwwGfgUrl_returnsGfg() {
        String url = "https://www.geeksforgeeks.org/two-sum/";
        assertEquals("geeksforgeeks", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withCodeChefUrl_returnsCodeChef() {
        String url = "https://www.codechef.com/problems/";
        assertEquals("codechef", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withHackerRankUrl_returnsHackerRank() {
        String url = "https://www.hackerrank.com/challenges/two-sum/problem";
        assertEquals("hackerrank", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withUnknownUrl_returnsUnknown() {
        String url = "https://www.unknown-site.com/problem";
        assertEquals("unknown-site", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withEmptyUrl_returnsUnknown() {
        String url = "";
        assertEquals("UNKNOWN", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectPlatform_withNullUrl_returnsUnknown() {
        String url = null;
        assertEquals("UNKNOWN", QuestionServiceUtil.detectPlatform(url));
    }

    @Test
    void detectProblemName_withLeetCodeSlug_returnsTitleCase() {
        String url = "https://leetcode.com/problems/two-sum/";
        assertEquals("Two Sum", QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withMultipleHyphens_returnsTitleCase() {
        String url = "https://leetcode.com/problems/longest-substring-without-repeating-characters/";
        assertEquals("Longest Substring Without Repeating Characters", 
                     QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withNumber_returnsFormattedName() {
        String url = "https://leetcode.com/problems/2-sum/";
        assertEquals("2 Sum", QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withGfgSlug_returnsTitleCase() {
        String url = "https://practice.geeksforgeeks.org/problems/two-sum/";
        assertEquals("Two Sum", QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withComplexSlug_returnsTitleCase() {
        String url = "https://leetcode.com/problems/valid-parentheses/";
        assertEquals("Valid Parentheses", QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withEmptyUrl_returnsUnknown() {
        String url = "";
        assertEquals("UNKNOWN", QuestionServiceUtil.detectProblemName(url));
    }

    @Test
    void detectProblemName_withNullUrl_returnsUnknown() {
        String url = null;
        assertEquals("UNKNOWN", QuestionServiceUtil.detectProblemName(url));
    }

    @ParameterizedTest
    @CsvSource({
            "https://leetcode.com/problems/two-sum/, 'Two Sum'",
            "https://www.geeksforgeeks.org/two-sum/, 'Two Sum'",
            "https://leetcode.com/problems/valid-anagram/, 'Valid Anagram'"
    })
    void detectProblemName_withVariousUrls_returnsExpected(String url, String expected) {
        assertEquals(expected, QuestionServiceUtil.detectProblemName(url));
    }
}
