package com.aibot.qa;

import java.io.Serializable;

public class GlobalConstants implements Serializable {

  public static final String taxonomiesBucketNameS3 = "tastebot-dictionary-release-march";
  public static final String taxonomiesFolderNameS3 = "QATaxonomy";
  public static final String qaFolderNameS3 = "QAExtend";
  public static final String patternFilePath = "Pattern.tsv";
  public static final String contextFilePath = "Context.tsv";
  public static final String synonymMappingFilePath = "SynonymMapping.tsv";
  public static final String locationLatLongFile = "QAExtend/qa_Geolocation.tsv";
  public static final String citiesCoveredFile = "QAExtend/qa_CitiesCovered.tsv";
  public static final String paxDictionary = "QAExtend/qa_Pax.tsv";
  public static final String countryMapping = "QAExtend/qa_Country_Nationality_Mapping.tsv";
  public static final String distanceDictionary = "QAExtend/qa_Distance.tsv";


  public static final String solrUrl = "http://10.10.14.56:8983/solr/sg_rest_v2/";
  public static final String solrUrl_V2 = "http://localhost:8983/solr/LocalBusiness";

    // Mysql Config
  public static final String mysqlIP = "march-release.cwo9o3ioyvzi.us-east-1.rds.amazonaws.com";
  public static final String mysqlPORT = "3306";
  public static final String mysqlUser = "tastebot";
  public static final String mysqlPass = "tastebot";
  public static final String mysqlDB = "CrayonBot";

//  public static final String mysqlIP = "10.10.14.219";
//  public static final String mysqlPORT = "3306";
//  public static final String mysqlUser = "root";
//  public static final String mysqlPass = "root";
//  public static final String mysqlDB = "CrayonBot";

  public static final int sessionInterval = 10;
  public static final int refineQuestionToleranceInterval = 5;

  //sentiment
  public static final String negativePhrasesPath = "sentiment/negative-phrases.txt";
  public static final String postNegPhrasesPath = "sentiment/post-negative-phrases.txt";
  public static final String conjunctionsFilePath = "sentiment/conjunctions.txt";
  public static final int preNegativePhraseWindow = 0;
  public static final int postNegativePhraseWindow = 2;

  public enum Entity {
    $actionword,
    $beverage,
    $chef,
    $cookingmethod,
    $country,
    $dish,
    $establishmenttype,
    $event,
    $ingredient,
    $location,
    $mealtype,
    $nationality,
    $refineestablishmenttype,
    $refinelocation,
    $religious,
    $restaurantfeature,
    $accompany,
    $occasion,
    $regular,
    $restaurantname,
    location, //original is #location, be careful
    cuisine,
    restaurantentity,
    $pricerange,
    $offer,
    $accolade,
    $regional,
    $distance,
    $city
  }

}
