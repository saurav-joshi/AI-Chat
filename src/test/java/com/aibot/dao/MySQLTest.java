package com.aibot.dao;


import com.aibot.entity.*;
//import com.crayon.recommender.RecommenderTest;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Ignore;

import java.util.Map;

@Ignore
public class MySQLTest {
    public void connectionTest(){
        try{
            MySQL.getConnection();
            Map<String, Long> e = new HashedMap();
            e.put("chinese",1l);
            e.put("thai",5l);
            System.out.println(e.toString().replace("{","").replace("}",""));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /*@Test
    public void qaTest(){
        Conversation cs = new Conversation();
        cs.setId(1);
        List<QA> qaList = new ArrayList<>();

        QA qa = new QA();
        qa.setTime(new Date());

        List<EntityExtractionUtil.EntityExtractionResult> entities = new ArrayList<>();
        EntityExtractionUtil.EntityExtractionResult entity =
                new EntityExtractionUtil.EntityExtractionResult("#location","orchard");
        entities.add(entity);

        entity =
                new EntityExtractionUtil.EntityExtractionResult("$offer","offer");

        entities.add(entity);
        entity =
                new EntityExtractionUtil.EntityExtractionResult("$beverage","coffee");
        entities.add(entity);
        qa.setEntities(entities);
        qa.setAnswer(new Answer("answer", new ArrayList<RecommenderResultsRestaurant>(),"explain"));

        qa.setMatchedPatternIdInLibrary(1000);

        List<State.StateType> stateTypes = new ArrayList<>();
        stateTypes.add(State.StateType.StartState);
        stateTypes.add(State.StateType.ConsumerQueryState);
        stateTypes.add(State.StateType.EndState);

        qa.setStatePaths(stateTypes);

        qaList.add(qa);
        cs.setQaList(qaList);

        QADao.getInstance().insertListQA(cs);

        QADao.getInstance().getQAByConversation(cs.getId());
    }*/
/*
    @Test
    public void qaSelectTest(){
        Conversation cs = new Conversation();
        cs.setId(27959252);
        List<QA> qaList = QADao.getInstance().getQAByConversation(cs.getId());
        System.out.println("Total qa: " + qaList.size());

    }*/

/*
    @Test
    public void qaSelectCurrentConTest(){
        //latest conversation
        Conversation con = ConversationDao.getInstance().selectConversation("1111");
        List<QA> qA = con.getQaList();
        System.out.println("Total qa session: " + qA.size());
        for (QA qa : qA) {
            System.out.println(qa.getQuestion() + " => " +qa.getAnswer().getMessage());
        }
        con = ConversationDao.getInstance().selectCurrentConversation("1111");
        List<QA> currentQA = con.getQaList();
        System.out.println("Total qa current session: " + currentQA.size());
        for (QA qa : currentQA) {
            System.out.println(qa.getQuestion() + " => " +qa.getAnswer().getMessage());
        }
    }*/

  


}