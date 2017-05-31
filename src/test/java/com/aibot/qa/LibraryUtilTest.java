package com.aibot.qa;

import edu.emory.mathcs.backport.java.util.Arrays;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

public class LibraryUtilTest {

    public void testPatternClassification() throws Exception {
        LibraryUtil.init();
        List<String> words = new ArrayList<>(Arrays.asList(new String[]{"none"}));//new ArrayList<>();
        //words.add("$offer");
//        Tuple2<LibraryUtil.Pattern,Double> results = LibraryUtil.patternClassification(words, LibraryUtil.flatContextsMap);
//        System.out.println(results._1.getId() + "--" + results._1.getLibraryName() + "--" + results._1.getUserInputPattern() + "--" + results._2);
        List<Tuple2<LibraryUtil.Pattern, Double>> results = LibraryUtil.patternClassificationMultiple(words, LibraryUtil.flatContextsMap);
        System.out.println("The top matched patterns are:");
        results.forEach(x -> {
            System.out.println(x._1.getId() + "--" + x._1.getLibraryName() + "--" + x._1.getUserInputPattern() + "--" + x._2);
        });


    }
}