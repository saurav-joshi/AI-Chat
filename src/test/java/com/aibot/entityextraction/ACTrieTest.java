package com.aibot.entityextraction;

import com.google.common.collect.SetMultimap;

import java.util.ArrayList;
import java.util.List;

public class ACTrieTest {


    public void testConstructACTrie() throws Exception {
        List<String[]> patterns = new ArrayList<>();
        patterns.add("Craystal jade kitchen".split("\\s"));
        patterns.add("Craystal star".split("\\s"));
        patterns.add("Craystal jade seafood store".split("\\s"));


        List<String[]> patterns1 = new ArrayList<>();
        patterns1.add("Craystal jade kitchen".split("\\s"));
        patterns1.add("Jade house".split("\\s"));
        patterns1.add("Craystal jade seafood store".split("\\s"));

        List<String[]> patterns2 = new ArrayList<>();
        patterns2.add("Craystal jade kitchen".split("\\s"));
        patterns2.add("jade kitchen".split("\\s"));
        patterns2.add("Craystal jade seafood store".split("\\s"));
        ACTrie acTrie = new ACTrie(patterns2, "test");

        String [] query = "jade kitchen".split("\\s");
        String [] query1 = "Craystal jade house".split("\\s");
        SetMultimap<Integer, Integer> results = acTrie.search(query1,true);
        System.out.println(results);

    }

}