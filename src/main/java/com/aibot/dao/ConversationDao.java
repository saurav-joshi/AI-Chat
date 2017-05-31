package com.aibot.dao;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ConversationDao {
    private static ConversationDao conversationDao = null;
    public static QADao qaDao = null;
    private final String tableName = "conversation";

    public static ConversationDao getInstance() {
        if (conversationDao == null) {
            conversationDao = new ConversationDao();
            qaDao = new QADao();
        }
        return conversationDao;
    }

    // NOT USED FOR NOW
    public void updateConversation(Conversation conversation){
        try{
            Connection connection = MySQL.getConnection();
            int conversationId = conversation.getId();
            String userId = conversation.getUserId();
            List<QA> qas = conversation.getQaList();
            List<String> qaIds = new ArrayList<>();
            qas.forEach(qa -> {
                qaIds.add(String.valueOf(qa.getId()));
            });
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET qa_ids=" + qaIds +
                    " WHERE user_id='" + userId + "' AND conversation_id = " + conversationId);
            System.out.printf("UPDATE " + tableName + " SET qa_ids=" + qaIds +
                " WHERE user_id='" + userId + "' AND conversation_id = " + conversationId);
            ps.execute();
            connection.commit();
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Error updating conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insertConversation(Conversation conversation){
        try{
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (user_id, conversation_id) " +
                    "VALUES( " + "?, ?)");
            System.out.println(ps.toString());
            int conversationId = conversation.getId();
            String userId = conversation.getUserId();

            ps.setString(1, userId);
            ps.setLong(2, conversationId);

            ps.execute();
            ps.close();
            connection.close();

        }catch (Exception e){
            System.out.println("Error inserting conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Conversation selectConversation(String userId){
        try {
            String query = "SELECT * FROM " + tableName + " WHERE user_id = '" + userId + "'";
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int conversationId = (int) rs.getLong("conversation_id");
                Conversation conversation = new Conversation();
                conversation.setQaList(QADao.getInstance().getQAByConversation(conversationId));
                conversation.setId(conversationId);
                conversation.setUserId(userId);
                conversation.setUserName(UserProfileDao.getInstance().getUserName(userId));
                return conversation;
            }
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Error getting conversation: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Conversation selectCurrentConversation(String userId){
        try {
            String query = "SELECT * FROM " + tableName + " WHERE user_id = '" + userId + "'";
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int conversationId = (int) rs.getLong("conversation_id");
                Conversation conversation = new Conversation();
                conversation.setQaList(QADao.getInstance().getQACurrentSession(conversationId));
                conversation.setId(conversationId);
                conversation.setUserId(userId);
                return conversation;
            }
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Error getting conversation: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
