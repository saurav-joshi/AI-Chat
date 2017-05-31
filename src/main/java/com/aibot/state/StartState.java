package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.entity.Suggestion;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.LibraryUtil;
import com.aibot.qa.Parser;
import com.aibot.entityextraction.LocationFromAddress;
import scala.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StartState extends State {

    public void setStateType() {
        this.stateType = StateType.StartState;
    }

    public StartState() {

    }

    public StateType process(Conversation conversation) {
        try{
            String question = String.join(" ", conversation.getLatestQA().getCleanedQuestionPatternWords());

            // check whether it is a clarification
            if(conversation.getQaList().size() > 1){
                Suggestion secondLastQASuggestion = conversation.getQaList().get(conversation.getQaList().size() - 2).getAnswer().getSuggestion();
                if(secondLastQASuggestion != null){
                    List<String> options = secondLastQASuggestion.getOptions();
                    // TODO: 10/28/2016: may need to change the criteria for clarification and others
                    if(secondLastQASuggestion.getType() != null
                            && options !=null && options.contains(conversation.getLatestQueston())){
                        LibraryUtil.Pattern clarifyPattern = LibraryUtil.getRandomPatternByQuestionClass(secondLastQASuggestion.getType());
                        conversation.getLatestQA().setMatchedPatternIdInLibrary(clarifyPattern.getId());
                        return StateType.ClarificationState;
                    }
                }
            }


            List<String> sentences = Parser.getSentencesfromText(question);
            System.out.println("question before classification : "+sentences);

            Tuple2<LibraryUtil.Pattern, Double> pattern_score = getCorrectPattern(question,conversation);
            if(pattern_score == null){
                return StateType.ExpectationManagementState;
            }
            System.out.println("------ matched pattern:" + pattern_score._1.getLibraryName() + "--- score:" + pattern_score._2);
            conversation.getLatestQA().setMatchedPatternIdInLibrary(pattern_score._1.getId());
            System.out.println(pattern_score._1.getQuestionPatternWords()+" Score: "+pattern_score._2);
            System.out.println("matched pattern library:" + conversation.getLatestQA().getMatchedPattern().getLibraryName());

            if(conversation.getLatestQA().getQuestion().equalsIgnoreCase(BookingState.bookingActivate)
                    && !conversation.isActivateBooking() && conversation.getBookingRestaurantId()==0){//just type not book
                return StateType.ExpectationManagementState;
            }

            //always check city, even if no location mention, example 'restaurant near by'
            String city = conversation.getCurrentCityOfUser();
            if(!city.isEmpty()){
                if(city.contains("||")){
                    //make suggestion and clarify
                    conversation.getLatestQA().getAnswer().setMessage("What you are looking for is available in several cities.");
                    conversation.getLatestQA().getAnswer().setSuggestion(new Suggestion(LibraryUtil.getRandomPatternByQuestionClass("Clarify.MultiCity").getSystemMessage(),
                            Arrays.asList(city.trim().split("\\|\\|")), "Clarify.MultiCity"));
                    return StateType.EndState;
                }
                if(!LocationFromAddress.getCitiesCovered().contains(city)){
                    conversation.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("Warning.CoveredCity").getSystemMessage());
                    return StateType.EndState;
                }
                conversation.getLatestQA().setCity(city);
            }/*else {
                conversation.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("SystemRejectQuery").getSystemMessage());
                LibraryUtil.Pattern disPattern = LibraryUtil.getRandomPatternByQuestionClass("Clarify.ShareLocation");
                conversation.getLatestQA().getAnswer().setSuggestion(new Suggestion(disPattern.getSystemMessage(), Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")), "Clarify.ShareLocation"));
                return StateType.EndState;
            }*/
            //end checking city

            if (pattern_score._2 > 0.3){
                if(!conversation.getLatestEntities().stream().anyMatch(x -> x.getEntityName().contains("change"))
                        && BookingState.isBookingLoop(conversation)){
                    return StateType.BookingState;
                }
                else{
                    return Arrays.stream(StateType.values())
                            .filter(x -> x.toString().equals(pattern_score._1.getLibraryName()))
                            .findFirst()
                            .get();
                }
            }
            else if(BookingState.isBookingLoop(conversation)){
                return StateType.BookingState;
            }
            else {
                if(conversation.getLatestEntities().stream().filter(
                        x -> !(x.getEntityName().contains("phone") ||  x.getEntityName().contains("pax")
                                ||  x.getEntityName().contains("distance") || x.getEntityName().contains("changeintentwords")
                                ||x.getEntityName().contains("accompany") || x.getEntityName().contains("occasion") || x.getEntityName().contains("regular")
                                ||  x.getEntityName().contains("email") ||  x.getEntityName().contains("date")))
                        .count() > 0){
                    System.out.println("Can not classify the question, but have extracted entity ! ");
                    conversation.getLatestQA().setMatchedPatternIdInLibrary(30);
                    return StateType.ConsumerQueryState;
                }
                System.out.println("Can not classify the question and also no entity extracted !");
                return StateType.ExpectationManagementState;
            }
        }catch (Exception e){
            e.printStackTrace();
            return StateType.ExpectationManagementState;
        }
    }

    public void allowedInputStateTypes() {

    }

    public boolean guardCheck(Conversation con){
        return true;
    }

    public Tuple2<LibraryUtil.Pattern, Double> getCorrectPattern(String question, Conversation conversation){
        // Case -1: deal with empty question with geo only.
        if(question.trim().equals("")
                && conversation.getLatestQA().getOriginalQuestion().trim().equals("")
                && conversation.getLatestQA().getGeo() != null){
            // return as a consumer query where system refine answers are also checked
            return new Tuple2<>(LibraryUtil.getRandomPatternByQuestionClass("ConsumerQuery.ClassMember"), 1.0);
        }

        // Case 0: solve any entity confusion: 1). distance vs pax vs phone for number only case
        boolean flagOfNum = false;
        Matcher matcher = Pattern.compile("([0-9]*[.])?[0-9]+").matcher(conversation.getLatestQA().getOriginalQuestion());
        if(matcher.find()){
            String theNum =  matcher.group(0);
            String restOfString = conversation.getLatestQA().getOriginalQuestion().replaceAll(theNum,"");
            if(restOfString.trim().length() == 0){
                flagOfNum = true;
            }
        }
        if(flagOfNum && !BookingState.isBookingLoop(conversation) && (
                conversation.getLatestEntities().stream().filter(x->x.getEntityName().contains("$pax")).count() > 0 ||
                conversation.getLatestEntities().stream().filter(x->x.getEntityName().contains("$phone")).count() > 0)){

            // check if previous question is a special state
            QA secondLastQA = conversation.getQaList().size() < 2 ? null : conversation.getQaList().get(conversation.getQaList().size() - 2);
            if(secondLastQA == null ||
                    secondLastQA.getStatePaths().size() == 0 ||
                    secondLastQA.getStatePaths().contains(StateType.UserGreetingState) ||
                    secondLastQA.getStatePaths().contains(StateType.MotivationState) ||
                    secondLastQA.getStatePaths().contains(StateType.HelpState) ||
                    secondLastQA.getStatePaths().contains(StateType.ExpectationManagementState) ||
                    secondLastQA.getStatePaths().contains(StateType.FixedAnswerState) ||
                    secondLastQA.getStatePaths().contains(StateType.BookingState)){
                return null;
            }

            // change the entity to distance
            List<EntityExtractionUtil.EntityExtractionResult> newEntities = new ArrayList<>();
            for(EntityExtractionUtil.EntityExtractionResult entity : conversation.getLatestEntities()){
                if(entity.getEntityName().contains("$pax") || entity.getEntityName().contains("$phone")){
                    newEntities.add(new EntityExtractionUtil.EntityExtractionResult("$distance", entity.getEntityValue(), entity.getStartIndex()));
                }else{
                    newEntities.add(entity);
                }
            }
            conversation.getLatestQA().setEntities(newEntities);
            question = question.replace("$phone","$distance");
            question = question.replace("$pax","$distance");
        }

        // Case 1: use the original question to match the patterns, to handle some fixed answer problems
        if(conversation.getLatestQA().getOriginalQuestion() != null && conversation.getLatestQA().getOriginalQuestion().trim().length() != 0){
            Tuple2<LibraryUtil.Pattern, Double> matchOriginal = matchSentencesWithPattern(Arrays.asList(String.join(" ", Parser.lemmatizeAndLowercaseText(conversation.getLatestQA().getOriginalQuestion()))), conversation);
            System.out.println("Case 1: try with original question:" + conversation.getLatestQA().getOriginalQuestion() + " --matching score:" + matchOriginal._2);
            if(matchOriginal._2 > 0.8){
                return matchOriginal;
            }
        }

        // Case 2: In case of multiple sentences in the question, match as one sentences, to handle the long patterns (e.g, multiple sentence as well)
        List<String> sentences = Parser.getSentencesfromText(question);
        if(sentences.size() > 1){
            Tuple2<LibraryUtil.Pattern, Double> matchOneSentence = matchSentencesWithPattern(Arrays.asList(question), conversation);
            System.out.println("Case 2: try with question pattern (one sentence):" + question + " --matching score:" + matchOneSentence._2);
            if(matchOneSentence._2 >= 0.8){
                return matchOneSentence;
            }
        }

        // Case 3: match each sentence one by one, and find the optimal matched one.
        Tuple2<LibraryUtil.Pattern, Double> matchOptimal = matchSentencesWithPattern(sentences, conversation);
        System.out.println("Case 3: try with question pattern (optimal sentence):" + question + " --matching score:" + matchOptimal._2);
        return matchOptimal;
    }

    // given the current conversation and the sentences in the query, find the correct pattern
    public Tuple2<LibraryUtil.Pattern, Double> matchSentencesWithPattern(List<String> sentences, Conversation conversation){
        // calculate the match score for each sentence, and track the maxscore and the corresponding sentence
        Map<String,List<Tuple2<LibraryUtil.Pattern,Double>>> sentenceScoreMap = new HashMap<>();
        String maxSentence = null;
        double maxScore = -1;
        for(String sentence : sentences){
            List<Tuple2<LibraryUtil.Pattern,Double>> pattern_scores = LibraryUtil.patternClassificationMultiple(new ArrayList<>(Arrays.asList(sentence.split("\\s+"))),LibraryUtil.flatContextsMap);
            sentenceScoreMap.put(sentence, pattern_scores);
            if(pattern_scores.get(0)._2 > maxScore){
                maxScore = pattern_scores.get(0)._2;
                maxSentence = sentence;
            }
        }

        // get the sentence with the highest score
        if(sentenceScoreMap.get(maxSentence).stream().map(x->x._1.getLibraryName()).distinct().count() == 1){
            System.out.println("******* Single pattern state is matched *******" );
            return sentenceScoreMap.get(maxSentence).get(new Random().nextInt(sentenceScoreMap.get(maxSentence).size()));
        }else{
            System.out.println("******* Multiple patterns states are matched *******");
            Set<String> potentialStates = sentenceScoreMap.get(maxSentence).stream().map(x->x._1.getLibraryName()).collect(Collectors.toSet());
            System.out.println("******* " + potentialStates);

            QA secondLastQA = conversation.getQaList().size() < 2 ? null : conversation.getQaList().get(conversation.getQaList().size() - 2);
            if(potentialStates.contains("UserRefineState") && potentialStates.contains("ConsumerQueryState")){
                if(secondLastQA == null ||
                        secondLastQA.getStatePaths().size() == 0 ||
                        secondLastQA.getStatePaths().contains(StateType.UserGreetingState) ||
                        secondLastQA.getStatePaths().contains(StateType.MotivationState) ||
                        secondLastQA.getStatePaths().contains(StateType.HelpState) ||
                        //secondLastQA.getStatePaths().contains(StateType.ExpectationManagementState) ||
                        secondLastQA.getStatePaths().contains(StateType.BookingState)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("ConsumerQueryState")).findFirst().get();
                }

                if(secondLastQA.getStatePaths().contains(StateType.FixedAnswerState)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("ConsumerQueryState")).findFirst().get();
                }

                // for all the other cases such as: ShowMoreState, ShowMoreState+ConsumerQueryState, ConsumerQueryState, UserRefineState
                if(conversation.isLastQAWithinTolerantTime(conversation.getLatestQA(), secondLastQA)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("UserRefineState")).findFirst().get();
                }else{
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("ConsumerQueryState")).findFirst().get();
                }
            }else if(potentialStates.contains("UserRefineState") && potentialStates.contains("SystemRefineState")){
                if(secondLastQA == null ||
                        secondLastQA.getStatePaths().size() == 0 ||
                        secondLastQA.getStatePaths().contains(StateType.UserGreetingState) ||
                        secondLastQA.getStatePaths().contains(StateType.MotivationState) ||
                        secondLastQA.getStatePaths().contains(StateType.HelpState) ||
                        secondLastQA.getStatePaths().contains(StateType.ExpectationManagementState) ||
                        secondLastQA.getStatePaths().contains(StateType.BookingState)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("SystemRefineState")).findFirst().get();
                }
                if(secondLastQA.getStatePaths().contains(StateType.FixedAnswerState)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("SystemRefineState")).findFirst().get();
                }
                if(secondLastQA.getStatePaths().contains(StateType.ConsumerQueryState) &&
                        secondLastQA.getStatePaths().contains(StateType.SystemRefineState)){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("SystemRefineState")).findFirst().get();
                }
                // for the other cases:
                if(secondLastQA.getEntities().stream().filter(x->x.getEntityName().contains("#location")).count() > 0 || secondLastQA.getEntities().stream().filter(x->x.getEntityName().contains("$location")).count()>0){
                    return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("UserRefineState")).findFirst().get();
                }

                return sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("UserRefineState")).findFirst().get();
            }if(potentialStates.contains("ConsumerQueryState")
                    &&( potentialStates.contains("FixedAnswerState")
                        || potentialStates.contains("HelpState")
                        || potentialStates.contains("UserGreetingState"))
                    ){
                return  sentenceScoreMap.get(maxSentence).stream().filter(x->x._1.getLibraryName().equalsIgnoreCase("ConsumerQueryState")).findFirst().get();
            }else{
                // other multiple cases/
                //TODO: should not using random to make test's results consistent
                System.out.println("******* Multiple other states found");
                return sentenceScoreMap.get(maxSentence).get(0);///.get(new Random().nextInt(sentenceScoreMap.get(maxSentence).size()));
            }
        }
    }
}
