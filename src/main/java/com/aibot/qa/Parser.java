package com.aibot.qa;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.util.CoreMap;
import org.ejml.simple.SimpleMatrix;
import scala.Tuple2;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {

    private static StanfordCoreNLP pipeline;
    private static StanfordCoreNLP lemmaPipeline;
    private static StanfordCoreNLP tokenPipeline;
    private static StanfordCoreNLP sentencePipeline;
    private static StanfordCoreNLP posPipeline;
    private static AnnotationPipeline sutimePipeline;
    private static HashSet<String> nounTagsSet;
    private static Morphology morpho;
    private static MaxentTagger tagger;

    static {
        Properties props = new Properties();
        props.put("annotators", "tokenize,ssplit,pos,parse");
        props.put("-depparse.model", "edu/stanford/nlp/models/parser/nndep/english_SD.gz");
        props.put("parse.maxlen", "100");

        pipeline = new StanfordCoreNLP(props);

        Properties sentenceProps = new Properties();
        sentenceProps.put("annotators", "tokenize,ssplit");
        sentencePipeline = new StanfordCoreNLP(sentenceProps);

        Properties lemmaProps = new Properties();
        lemmaProps.put("annotators", "tokenize, ssplit, pos, lemma");
        lemmaPipeline = new StanfordCoreNLP(lemmaProps);

        Properties tokenProps = new Properties();
        tokenProps.put("annotators", "tokenize, ssplit");
        tokenPipeline = new StanfordCoreNLP(tokenProps);

        Properties posProps = new Properties();
        posProps.put("annotators", "tokenize, ssplit, pos");
        posPipeline = new StanfordCoreNLP(posProps);

        nounTagsSet = new HashSet<>();
        nounTagsSet.add("NN");
        nounTagsSet.add("NNP");
        nounTagsSet.add("NNS");
        morpho = new Morphology();
        tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");

    }

    public static String stem(String word) {
        if (word.trim().equals(""))
            return "";
        List<String> result = new ArrayList<>();
        for(String w: word.split("\\s+")){
            if(!(w.endsWith("i") || w.endsWith("ed") || w.endsWith("ing") || w.endsWith("is")
                    || w.endsWith("a") || w.endsWith("'s") || w.endsWith("en")
                    || w.equals("feet") || w.equals("rose") ||w.equals("no") || w.contains("_") || w.equals("made")
                    || w.equals("am") || w.equals("are") || w.equals("me") || w.equals("us")
                    || w.length()==1)){
                w = morpho.stem(w);
            }
            result.add(w);
        }
        return String.join(" ", result);
    }

    public static String extractDate (String text) {
        AnnotationPipeline pipeline = initDatePipeline();
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        if (!timexAnnsAll.isEmpty()) {
            return timexAnnsAll
                    .get(0)
                    .get(TimeExpression.Annotation.class)
                    .getTemporal()
                    .toString();
        }

        return "";
    }

    private static AnnotationPipeline initDatePipeline() {
        if (sutimePipeline == null) {
            Properties props = new Properties();
            sutimePipeline = new AnnotationPipeline();
            sutimePipeline.addAnnotator(new TokenizerAnnotator(false));
            sutimePipeline.addAnnotator(new WordsToSentencesAnnotator(false));
            sutimePipeline.addAnnotator(new POSTaggerAnnotator(false));
            props.put("sutime.rules",
                    "edu/stanford/nlp/models/sutime/defs.sutime.txt,edu/stanford/nlp/models/sutime/english.sutime.txt"
                            + ",edu/stanford/nlp/models/sutime/english.holidays.sutime.txt");
            sutimePipeline.addAnnotator(new TimeAnnotator("sutime", props));
        }
        return sutimePipeline;
    }

    public static Stream<String> getPOSfromText(String text) {
        Annotation annotation = new Annotation(text);
        posPipeline.annotate(annotation);

        return annotation
            .get(SentencesAnnotation.class)
            .stream()
            .flatMap( sentence ->
                sentence
                .get(TokensAnnotation.class)
                .stream()
                .map( corelabel ->
                    corelabel.get(PartOfSpeechAnnotation.class)
                )
            );
    }

    public static boolean isNoun(String word) {
        if(word.equalsIgnoreCase("restaurant") || word.equalsIgnoreCase("taste") || word.equalsIgnoreCase("ok")){
            return false;
        }
        return tagger.tagTokenizedString(word).contains("NN");
    }

    public static List<String> getSentencesfromText(String text) {
        Annotation annotation = new Annotation(text);
        sentencePipeline.annotate(annotation);
        return annotation.get(SentencesAnnotation.class).stream().map(r -> r.toString()).collect(Collectors.toList());
    }

    public static CoreMap createCoreMap(String sentence) {
        Annotation document = new Annotation(sentence);
        try {
            pipeline.annotate(document);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return document.get(SentencesAnnotation.class).get(0);
    }

    static Stream<String> tokenizeParaIntoSentences(String para) {
        Annotation doc = new Annotation(para);
        sentencePipeline.annotate(doc);

        return doc
            .get(SentencesAnnotation.class)
            .stream()
            .map( coremap ->
                coremap.get(TextAnnotation.class)
            );
    }

    public static String lemmatize(String word) {
        if (word.trim().equals(""))
          return "";
        Annotation doc = new Annotation(word);
        lemmaPipeline.annotate(doc);
        List<String> result =
            doc
                .get(SentencesAnnotation.class)
                .stream()
                .flatMap( coremap ->
                  coremap.get(TokensAnnotation.class)
                  .stream()
                  .map( token ->
                    token.get(LemmaAnnotation.class)
                  )
                ).map(x->x.toString()).map(String::toLowerCase)
                .collect(Collectors.toList());

        if (result.size() == 0)
          return "";
        return String.join(" ", result);
    }

    public static List<String> lemmatizeAndLowercaseText(String text) {
        return new LinkedList<>(Arrays.asList(String.join(" ", Parser.wordTokenize(text).stream().map(x->Parser.stem(x.toLowerCase())).collect(Collectors.toList())).replace("$ ", "$").split("\\s+")));
    }

    static String getTextFromCoreMap(CoreMap coremap) {
        return coremap.get(TextAnnotation.class);
    }

    static Stream<String> getWordsFromCoreMap(CoreMap coremap) {
        return coremap
            .get(TokensAnnotation.class)
            .stream()
                .map( corelabel ->
                  corelabel
                  .get(TextAnnotation.class)
                );
    }

    static List<Tuple2<String, String>> getWordsAndPOSFromCoreMap(String text) {
        List<Tuple2<String, String>> result = new ArrayList<>();
        Annotation doc = new Annotation(text);
        lemmaPipeline.annotate(doc);

        // Iterate over sentence words, and add them with their POS tag
        // to the result map
        doc
        .get(SentencesAnnotation.class)
        .stream()
        .forEach(coreMap ->
            coreMap
            .get(TokensAnnotation.class)
            .stream()
            .forEach(corelabel ->
                result.add(new Tuple2<>(corelabel.get(LemmaAnnotation.class), corelabel.get(PartOfSpeechAnnotation.class)))
            )
        );

        return result;
    }

    static List<CoreMap> getSentenceCoreMaps(String para) {
        try {
            if (para.length() > 1000)
              return new ArrayList<>();
            Annotation doc = new Annotation(para);
            pipeline.annotate(doc);
            return doc.get(SentencesAnnotation.class);
        } catch (Exception e) {
            System.out.println(e.toString());
            return new ArrayList<>();
        } catch (Throwable e) {
            System.out.println(e.toString());
            return new ArrayList<>();
        }
    }

    //old version from Xiangtao
    public static List<String> tokenize(String text) {
        Annotation annotation = new Annotation(text);
        tokenPipeline.annotate(annotation);

        return annotation
                .get(SentencesAnnotation.class)
                .stream()
                .flatMap( sentence ->
                  sentence
                  .get(TokensAnnotation.class)
                  .stream()
                  .map(corelabel ->
                    corelabel
                    .get(TextAnnotation.class)
                  )
                ).map(x->x.toString()).collect(Collectors.toList());
    }

    public static List<String> wordTokenize(String s) {
        //reference: http://stackoverflow.com/questions/14058399/stanford-corenlp-split-words-ignoring-apostrophe
        PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<CoreLabel>(
                new StringReader(s), new CoreLabelTokenFactory(), "normalizeParentheses=false");
        List<String> sentence = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (CoreLabel label; ptbt.hasNext();) {
            label = ptbt.next();
            String word = label.word();
            if (word.startsWith("'")) {
                sb.append(word);
            } else {
                if (sb.length() > 0)
                    sentence.add(sb.toString());
                sb = new StringBuilder();
                sb.append(word);
            }
        }
        if (sb.length() > 0)
            sentence.add(sb.toString());
        return sentence;
    }

    private static Tree buildTree(CoreMap coreMap) {
        return coreMap.get(TreeAnnotation.class);
    }

    public static TreeGraphNode createTreeGraphReturnRoot(Tree tree) {
        return new TreeGraphNode(tree.label(), tree.getChildrenAsList());
    }

    private static Table getDependencyRelations(String text) {
        Table<String, String, String> table = HashBasedTable.create();
        getSentenceCoreMaps(text).forEach(sentenceCoreMap -> {
            SemanticGraph sg = sentenceCoreMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
            sg.edgeListSorted().forEach(x -> {
                //                System.out.println("-"+sge.getRelation().getShortName() +"-"+sge.getSource() +"-"+ sge.getTarget());
//                System.out.println("-"+sge.getRelation().getShortName() +"-"+sge.getSource().word() +"-"+ sge.getTarget().word());
                if(x.getRelation().getShortName() != null && x.getSource().word() != null && x.getTarget().word() != null )
                table.put(x.getSource().word(),  x.getTarget().word() , x.getRelation().getShortName());
            });
        });
        return table;
    }

    static List<String> windowScanDependencyRelations(String text){
        List<String> relations = new ArrayList<>();
        Table table = getDependencyRelations(text);
        Set<Table.Cell> cells = table.cellSet();
        cells.stream().forEach(x -> {
           relations.add( x.getRowKey().toString() + "-" + x.getValue().toString() );
           relations.add( x.getValue().toString() +x.getColumnKey().toString());
           relations.add( x.getRowKey().toString() + "-" + x.getValue().toString() + "-" + x.getColumnKey() );
        });
        return relations;
    }

    static List<String> extractNP(CoreMap coreMap) {
        Tree tree = buildTree(coreMap);
        return extractNP(tree)
            .stream()
            .filter( x ->
              !x.endsWith("'s") && !x.endsWith("''")
            ).collect(Collectors.toList());
    }

    private static int extractSentimentClass(Tree tree) {
        return RNNCoreAnnotations.getPredictedClass(tree) - 2;
    }

    public static double extractSentimentScore(Tree tree) {
        SimpleMatrix vector = RNNCoreAnnotations.getPredictions(tree);
        int level = extractSentimentClass(tree);
        if (level == 0)
          return 0.0;

        double score = 0.0;
        // TODO: Check if 'i' in if below should be 'vector.get(i)'
        for (int i = 0; i < vector.getNumElements() - 1; i++) {
            if (i > 2)
                score = score + vector.get(i);
            else if (i < 2)
              score = score - vector.get(i);
        }

        return score;
    }

    private static Tree searchTreeLeafNodes(Tree tree, String word) {
        return tree
                .preOrderNodeList()
                .stream()
                .filter( node ->
                  node.children().length == 0
                  && node.yieldWords().size() == 1
                  && node.yieldWords().get(0).word().equals(word)
                ).findFirst()
                .get();
    }

    private static List<String> processPossibleAbbreviation(String abbr) {
        List<String> result = new ArrayList<>();
        abbr =
            String.join(
                " ",
                Arrays.asList(abbr.split("!"))
                .stream()
                .filter(x -> !x.trim().isEmpty())
                .collect(Collectors.toList())
            );

        if (abbr.contains(".")) {
            boolean allCaps = true;
            for (int i = 0; i < abbr.length(); i++)
                if (Character.isLetter(abbr.charAt(i)) && Character.isLowerCase(abbr.charAt(i))) {
                    allCaps = false;
                    break;
                }
            if (!allCaps)
                result.addAll(Arrays.asList(abbr.split(".")));
            else
                result.add(abbr);
        } else {
            result.add(abbr);
        }

        List<String> validPhrases = new ArrayList<>();
        for (String phrase : result) {
            String[] words = phrase.split("\\s+");
            int i = words.length - 1;
            while (i >= 0 && (words[i].trim().toLowerCase().equals("and") || words[i].trim().length() == 1))
                i--;
            if (i >= 0)
                validPhrases.add(String.join(" ", Arrays.copyOfRange(words, 0, i + 1)));
        }

        return validPhrases;
    }

    private static List<String> getNounPhrasesFromYield(List<LabeledWord> yieldList) {
        List<String> nounPhraseList = new ArrayList<>();
        StringBuffer curPhrase = new StringBuffer();
        Set<String> tagForSubject = new HashSet<>(Arrays.asList("NNP", "NN", "NNPS", "NNS"));
        for ( LabeledWord lbword : yieldList ) {
            if (tagForSubject.contains(lbword.tag().toString())
                || lbword.tag().toString().equals("CC")) {
                curPhrase.append(lbword.word());
                curPhrase.append(' ');
            } else {
                if ( curPhrase.toString().trim().length() > 1 )
                    nounPhraseList.addAll(
                        processPossibleAbbreviation(curPhrase.toString().trim())
                    );
                if ( curPhrase.length() > 0 )
                    curPhrase = new StringBuffer();
            }
        }

        if (curPhrase.length() > 0)
            nounPhraseList.addAll(
                processPossibleAbbreviation(curPhrase.toString().trim())
            );

        return nounPhraseList;
    }

    private static List<String> extractNP(Tree tree) {
        Stream<String> tags =
            tree.labeledYield()
            .stream().parallel()
            .map( f -> f.tag().value() );

        List<String> tagForSubject = Arrays.asList("NNP", "NN", "NNPS", "NNS");
        List<String> resultList = new ArrayList<>();

        // Recurse on tree nodes with tags matching any of tagForSubject
        // Add all the extracted phrases from these nodes to the resultList and return
        if (tree.value().equals("NP") && tags.anyMatch(tagForSubject::contains)) {
            Arrays.stream(tree.children())
            .forEach( f -> resultList.addAll(extractNP(f)) );

            if (!tree.yieldWords().isEmpty() && resultList.isEmpty()) {
                resultList.addAll( getNounPhrasesFromYield(tree.labeledYield()) );
            }
        } else {
            if (tree.children() == null)
              return resultList;

            Arrays.stream(tree.children())
            .forEach( f -> resultList.addAll(extractNP(f)) );
        }

        return resultList;
    }

    static boolean checkNegation(CoreMap coreMapForSentence, String adj) {
        try {
            SemanticGraph sg = coreMapForSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            List<String> words =
                buildTree(coreMapForSentence)
                .yieldWords()
                .stream().parallel()
                .map(Object::toString)
                .collect(Collectors.toList());

            if (words.indexOf(adj) == -1)
                return false;

            IndexedWord node = sg.getNodeByIndex(words.indexOf(adj) + 1);

            return sg
                .getOutEdgesSorted(node)
                .stream().parallel()
                .anyMatch( e ->
                      e.getRelation()
                      .toString()
                      .equals("neg")
                    );
        } catch (Exception e) {
            System.err.println("No SemanticGraph node after adj");
        }

        return false;
    }

    private static String extendOverRelation(SemanticGraph sg, IndexedWord node, String relation, boolean reverse) {
        StringBuilder result =
            reverse ? new StringBuilder(node.word()).reverse() : new StringBuilder();

        sg
        .getOutEdgesSorted(node)
        .stream()
        .forEach(e -> {
            if (e.getRelation().toString().startsWith(relation)) {
                result.append(" ");
                result.append(
                    reverse ? new StringBuilder(e.getTarget().word()).reverse() : e.getTarget().word()
                );
            }
        });

        if (reverse)
            return result.reverse().toString();

        return result.append(" ").append(node.word()).toString();
    }

    static List<String> getAdj(CoreMap coreMapForSentence, String NP) {
        List<String> JJlist = new ArrayList<>();

        try {
            SemanticGraph sg = coreMapForSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            List<String> words =
                buildTree(coreMapForSentence)
                .yieldWords().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

            String lastWordInNP = Arrays.stream(NP.split("\\s+")).reduce((a, b) -> b).get();
            IndexedWord node = sg.getNodeByIndex(words.indexOf(lastWordInNP) + 1);

            sg
            .getIncomingEdgesSorted(node)
            .stream()
            .forEach(e -> {
                if (e.getRelation().toString().equals("nsubj")
                   && e.getSource().tag().startsWith("JJ"))
                    JJlist.add(extendOverRelation(sg, e.getSource(), "advmod", true));

                if (e.getRelation().toString().equals("prep_of")
                   && e.getSource().tag().startsWith("JJ"))
                    JJlist.add(extendOverRelation(sg,e.getSource(),"advmod", true));

                if (e.getRelation().toString().equals("nsubj")
                   && e.getSource().tag().startsWith("VB")) {
                    sg
                    .getOutEdgesSorted(e.getSource())
                    .stream()
                    .forEach( ee -> {
                      if (ee.getRelation().toString().equals("acomp")
                         && ee.getTarget().tag().startsWith("JJ")) {
                        JJlist.add(extendOverRelation(sg, ee.getTarget(), "advmod", true));
                      }
                    });
                } // end if
            }); // end for each

            sg
            .getOutEdgesSorted(node)
            .stream()
            .forEach( e -> {
              if (e.getRelation().toString().equals("amod")
                 && e.getTarget().tag().startsWith("JJ"))
                JJlist.add(extendOverRelation(sg, e.getTarget(), "advmod", true));

              if (e.getRelation().toString().equals("rcmod")
                 && e.getTarget().tag().startsWith("JJ"))
                JJlist.add(extendOverRelation(sg, e.getTarget(), "advmod", true));
            }); // end for each

        } catch (Exception e) {
            //System.err.println("No SemanticGraph node after last word in NP");
        }

        return JJlist;
    }

    static List<String> getVerbs(CoreMap coreMapForSentence, String NP) {
        List<String> VBlist = new ArrayList<>();

        try {
            SemanticGraph sg = coreMapForSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            List<String> words =
                buildTree(coreMapForSentence)
                .yieldWords().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

            String lastWordInNP = Arrays.stream(NP.split("\\s+")).reduce((a, b) -> b).get();
            IndexedWord node = sg.getNodeByIndex(words.indexOf(lastWordInNP) + 1);

            sg
            .getIncomingEdgesSorted(node)
            .stream()
            .forEach(e -> {
                if (e.getRelation().toString().startsWith("nsubj")
                && e.getSource().tag().startsWith("VB"))
                    VBlist.add(extendOverRelation(sg, e.getSource(), "aux", false));

                if (e.getRelation().toString().startsWith("dobj")
                && e.getSource().tag().startsWith("VB")) {
                    VBlist.add(extendOverRelation(sg, e.getSource(), "aux", false));
                } // end if
            }); // end for each

            sg
            .getOutEdgesSorted(node)
            .stream()
            .forEach( e -> {
                if (e.getRelation().toString().equals("nsubj")
                && e.getTarget().tag().startsWith("VB"))
                    VBlist.add(extendOverRelation(sg, e.getTarget(), "aux", false));

                if (e.getRelation().toString().equals("dobj")
                && e.getTarget().tag().startsWith("VB"))
                    VBlist.add(extendOverRelation(sg, e.getTarget(), "aux", false));
            }); // end for each

        } catch (Exception e) {
            //System.err.println("No SemanticGraph node after last word in NP");
        }

        return VBlist;
    }

    public static List<String> bigrams(List<String> tokens, boolean includeUnigram) {
        ArrayList<String> result = new ArrayList<>();
        for ( int i = 0; i < tokens.size(); i++ ) {
            if ( i < tokens.size() - 1 )
                result.add(String.join(" ", tokens.subList(i, i + 2)));
            if(includeUnigram) result.add(tokens.get(i));
        }
        return result;
    }

    public static List<String> ngrams(int n, List<String> tokens) {
        List<String> ngrams = new ArrayList<>();
        try {
            String[] words = tokens.toArray(new String[0]);
            for (int i = 0; i < words.length - n + 1; i++)
                ngrams.add(concat(words, i, i + n));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ngrams;
    }

    public static String concat(String[] words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = start; i < end; i++)
                sb.append((i > start ? " " : "") + words[i]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    static List<String> getConjunctions(CoreMap sentenCoreMap, List<String> npList) {
        return
            npList.stream()
            .flatMap( x ->
                getConjunctions(sentenCoreMap, x).stream()
            ).collect(Collectors.toList());
    }

    static List<String> getConjunctions(CoreMap sentenceCoreMap, String NP)
    {
        if (NP.trim().isEmpty())
            return new ArrayList<>();
        ArrayList<String> andList = new ArrayList<>();
        try {
            SemanticGraph sg =
                sentenceCoreMap.get(CollapsedCCProcessedDependenciesAnnotation.class);
            List<String> words =
                buildTree(sentenceCoreMap)
                .yieldWords().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

            String lastWordInNP = Arrays.stream(NP.split("\\s+")).reduce((a, b) -> b).get();
            IndexedWord node = sg.getNodeByIndex(words.indexOf(lastWordInNP) + 1);

            sg
            .getIncomingEdgesSorted(node)
            .stream()
            .forEach(e -> {
                if (e.getRelation().toString().startsWith("conj")
                        && nounTagsSet.contains(e.getSource().tag())
                        && nounTagsSet.contains(e.getTarget().tag())) {
                    StringBuilder fromPhrase = new StringBuilder();
                    sg.getOutEdgesSorted(e.getSource()).forEach(compoundEdge -> {
                        if (compoundEdge.getRelation().toString().equals("compound")) {
                            fromPhrase.append(compoundEdge.getTarget().word());
                            fromPhrase.append(" ");
                        }
                    });
                    fromPhrase.append(e.getSource().word());
                    if (fromPhrase.toString().trim().length() > 0)
                        processPossibleAbbreviation(fromPhrase.toString().trim())
                        .forEach(phrase -> andList.add(phrase.trim() + "," + NP));
                }
            }); // end for each

            sg
            .getOutEdgesSorted(node)
            .stream()
            .forEach( e -> {
                if (e.getRelation().toString().startsWith("conj")
                        && nounTagsSet.contains(e.getSource().tag())
                        && nounTagsSet.contains(e.getTarget().tag())) {
                    StringBuilder toPhrase = new StringBuilder();
                    sg.getOutEdgesSorted(e.getTarget()).forEach(compoundEdge -> {
                        if (compoundEdge.getRelation().toString().equals("compound")) {
                            toPhrase.append(compoundEdge.getTarget().word());
                            toPhrase.append(" ");
                        }
                    });
                    toPhrase.append(e.getTarget().word());
                    if (toPhrase.toString().trim().length() > 0)
                        processPossibleAbbreviation(toPhrase.toString().trim())
                        .forEach(phrase -> andList.add(NP + "," + phrase.trim()));
                }
            }); // end for each

        } catch (Exception e) {
            //e.printStackTrace();
        }

        return andList;
    }

    public static List<String> describedBySameAdjVerb (CoreMap sentenceCoreMap) {
        ArrayList<String> similarNPs = new ArrayList<>();
        try {
            SemanticGraph sg = sentenceCoreMap.get(CollapsedCCProcessedDependenciesAnnotation.class);

            sg.vertexListSorted()
            .forEach(x -> {
                StringBuilder forThisWord = new StringBuilder();
                if (x.tag().equals("JJ") || x.tag().startsWith("VB")) {
                    sg.getIncomingEdgesSorted(x)
                    .forEach(edge -> {
                        if (nounTagsSet.contains(edge.getSource().tag())) {
                            StringBuilder nounPhrase = new StringBuilder();
                            sg.getOutEdgesSorted(edge.getSource()).forEach(compoundEdge -> {
                                if (compoundEdge.getRelation().toString().equals("compound")) {
                                    nounPhrase.append(compoundEdge.getTarget().word());
                                    nounPhrase.append(" ");
                                }
                            });
                            nounPhrase.append(edge.getSource().word());
                            if (nounPhrase.toString().trim().length() > 0) {
                                processPossibleAbbreviation(nounPhrase.toString().trim())
                                .forEach(phrase -> {
                                    forThisWord.append(phrase.trim());
                                    forThisWord.append(",");
                                });
                            }
                        }
                    });
                    sg.getOutEdgesSorted(x)
                    .forEach(edge -> {
                        if (nounTagsSet.contains(edge.getTarget().tag())) {
                            StringBuilder nounPhrase = new StringBuilder();
                            sg.getOutEdgesSorted(edge.getTarget()).forEach(compoundEdge -> {
                                if (compoundEdge.getRelation().toString().equals("compound")) {
                                    nounPhrase.append(compoundEdge.getTarget().word());
                                    nounPhrase.append(" ");
                                }
                            });
                            nounPhrase.append(edge.getTarget().word());
                            if (nounPhrase.toString().trim().length() > 0) {
                                processPossibleAbbreviation(nounPhrase.toString().trim())
                                .forEach(phrase -> {
                                    forThisWord.append(phrase.trim());
                                    forThisWord.append(",");
                                });
                            }
                        }
                    });
                }
                if (forThisWord.length() > 0 && forThisWord.charAt(forThisWord.length()-1) == ',')
                    forThisWord.deleteCharAt(forThisWord.length()-1);
                String toAdd = forThisWord.toString().trim();
                if ( toAdd.length() > 0 )
                    similarNPs.add(toAdd);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (similarNPs.size() > 1)
            return similarNPs;
        else
            return new ArrayList<>();
    }

}
