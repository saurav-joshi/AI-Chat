package com.aibot.spelling;

import com.aibot.qa.LibraryUtil;

import java.util.List;
import java.util.Map;

public class SpellingCorrectionTest {
    public void testSpelling() {
        LibraryUtil.init();
        SpellingCorrection sc = new SpellingCorrection();
        String text = "mor"; //error no candidate found
        Map<String, List<String>> candidates = sc.spellingCandidates(text);
        System.out.println(candidates.toString());
        System.out.println(sc.replaceSpelling(text,candidates));

    }
}