package com.aibot.entityextraction;

import com.google.common.collect.SetMultimap;
import scala.Tuple2;

import java.util.List;

public interface EntityExtraction{

    /**
     * search patternsForState and indices of the patternsForState from a given array
     * @param text
     * @return patternsForState and start indices of the pattern
     */
    SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text);

    SetMultimap<String[], Tuple2<Integer, String[]>> searchPartial(String[] text);

    EntityExtraction setEntityName(String entityName);

    String getName();

    List<String[]> getPatterns();

    EntityExtraction setPatterns(List<String[]> patterns);

    boolean isPartialMatch();

}
