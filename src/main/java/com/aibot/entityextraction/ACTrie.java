package com.aibot.entityextraction;

import com.aibot.qa.Parser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import scala.Tuple2;
import scala.Tuple3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;



public class ACTrie<T> implements Serializable{

    private class State implements  Serializable{

        private final Map<T, State> goToTransitions = new HashMap<>();
        private final List<Integer> outputs = new ArrayList<>();   // instead of HashSet for memory saving
        private State failTransition;
        private State defaultTransition;
        public Map<T, State> getGoToTransitions() {
            return Collections.unmodifiableMap(goToTransitions);
        }

        public State goTo(T symbol) {
            return goToTransitions.containsKey(symbol) ? goToTransitions.get(symbol) : defaultTransition;
        }

        public boolean canGoTo(T symbol) {
            return defaultTransition != null || goToTransitions.containsKey(symbol);
        }

        public State addGoTo(T symbol, State target) {
            goToTransitions.put(symbol, target);
            return target;
        }

        public void addOutput(Integer patternIndex) {
            outputs.add(patternIndex);
        }

        public void addOutputs(List<Integer> patternIndices) {
            outputs.addAll(patternIndices);
        }

        public List<Integer> getOutputs() {
            return Collections.unmodifiableList(outputs);
        }

        public State fail() {
            return failTransition;
        }

        public void setFail(State target) {
            failTransition = target;
        }

        public void setDefault(State state) {
            defaultTransition = state;
        }

        public boolean endsWord(){
            return getOutputs().size() > 1;
        }
    }

    private State root;
    private String name;

    public List<T[]> getPatterns() {
        return patterns;
    }

    private List<T[]> patterns;

    private void writeObject (ObjectOutputStream stream) throws IOException {
        stream.writeInt(patterns.size());
        for (T[] t : patterns) {
            stream.writeObject(t);
        }
    }

    private void readObject (ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int numElems = stream.readInt();
        patterns = new ArrayList<>();
        root = new State();
        for ( int i = 0; i < numElems; i++ ) {
            T[] t = (T[]) stream.readObject();
            patterns.add(t);
        }
        constructACTrie();
    }

    public ACTrie(List<T[]> patterns, String name) {
        /*
         * Builds finite state machine and assigns some outputs.
         */
        root = new State();
        Set<List<T>> uniquePatternSet = patterns.stream().map(t -> Arrays.asList(t)).collect(Collectors.toSet());

        List<T[]> uniquePatterns = uniquePatternSet.stream().map( x-> {
            T[] ts = (T[]) Array.newInstance(patterns.get(0)[0].getClass(), x.size());
            x.toArray(ts);
            return ts;
        }).collect(Collectors.toList());

        this.name = name;
        this.patterns = Collections.unmodifiableList(uniquePatterns);
        constructACTrie();
    }

    private void constructACTrie() {
        for (int i = 0, s = patterns.size(); i < s; ++i) {
            State current = root;
            for (T symbol : patterns.get(i))
                current = current.canGoTo(symbol) ? current.goTo(symbol) : current.addGoTo(symbol, new State());
            current.addOutput(i);
        }
        root.setDefault(root);
        /*
         * Builds the fail function and assigns the rest of the outputs.
         */
        // initialization: all the depth == 1 nodes get a fail to 0.
        Deque<State> pool = root.goToTransitions.values().stream().collect(Collectors.toCollection(LinkedList::new));
        pool.forEach(s -> s.setFail(root));
        while (!pool.isEmpty()) {
            State state = pool.pop();
            for (Map.Entry<T, State> e : state.getGoToTransitions().entrySet()) {
                T symbol = e.getKey();
                State next = e.getValue();
                pool.addLast(next);
                State fail = state.fail();
                while (!fail.canGoTo(symbol)) {
                    fail = fail.fail();
                }
                fail = fail.goTo(symbol);
                next.setFail(fail);
                next.addOutputs(fail.getOutputs());
            }
        }
    }

    public List<T[]> searchPattern(T[] text) {
        return search(text, true).keys().stream().map(x->patterns.get(x)).collect(Collectors.toList());
    }

    public List<String> searchPatternInString(String[] text) {
        return search((T[])text, true).keys().stream().map(x ->{
            Object[] obj = patterns.get(x);
            String[] arr = new String[obj.length];
            for(int i=0; i<obj.length;i++) {
                String someString = (String)obj[i];
                arr[i] = someString;
            }
            return String.join(" ", arr);
        }).collect(Collectors.toList());
    }

    // pattern index is the index of the last matched element in a pattern
    public SetMultimap<Integer, Integer> searchPatternIndexToPosEndIndex(T[] text) { return search(text, true);}

    public SetMultimap<Integer, Integer> searchPosEndIndexToPatternIndex(T[] text) {
        return search(text, false);
    }

    public SetMultimap<Integer, T[]> searchPosIndexToPattern(T[] text) {
        SetMultimap<Integer, T[]> result = HashMultimap.create();
        search(text, true).entries().stream().
            forEach(x->result.put(x.getValue() - patterns.get(x.getKey()).length + 1, patterns.get(x.getKey())));
        return result;
    }

    public SetMultimap<T[], Integer> searchPatternToPosStartIndex(T[] text) {
        SetMultimap<T[], Integer> result = HashMultimap.create();
        search(text, true).entries().stream()
            .forEach(x->result.put(patterns.get(x.getKey()), x.getValue() - patterns.get(x.getKey()).length + 1 ));
        return result;
    }

    /**
     *
     * @param text
     * @param reversed
     * @return key: begin index, value:  end index
     */
    public SetMultimap<Integer, Integer> search(T[] text, boolean reversed) {
        SetMultimap<Integer, Integer> result = HashMultimap.create();
        State state = root;
        for (int i = 0, s = text.length; i < s; ++i) {
            T symbol = text[i];
            while (!state.canGoTo(symbol)){
                state = state.fail();
                //Set<Integer> output =  new HashSet(state.getOutputs());
            }
            state = state.goTo(symbol);
            Set<Integer> output =  new HashSet(state.getOutputs());
            if (!reversed)  result.putAll(i,output);
            else for (Integer patternIndex : output)
                result.put(patternIndex, i);
        }
        return result;
    }

    /**
     *  Suppose string is correctly spelled
     //Once the stem is found, perform a breadth first search to generate suggestions:
     //    Create a queue (LinkedList) and add the node that completes the stem to the back
     //       of the list.
     //    Create a list of completions to return (initially empty)
     //    While the queue is not empty and you don't have enough suggestions:
     //       remove the first Node from the queue
     //       If it is a word, add it to the completions list
     //       Add all of its child nodes to the back of the queue
     // Return the suggestions
     * @param text
     * @return: (found match[], <index found, source to match[]>)
     */
    public SetMultimap<T[], Tuple2<Integer, T[]>> searchPartial(T[] text) {
        SetMultimap<T[], Tuple2<Integer, T[]>> suggestions = HashMultimap.create();
        try{
            SetMultimap<Integer, Integer> exactCandidates = HashMultimap.create();
            SetMultimap<Integer, Tuple2<Integer, List<T>>> partialCandidates = HashMultimap.create();
            State state = root;
            List<Tuple3<State, Integer, List<T>>> nodeQueue = new LinkedList<>();
            List<T> sequence =  new ArrayList<>();
            for (int i = 0, s = text.length; i < s; ++i) {
                T symbol = text[i];
                while (!state.canGoTo(symbol)){
                    //end continuous match and start transition
                    //maintain the source string
                    if(sequence.size()>0) {
                        if(Parser.isNoun((String)(sequence.get(0))))
                            nodeQueue.add(new Tuple3<>(state, i-1, sequence));
                    }
                    sequence = new ArrayList<>();
                    state = state.fail();
                }
                state = state.goTo(symbol);
                if(root.goToTransitions.containsKey(symbol)|| sequence.size() > 0)
                    sequence.add(symbol);
                Set<Integer> output =  new HashSet(state.getOutputs());
                for (Integer patternIndex : output)
                    exactCandidates.put(patternIndex, i);
                if(i==s-1 & sequence.size()>0){
                    if(Parser.isNoun((String)(sequence.get(0))))
                        nodeQueue.add(new Tuple3<>(state, i, sequence));
                }
            }

            while (!nodeQueue.isEmpty()) {
                Tuple3<State, Integer, List<T>> tn = nodeQueue.remove(0);
                state = tn._1();
                Set<Integer> stateOutput = new HashSet(state.getOutputs());
                int index = tn._2();
                List<T> tn_sequence = tn._3();

                if(stateOutput.isEmpty()){//not exact match
                    Map<T, State> childStates = state.getGoToTransitions();
                    childStates.entrySet().stream().forEach(r -> {
                        State childState = r.getValue();

                        if (!nodeQueue.contains(childState))
                            nodeQueue.add(new Tuple3<>(childState, index, tn_sequence));
                    });

                }else {
                    for (Integer patternIndex : stateOutput) {
                        if(!exactCandidates.containsKey(patternIndex)
                                & isIntersec(patterns.get(patternIndex),tn_sequence)
                                //&!isExisted(partialCandidates,tn_sequence)
                                ) {
                            partialCandidates.put(patternIndex, new Tuple2<>(index, tn_sequence));
                        }
                    }
                }
            }

            exactCandidates.entries().stream()
                    .forEach(x->suggestions.put(patterns.get(x.getKey()),
                            new Tuple2<>(x.getValue() - patterns.get(x.getKey()).length + 1, patterns.get(x.getKey()))));

            partialCandidates.entries().stream()
                    .forEach(x->{
                        int starIndex = x.getValue()._2().size()==1 ? x.getValue()._1(): (x.getValue()._1() - x.getValue()._2().size() + 1);
                        //sequence
                        T[] ts = (T[]) Array.newInstance(x.getValue()._2().get(0).getClass(), x.getValue()._2().size()); //set class for []
                        suggestions.put(patterns.get(x.getKey()),
                                new Tuple2<>(starIndex, x.getValue()._2().toArray(ts)));
                    });
            //ranking match
        }catch (Exception e){
            e.printStackTrace();
            return suggestions;

        }
        return suggestions;
    }

    private boolean isIntersec(T[] symbol, List<T> sequence){
        if(Arrays.asList(symbol).containsAll(sequence)) return true;
        return false;
    }

    private boolean isExisted(SetMultimap<Integer, Tuple2<Integer, List<T>>> partialCandidates, List<T> sequence){
        return partialCandidates.entries().stream().anyMatch(x-> (!x.getValue()._2().equals(sequence) & isIntersec(patterns.get(x.getKey()),sequence)));
    }

    public T[] getPatternFromIndex (int index)
    {
        return patterns.get(index);
    }
}
