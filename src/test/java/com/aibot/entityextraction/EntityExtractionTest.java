package com.aibot.entityextraction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.aibot.entity.QA;
import com.aibot.qa.GlobalConstants;
import com.aibot.qa.LibraryUtil;
import com.aibot.qa.Parser;
import com.aibot.qa.SynonymMappingAndLemmatization;
import com.google.common.collect.SetMultimap;
import com.wanasit.chrono.Chrono;
import com.wanasit.chrono.ChronoOption;
import com.wanasit.chrono.ParsedResult;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractionTest {

    static EntityExtractionUtil entityExtractionUtil;
    public static void setup() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
        //System.out.println(Parser.wordTokenize("only me_").toString());

    }

    //no use
    public static void setupAllEntity(){
        SynonymMappingAndLemmatization.init(GlobalConstants.synonymMappingFilePath);
        entityExtractionUtil = new EntityExtractionUtil(SynonymMappingAndLemmatization.synMapping.keySet()).caseInsensitive().lemmatizePatterns().loadEntityExtractions();
        entityExtractionUtil.getEntityExtractionList().forEach(e -> e.setEntityName("$" + e.getName().toLowerCase()));
        //filter
        //entityExtractionUtil.getEntityExtractionList().removeIf(e -> !e.getName().contains("offer"));
        LibraryUtil.init();
    }

    public void geoTest(){
        String path = "sg_address.txt";
        LocationFromAddress lc = new LocationFromAddress();
        lc.locationFromAddress(path);

        System.out.println("9 test/me 9".replaceAll("^\\d+", ""));
        System.out.println("574623".replaceAll("^\\d+", ""));
    }

    public void phoneTest(){
        String text = "12 34 56 78";
        PhoneNumberExtraction phoneNumberExtraction = new PhoneNumberExtraction();
        SetMultimap<String[], Integer> result = phoneNumberExtraction.searchPatternToPosIndex(text.split("\\s+"));
        result.asMap().entrySet().forEach(e -> {
            System.out.println(String.join(" ",e.getKey()) + "\t" + e.getValue());
        });
    }

    public void emailTest(){
        String text = "abc_def@yahoo.com";
        EmailExtraction emailExtraction = new EmailExtraction();
        SetMultimap<String[], Integer> result = emailExtraction.searchPatternToPosIndex(text.split("\\s+"));
        result.asMap().entrySet().forEach(e -> {
            System.out.println(String.join(" ",e.getKey()) + "\t" + e.getValue());
        });
    }

    public void dateTest(){
        String text = "friday 8pm";
        text = text.toLowerCase().replace("utc","");
        DateExtraction dateExtraction = new DateExtraction();
        SetMultimap<String[], Integer> result = dateExtraction.searchPatternToPosIndex(Parser.wordTokenize(text).stream().toArray(String[]::new));
        result.asMap().entrySet().forEach(e -> {
            String date = String.join(" ",e.getKey());
            System.out.println( date+ "\t" + DateExtraction.formatDate(date)+ e.getValue());
        });
    }

    public void customizeDate(){
        List<String> cases = new ArrayList<>();
        cases.add("30 Oct 12:30pm");
        cases.add("Oct 12 13:30pm");
        cases.add("tmr 6pm");
        cases.add("7 dinner");
        cases.add("January 1, 2017 at 8pm");
        cases.add("7 pm today");
        cases.add("8pm friday");

        ChronoOption options = ChronoOption.casualOptions();
        options.parsers.remove(1);
        options.parsers.remove(12);
        //options.parsers.add(new ENCasualDateParser());
        options.parsers.add(new ENCustomizeTimeExpressionParser());
        options.parsers.add(new ENCustomizeMonthNameLittleEndianParser());
        options.parsers.add(new ENCustomizeDayOfWeekDateFormatParser());

        Chrono chrono = new Chrono(options);

        for(String s: cases){
            System.out.println(s);
            List<ParsedResult> dates = chrono.parse(s);//Chrono.casual.parse

            for(ParsedResult date: dates){
                //String[] words = com.crayon.qa.Parser.wordTokenize(date.text).stream().toArray(String[]::new);
                String s_date = date.text;
                System.out.println( date+ "\t" + DateExtraction.getInstance().formatDate(s_date));
                /*if(!BookingState.isValidDate(DateExtraction.getDate(s_date), new Date())){
                    Date today = new Date();
                    System.out.println("Please choose another time! The date you typed is in the past, today is: " + today.toString());
                }*/
            }
        }
    }

    public void replaceTest(){
        String text = "Thanks.  Following are details for booking at $RestaurantName. Please Confirm.";
        String sentence = text.replace("$RestaurantName","nn");

        System.out.println(sentence);
    }

    public void timeTest(){
        String text = "7pm tmr";
        TimeExtraction timeExtraction = new TimeExtraction();
        SetMultimap<String[], Integer> result = timeExtraction.searchPatternToPosIndex(text.split("\\s+"));
        result.asMap().entrySet().forEach(e -> {
            System.out.println(String.join(" ",e.getKey()) + "\t" + e.getValue());
        });
    }

    public void numOfPaxTest(){
        /*
        1 adult
        5adults 2children
        5adults
        5 adults and 2 children
        abc@xyz.com and Dec25 6pm and change to 12 adults and 5 kids instead
         */
        String text = "7 adult 8 kid today 9pm"; //none . change to tomorrow 8 pm for 7pax
        PaxExtraction paxExtraction = new PaxExtraction();
        //System.out.println(text);
        SetMultimap<String[], Integer> result = paxExtraction.searchPatternToPosIndex(Parser.wordTokenize(text).stream().toArray(String[]::new));
        result.asMap().entrySet().forEach(e -> {
            String pax = String.join(" ",e.getKey());
            System.out.println( pax+ "\t" + e.getValue());
            Tuple2<Integer,Integer> temp = EntityExtractionUtil.getNumberOfPax(pax);
            System.out.println(EntityExtractionUtil.formatOfPax(temp));

        });

    }

    public void entityExtractionTest() {
        setupAllEntity();
        String text = "7 adult 8 kid today 9pm";

        //testing entity extraction in flow management
        List<String> patternWords = Parser.wordTokenize(text);

        //System.out.println(patternWords.toString());
        List<EntityExtractionUtil.EntityExtractionResult> entityExtractionResults = entityExtractionUtil.extractEntityPartial(patternWords.stream().toArray(String[]::new));
        if( entityExtractionResults.stream().filter(x->x.getEntityName().equalsIgnoreCase("#location")).count() > 0){
            EntityExtractionUtil.EntityExtractionResult locationWithPreposition = entityExtractionResults.stream()
                    .filter(x->x.getEntityName().equalsIgnoreCase("#location")).findFirst().get();
            String[] locationArray = Arrays.copyOfRange(locationWithPreposition.getEntityValue(), 1, locationWithPreposition.getEntityValue().length);
            EntityExtractionUtil.EntityExtractionResult location = new EntityExtractionUtil.EntityExtractionResult("$location",locationArray,locationWithPreposition.getStartIndex()+1);
            entityExtractionResults.add(location);
        }

        System.out.println("Entity Extraction All: "+ entityExtractionResults);
        List<EntityExtractionUtil.EntityExtractionResult> selectEEResults = EntityExtractionUtil.choosePartialMatch(entityExtractionResults);
        //entityExtractionResults.removeIf(e -> selectEEResults.contains(e));
        System.out.println("Entity Extraction After Merging: "+ selectEEResults);

        entityExtractionResults.removeIf(e -> selectEEResults.contains(e)
                || e.getMatchScore()>=1.0
                || !selectEEResults.stream().anyMatch(x -> x.getEntityName().equalsIgnoreCase(e.getEntityName()))
                || e.getEntityName().equalsIgnoreCase("#restaurantentity")
                || (e.getEntityName().equalsIgnoreCase("#location") & entityExtractionResults.stream().anyMatch(x -> e.getEntityName().equalsIgnoreCase("$location"))));

        List<String> suggestions = new ArrayList<>();
        for (EntityExtractionUtil.EntityExtractionResult c: entityExtractionResults){
            suggestions.add(String.join(" ", c.getEntityValue()));
        }

        //choose the shortest
        Map<String,Set<String>> candidateMap = EntityExtractionUtil.getCandidateMap(entityExtractionResults);

        System.out.println("Suggestion: "+ QA.getTopTargets(3,candidateMap));
        System.out.println("Partial match: "+ candidateMap.toString());

        //testing match pattern words in flow management
        selectEEResults.stream().sorted((a, b) -> b.getStartIndex() - a.getStartIndex()).forEach(r -> {
            List<String> matchedPattern = patternWords.subList(r.getStartIndex(), r.getEndIndex() +1);
            matchedPattern.clear();
            matchedPattern.add(r.getEntityName());
        });

        System.out.println("Question with Pattern Replacement: "+ patternWords);
    }

    public void intentTest(){
        setupAllEntity();
        /*String question = "$accolades restaurant";

        List<String> sentences = Parser.getSentencesfromText(question);

        Tuple2<LibraryUtil.Pattern, Double> pattern_score = sentences.stream().map(sen -> {
            Tuple2<LibraryUtil.Pattern,Double> rs  = LibraryUtil.patternClassification(Arrays.asList(sen.split("\\s+")), LibraryUtil.flatContextsMap);
            System.out.println(rs._1().getUserInputPattern() + "\t" + rs._2());
            return rs;
        }).sorted((x, y) -> (int) (y._2 - x._2)).findFirst().get();

        System.out.println(pattern_score._1.getQuestionPatternWords()+" Score: "+pattern_score._2);*/

    }

    public void lemmaTest(){
        System.out.println(SynonymMappingAndLemmatization.run("On Sunday, Oct 9 at 6:30PM"));
        System.out.println(Parser.wordTokenize("only me."));

    }

    public void isNoun(){
        String word = "authentic";
        System.out.println(Parser.isNoun(word));
    }

    public void stemTest(){
        String word = "fusionopolis";
        System.out.println(Parser.stem(word));
    }

    public void tokenTest(){
        String text = "how about mc donald's in orchard?";
        System.out.println(Parser.wordTokenize(text));
       // System.out.println(Parser.tokenize(text));
    }

    public void regexTest() throws ParseException {
        String abc = "today at 9pm";
        System.out.println(abc.replaceAll("\\b@\\b","at"));
        Pattern punctPattern = Pattern.compile("\\p{Punct}");
        String[] text = abc.split("\\s+");
        for(int i=0; i< text.length; i++){
            Matcher matcher = punctPattern.matcher(text[i]);
            if(matcher.find()){
                System.out.println(matcher.group());
            }
        }
    }

    public void locationTest(){
        System.out.println(LocationFromAddress.getCityAndCountryByLocation("civil line"));
    }
}