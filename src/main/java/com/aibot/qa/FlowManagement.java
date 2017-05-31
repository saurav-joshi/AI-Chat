package com.aibot.qa;

import com.aibot.entity.Answer;
import com.aibot.entity.Clarification;
import com.aibot.entity.Conversation;
import com.aibot.entity.Suggestion;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.spelling.SpellingCorrection;
import com.aibot.state.BookingState;
import com.aibot.state.State;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

public class FlowManagement {
    private static FlowManagement flowManagement;
    public static Map<String, String> contextToEntityMapping = new HashMap<>();
    private static EntityExtractionUtil entityExtractionUtil;
    private final static Integer MAXSTATEPATHS = 10;
    private final static Map<State.StateType, State> stateMap = new HashMap<>();

    public static  Map<State.StateType, State>  getStateMap(){
        return stateMap;
    }
    private static SpellingCorrection  sc;

    public static FlowManagement init() {
        if (flowManagement == null) {

            SynonymMappingAndLemmatization.init(GlobalConstants.synonymMappingFilePath);
            entityExtractionUtil = new EntityExtractionUtil(SynonymMappingAndLemmatization.synMapping.keySet()).caseInsensitive().lemmatizePatterns().loadEntityExtractions();
            flowManagement = new FlowManagement();
            flowManagement.entityExtractionUtil.getEntityExtractionList().forEach(e -> e.setEntityName("$" + e.getName().toLowerCase()));
            LibraryUtil.init();
            flowManagement.addContextToEntityExtraction();
            createStates();
            sc = new SpellingCorrection();
            System.out.println(" The Service Is Ready Now ....");
            return flowManagement;
        } else return flowManagement;
    }

    private static void createStates() {
        for (State.StateType type : State.StateType.values()) {
            try {
                Class<?> clazz = Class.forName("com.oracle.iaasimov.state." + type.name());
                State state = (State) clazz.getConstructor().newInstance();
                state.setStateType();
                state.allowedInputStateTypes();
                stateMap.put(type, state);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addContextToEntityExtraction() {
        for (Map.Entry<String, Set<String>> entry : LibraryUtil.flatContextsMap.entrySet()) {
            List<String> patterns = entry.getValue().stream().flatMap(r ->
                createPatternsForEntityExtraction(LibraryUtil.flatContextsMap, r).stream()
            ).collect(Collectors.toList());
            entityExtractionUtil.getEntityExtractionList().add(entityExtractionUtil.buildEntityExtraction(entry.getKey(), patterns));
        }
    }

    private List<String> createPatternsForEntityExtraction(Map<String, Set<String>> flatContextMap, String rule) {
        // support multi entities in one rule
        if (rule.indexOf("$") != -1) {
            String entityName = Arrays.stream(rule.split("\\s+")).filter(x -> x.startsWith("$")).findFirst().get().toLowerCase();
            return entityExtractionUtil.getEntityExtractionByName(entityName).getPatterns().stream().map(p -> {
                List<String> pattern = new ArrayList<>();
                Arrays.stream(rule.split("\\s+")).forEach(w -> {
                    if (!w.startsWith("$")) pattern.add(w);
                    else pattern.addAll(Lists.newArrayList(p));
                });
                contextToEntityMapping.put(String.join(" ", pattern), String.join(" ", p));
                return String.join(" ", pattern);
            }).collect(Collectors.toList());
        } else  // rule include "#"
        {
            String contextName = Arrays.stream(rule.split("\\s+")).filter(x -> x.startsWith("#")).findFirst().get().toLowerCase();
            return flatContextMap.get(contextName).stream().flatMap(r ->
                createPatternsForEntityExtraction(flatContextMap, r).stream()
            ).collect(Collectors.toList()).stream().map(p -> {
                List<String> pattern = new ArrayList<>();
                Arrays.stream(rule.split("\\s+")).forEach(w -> {
                    if (!w.startsWith("#")) pattern.add(w);
                    else pattern.addAll(Lists.newArrayList(p));
                });
                return String.join(" ", pattern);
            }).collect(Collectors.toList());
        }
    }

    public Answer process(Conversation con) {
        //spellingCorrection(con).
        con = synonymMapping(con);
        con = entityExtractionPartial(con);
        con = analyze(con);
        return con.getLatestQA().getAnswer();
    }

    public static Conversation analyze(Conversation con){
        con = extractPatternWords(con);
        return fsm(con);
    }

    public static Conversation analyzeWithPartialMatchStep(Conversation con){
        //con = entityExtractionPartial(con);
        if(con.getLatestEntities().isEmpty()){
            con = spellingCorrection(con);
            con = entityExtraction(con);
        }
        con.setPreProcess(true);
        return analyze(con);
    }

    public static Conversation spellingCorrection(Conversation conversation) {
        Map<String,List<String>> candidateMap = sc.spellingCandidates(conversation.getLatestQueston());
        String text = conversation.getLatestQueston();

        //Replace and remove from candidate list
        Iterator<Map.Entry<String,List<String>>> it = candidateMap.entrySet().iterator();
        List<String> usedgram = new ArrayList<>();
        while (it.hasNext())
        {
            Map.Entry<String,List<String>> en = it.next();
            if(!SpellingCorrection.isUsed(usedgram,en.getKey())){
                text = text.replace(en.getKey(), en.getValue().iterator().next());//pop up first value, highest score
                usedgram.add(en.getKey());
                it.remove();
            }
        }

        conversation.getLatestQA().setQuestion(text);

        if(!candidateMap.isEmpty()){
            System.out.println("Candidate of spelling correction: " + candidateMap.toString());

            Set<Clarification> clarifications = candidateMap.entrySet().stream().map( en ->
                    new Clarification(Clarification.Type.SpellingCorrection, en.getKey(), new HashSet<>(en.getValue()), false)
            ).collect(Collectors.toSet());
            conversation.getLatestQA().setClarification(clarifications);
            conversation.getLatestQA().getAnswer().setSuggestion(
                    new Suggestion(LibraryUtil.getRandomPatternByQuestionClass("Clarify.Spelling").getSystemMessage()
                            , conversation.getLatestQA().getTopTargets(3), "Clarify.Spelling"));
        }
        System.out.println("Question after spelling correction :" + text);
        return conversation;
    }

    public static Conversation synonymMapping(Conversation conversation){
        String text = SynonymMappingAndLemmatization.run(conversation.getLatestQueston());
        if(BookingState.isBookingLoop(conversation)){
            text = text
                    .replace(" @","at")
                    .replace(", time :","at")
                    .replace("utc","");
        }
        conversation.getLatestQA().setQuestion(text);
        System.out.println("Question after SynonymMapping :" + text);
        return conversation;
    }

    public static Conversation entityExtraction(Conversation con){
        String question = con.getLatestQA().getQuestion();
        List<String> patternWords = Lists.newLinkedList(Arrays.asList(question.split("\\s+")));
        List<EntityExtractionUtil.EntityExtractionResult> entityExtractionResults = entityExtractionUtil.extractEntity(patternWords.stream().toArray(String[]::new));

        List<EntityExtractionUtil.EntityExtractionResult> selectEEResults = EntityExtractionUtil.mergeIntervals(entityExtractionResults);
        con.getLatestQA().setEntities(selectEEResults);
        System.out.println("Entity Extraction: "+ entityExtractionResults);
        return con;
        //return this;
    }

    public static Conversation entityExtractionPartial(Conversation con){
        String question = con.getLatestQA().getQuestion();
        List<String> patternWords = Lists.newLinkedList(Arrays.asList(question.split("\\s+")));
        List<EntityExtractionUtil.EntityExtractionResult> entityExtractionResults = entityExtractionUtil.extractEntityPartial(patternWords.stream().toArray(String[]::new));

        List<EntityExtractionUtil.EntityExtractionResult> selectEEResults = EntityExtractionUtil.choosePartialMatch(entityExtractionResults);
        //filter
        entityExtractionResults.removeIf(e -> selectEEResults.contains(e)
                || e.getMatchScore()>=1.0
                || !selectEEResults.stream().anyMatch(x -> x.getEntityName().equalsIgnoreCase(e.getEntityName()))
                || e.getEntityName().equalsIgnoreCase("#restaurantentity")
                || (e.getEntityName().equalsIgnoreCase("#location") & entityExtractionResults.stream().anyMatch(x -> e.getEntityName().equalsIgnoreCase("$location"))));
        con.getLatestQA().setEntities(selectEEResults);
        if(!entityExtractionResults.isEmpty()){
            Map<String,Set<String>> candidateMap = EntityExtractionUtil.getCandidateMap(entityExtractionResults);
            if(!candidateMap.isEmpty()){
                Set<Clarification> clarifications = candidateMap.entrySet().stream().map( en ->
                        new Clarification(Clarification.Type.FuzzyMatch, en.getKey(), en.getValue(), false)
                ).collect(Collectors.toSet());
                con.getLatestQA().setClarification(clarifications);
                con.getLatestQA().getAnswer().setSuggestion(
                        new Suggestion(LibraryUtil.getRandomPatternByQuestionClass("Clarify.PartialMatch").getSystemMessage()
                                ,con.getLatestQA().getTopTargets(3), "Clarify.PartialMatch"));
            }
        }

        System.out.println("Partial Entity Extraction: "+ entityExtractionResults);
        return con;
        //return this;
    }

    public static Conversation extractPatternWords(Conversation con) {
        List<String> patternWords = Lists.newLinkedList(Arrays.asList(con.getLatestQA().getQuestion().split("\\s+")));
        con.getLatestEntities().stream().sorted((a, b) -> b.getStartIndex() - a.getStartIndex()).forEach(r -> {
            //those are not restaurant's entities, we keep, but not replace
            if(! (r.getEntityName().contains("regular")
                    || r.getEntityName().contains("accompany")
                    || r.getEntityName().contains("occasion")) && r.getEndIndex()< patternWords.size()) {
                List<String> matchedPattern = patternWords.subList(r.getStartIndex(), r.getEndIndex()+ 1);
                matchedPattern.clear();
                matchedPattern.add(r.getEntityName());
            }
        });
        con.getLatestQA().setCleanedQuestionPatternWords(patternWords);
        System.out.println("Question with Pattern Replacement: "+ patternWords);
        System.out.println("Entity After Extract Pattern"+ con.getLatestEntities());
        return con;
    }

    public static Conversation fsm(Conversation con) {
        State state = stateMap.get(State.StateType.StartState);
        while (!state.getStateType().equals(State.StateType.EndState)) {
                List<State.StateType> statePaths = con.getLatestQA().getStatePaths();
                State.StateType currentStateType = state.stateType;
                State.StateType nextStateType;
                if(state.guardCheck(con)) {
                    nextStateType = state.process(con);
                    statePaths.add(state.getStateType());
                }
                else nextStateType = statePaths.get(statePaths.size()-1);

                state = stateMap.get(nextStateType);

                if (statePaths.size() > MAXSTATEPATHS){
                    throw new IllegalStateException("Exceed The MaxStatePaths: " + statePaths.size());
                }
                if (nextStateType.equals(State.StateType.EndState) && con.getLatestQA().getAnswer() == null){
                    throw new IllegalStateException("Input of End State Do Not Have Answer");
                }
        }
        con.getLatestQA().getAnswer().setExtractedEntity(con.getLatestQA().getEntities().toString());
        return con;
    }
}
