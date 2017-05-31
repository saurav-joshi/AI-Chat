package com.aibot.state;

import com.aibot.data.model.TimeSlot;
import com.aibot.data.repo.TimeSlotRepo;
import com.aibot.entity.Conversation;
import com.aibot.entityextraction.DateExtraction;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.GeoCalculator;
import com.aibot.dao.QADao;
import com.aibot.data.model.Booking;
import com.aibot.data.repo.BookingRepo;
import com.aibot.entity.QA;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Recommender;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class BookingState extends State {
    private enum detailType {restaurant_id, pax, date, phone, email, special}
    public static final String bookingActivate = "book";
    public static final String bookingCancel = "cancelbook";
    public static final String bookingConfirm = "confirmbook";
    private static BookingRepo bookingRepo;
    private static TimeSlotRepo timeSlotRepo;

    @Autowired
    BookingRepo aBookingRepo;
    @Autowired
    TimeSlotRepo aTimeSlotRepo;
    @PostConstruct
    public void init() {
        this.bookingRepo = aBookingRepo;
        this.timeSlotRepo = aTimeSlotRepo;
    }
    @Override
    public void setStateType() {
        this.stateType = StateType.BookingState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.UserRefineState);
    }

    @Override
    public boolean guardCheck(Conversation con) {
        int qaId = getQaWithBooking(con);
        if(qaId==-1){
            System.out.println("Not valid booking process, user did not click button!");
            return false;
        }
        return true;
    }

    @Override
    public StateType process(Conversation conversation) {
        int qaId = getQaWithBooking(conversation);
        Booking booking = bookingRepo.findByQaIdAndUserId(qaId,conversation.getUserId());
        String message = "";
        String restaurantId = String.valueOf(conversation.getBookingRestaurantId());
        if(restaurantId.equals("0") && booking!=null){
            restaurantId = String.valueOf(booking.getRestaurantId());
        }
        RecommenderResultsRestaurant restaurant = Recommender.getInstance().getRestaurantInfoById(restaurantId);

        if(conversation.isActivateBooking()){//activate booking
            //String email = BookingDao.getInstance().getEmail(conversation.getUserId());
            String email = "";
            String phone = "";
            Booking lastBooking = bookingRepo.findFirstByUserId(conversation.getUserId());
            if(lastBooking!=null){
                email = lastBooking.getEmail();
                phone = lastBooking.getPhone();
            }
            //String phone = BookingDao.getInstance().getPhone(conversation.getUserId());
            //check user-email and phone available
            message = LibraryUtil.getRandomPatternByQuestionClass("Booking.ActivateQuestion").getSystemMessage();
            if(restaurant!=null) message = message.replace("$RestaurantName",restaurant.getName());
            /*BookingDao.getInstance().insertNewBooking(conversation.getLatestQA().getId(),
                    conversation.getBookingRestaurantId(), conversation.getUserId());*/
            if(booking==null){
                booking = new Booking();
            }
            booking.setUserId(conversation.getUserId());
            booking.setQaId(conversation.getLatestQA().getId());
            booking.setRestaurantId(conversation.getBookingRestaurantId());
            bookingRepo.save(booking);
            if(!phone.isEmpty()){
                booking.setPhone(phone);
                bookingRepo.save(booking);
                //BookingDao.getInstance().updateBookingDetail(qaId,conversation.getUserId(), BookingDao.detailType.phone.name(), phone);
            }

            if(!email.isEmpty()){
                booking.setEmail(email);
                bookingRepo.save(booking);
                //BookingDao.getInstance().updateBookingDetail(qaId,conversation.getUserId(), BookingDao.detailType.email.name(), email);
            }

            conversation.getLatestQA().getAnswer().setMessage(message + " " + LibraryUtil.getRandomPatternByQuestionClass("Booking.PaxQuestion").getSystemMessage());
            return StateType.EndState;
        }else{
            String pax = "";
            String date = "";
            for (EntityExtractionUtil.EntityExtractionResult r : conversation.getLatestEntities()) {
                String type = r.getEntityName().replace("$", "");
                switch(type){
                    case "email":
                        //BookingDao.getInstance().updateBookingDetail(qaId, conversation.getUserId(), type, String.join(" ", r.getEntityValue()));
                        booking.setEmail(String.join(" ", r.getEntityValue()));
                        bookingRepo.save(booking);
                        break;
                    case "phone":
                        String phone = String.join(" ", r.getEntityValue());
                        if (isValidPhone(phone)) {
                            //BookingDao.getInstance().updateBookingDetail(qaId, conversation.getUserId(), type, phone);
                            booking.setPhone(phone);
                            bookingRepo.save(booking);
                        }
                        else {
                            conversation.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("Booking.PhoneError").getSystemMessage());
                            return StateType.EndState;
                        }
                        break;
                    case "date":
                        date = String.join(" ", r.getEntityValue());
                        //String sOldDate = BookingDao.getInstance().getDate(qaId,conversation.getUserId());
                        String sOldDate = booking.getDate();
                        //Date today = new Date();
                        Date today = getCurrentDate(getCityByRestaurantId(restaurant,conversation));//getTimezone(conversation)
                        Date newDate = DateExtraction.getDate(date);
                        if(sOldDate!=null && !sOldDate.isEmpty() & DateUtils.isSameDay(today,newDate) & !date.contains("today")){
                            date = mergeDate(newDate, sOldDate);
                        }
                        if (isValidDate(newDate, today)) {
                            //if changing
                            if(conversation.getLatestQA().getMatchedPattern().getQuestionType().contains("Booking.ChangeBooking.SingleDate")){
                                message = LibraryUtil.getRandomPatternByQuestionClass("Booking.ChangeBooking.SingleDate.Response").getSystemMessage().replace("$Date", date);
                                //in case only mention hour
                            }
                            //BookingDao.getInstance().updateBookingDetail(qaId, conversation.getUserId(), type, date);
                            booking.setDate(date);
                            bookingRepo.save(booking);
                        }
                        else {
                            conversation.getLatestQA().getAnswer().setMessage("Please choose another time! The date you selected is in the past, today is: " + DateExtraction.formatDate(today));
                            return StateType.EndState;
                        }
                        break;
                    case "pax":
                        pax = String.join(" ", r.getEntityValue());
                        if (!pax.isEmpty()) {
                            Tuple2<Integer, Integer> tPax = EntityExtractionUtil.getNumberOfPax(pax);
                            pax = EntityExtractionUtil.formatOfPax(tPax);

                            //if changing
                            if(conversation.getLatestQA().getMatchedPattern().getQuestionType().contains("Booking.ChangeBooking.SinglePax")){
                                message = LibraryUtil.getRandomPatternByQuestionClass("Booking.ChangeBooking.SinglePax.Response").getSystemMessage().replace("$Pax", pax);
                            }
                            //String sOldPax = BookingDao.getInstance().getPax(qaId,conversation.getUserId());
                            String sOldPax = booking.getPax();
                            if(sOldPax==null //haven't set before
                                    || isChangingDate(conversation)){ //or user change again
                                //BookingDao.getInstance().updateBookingDetail(qaId, conversation.getUserId(), BookingDao.detailType.pax.name(), pax);
                                booking.setPax(pax);
                                bookingRepo.save(booking);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            //if changing
            if(conversation.getLatestQA().getMatchedPattern().getQuestionType().contains("Booking.ChangeBooking.Double") & !pax.isEmpty() & !date.isEmpty()){
                message = LibraryUtil.getRandomPatternByQuestionClass("Booking.ChangeBooking.Double.Response").getSystemMessage().replace("$Pax",pax).replace("$Date",date);
            }
            if (isSpecialAnswer(conversation)) {
                /*BookingDao.getInstance().updateBookingDetail(qaId, conversation.getUserId(), BookingDao.detailType.special.name(),
                        conversation.getLatestQA().getOriginalQuestion());*/
                booking.setSpecialRequest(conversation.getLatestQA().getOriginalQuestion());
                bookingRepo.save(booking);
            }
        }
        //checking if have all information to confirm booking
        Map<String,String> bookingDetail  = convertBookingToMap(booking);//BookingDao.getInstance().getBookingDetail(qaId, conversation.getUserId());
        if(bookingDetail.size()<detailType.values().length){
            for(detailType type: detailType.values()){
                if(!type.name().equalsIgnoreCase("restaurant_id")
                        & !bookingDetail.containsKey(type.name()) ){//keep asking again
                    String refinedType = type.toString().substring(0,1).toUpperCase()+type.toString().substring(1,type.toString().length());
                    message += "  " + LibraryUtil.getRandomPatternByQuestionClass("Booking." + refinedType + "Question").getSystemMessage();
                    conversation.getLatestQA().getAnswer().setMessage(message.trim());
                    return StateType.EndState;
                }
            }
        }else{//confirm booking
            //String restaurantId = bookingDetail.get(detailType.restaurant_id.name());
            message += "  " + LibraryUtil.getRandomPatternByQuestionClass("Booking.SummaryStatement").getSystemMessage();
            //RecommenderResultsRestaurant restaurant = Recommender.getInstance().getRestaurantInfoById(restaurantId);
            if(restaurant!=null) message = message.replace("$RestaurantName",restaurant.getName());

            String pax = bookingDetail.get(detailType.pax.name());
            Tuple2<Integer, Integer> tPax = EntityExtractionUtil.getNumberOfPax(pax);

            booking.setPax(pax);
            Date date = DateExtraction.getDate(bookingDetail.get(detailType.date.name()));
            String day = DateExtraction.formatDateOnly(date);
            String time = DateExtraction.formatTimeOnly(date);

            booking.setDate(day);
            booking.setTime(time);

            if(isValidTimeSlot(day, time, (int)tPax._2(), (int)tPax._1(),restaurantId)){
                conversation.getLatestQA().getAnswer().setMessage(message.trim());
                conversation.getLatestQA().getAnswer().setBookingInfo(booking);
                conversation.getLatestQA().getAnswer().setConfirmBooking(true);
            }else{
                String timeSlot = getTimeSlot(day, time, (int)tPax._2(), (int)tPax._1(),restaurantId, getCityByRestaurantId(restaurant,conversation)); //getTimezone(conversation)
                if(timeSlot.isEmpty()){
                    message = "Sorry, the restaurant is full at the moment! Please try the other restaurants.";
                }else{
                    message = LibraryUtil.getRandomPatternByQuestionClass("Booking.ConfirmBookingTime").getSystemMessage().replace("<show time>",timeSlot);
                }
                conversation.getLatestQA().getAnswer().setMessage(message.trim());
            }
        }
        return StateType.EndState;
    }

    public static Map<String,String> convertBookingToMap(Booking booking){
        Map<String, String> result = new HashedMap();
        if(booking==null){
            return result;
        }
        String restaurantId = String.valueOf(booking.getRestaurantId());
        if(restaurantId!=null && !restaurantId.isEmpty()){
            result.put(detailType.restaurant_id.name(), restaurantId);
        }
        String phone = booking.getPhone();
        if(phone!=null && !phone.isEmpty()){
            result.put(detailType.phone.name(), phone);
        }
        String email = booking.getEmail();
        if(email!=null &&  !email.isEmpty()){
            result.put(detailType.email.name(), email);
        }
        String date = booking.getDate();
        if(date!=null &&  !date.isEmpty()){
            result.put(detailType.date.name(), date);
        }
        String special = booking.getSpecialRequest();
        if(special!=null && !special.isEmpty()){
            result.put(detailType.special.name(), special);
        }
        String pax = booking.getPax();
        if(pax!=null && !pax.isEmpty()){
            result.put(detailType.pax.name(), pax);
        }
        return result;
    }

    public static int getQaWithBooking(Conversation conversation){
        List<QA> qaList = QADao.getInstance().getQACurrentSession(conversation.getQaList());
        for (int i = qaList.size() - 1; i > -1; i--) {
            QA qa = qaList.get(i);
            if (qa.getQuestion().equalsIgnoreCase(bookingActivate)) {
                return qa.getId();
            }
        }
        return -1;
    }

    public static boolean isBookingLoop(Conversation conversation){
        if(conversation.getQaList().size() < 2) return false;
        QA lastQA = conversation.getQaList().get(conversation.getQaList().size()-2);
        int qaId = getQaWithBooking(conversation);

        Booking booking = bookingRepo.findByQaIdAndUserId(qaId,conversation.getUserId());
        Map<String,String> bookingDetail  = convertBookingToMap(booking);
        if(qaId!=-1 && lastQA.getStatePaths().contains(StateType.BookingState)
                && (bookingDetail.size()<detailType.values().length || isCheckingTime(conversation)))return true;
        return false;
    }

    public static boolean isBookingExpired(Conversation conversation){
        List<QA> qaList = QADao.getInstance().getQACurrentSession(conversation.getQaList());
        if(conversation.getQaList().size() < 2) return false;
        QA lastQA = conversation.getQaList().get(conversation.getQaList().size()-2);
        if (qaList.size()==1 //only has the current question
                //and the previous is booking
                && lastQA.getStatePaths().contains(StateType.BookingState)
                //and the message is not cancelBooking or confirmBooking
                && !(lastQA.getAnswer().getMessage().contains("the restaurant is full at the moment"))
                ) return true;
        return false;
    }

    public boolean isSpecialAnswer(Conversation conversation){
        if(conversation.getQaList().size() < 2) return false;
        QA lastQA = conversation.getQaList().get(conversation.getQaList().size()-2);
        if(lastQA.getAnswer().getMessage().contains(LibraryUtil.getRandomPatternByQuestionClass("Booking.SpecialQuestion").getSystemMessage().trim())) return true;
        return false;
    }

    public static boolean isCheckingTime(Conversation conversation){
        if(conversation.getQaList().size() < 2) return false;
        QA lastQA = conversation.getQaList().get(conversation.getQaList().size()-2);
        if(lastQA.getAnswer().getMessage().contains(LibraryUtil.getRandomPatternByQuestionClass("Booking.ConfirmBookingTime")
                .getSystemMessage().replace("<show time>.","").trim()) || lastQA.getAnswer().getMessage().contains("Please choose another time!")) return true;
        return false;
    }

    public boolean isValidDate(Date date, Date today){
        if (date.after(today)) {
            return true;
        }
        return false;
    }

    public boolean isValidPhone(String phone){
        if(phone.length()>=8) return true;
        return false;
    }

    public boolean isValidTimeSlot(String day, String time, int adults, int children, String restaurantId){
        //TODO: check with 3rd party API
        TimeSlot timeSlot = timeSlotRepo.findByRestaurantIdAndDateAndTime(Long.parseLong(restaurantId),day,time);
        return timeSlot!=null;
        //return BookingDao.getInstance().isValidTimeSlot(day, time, restaurantId);
    }

    private String getTimeSlot(String day, String time, int adults, int children, String restaurantId, String timezone){
        List<String> slot = new ArrayList<>();//BookingDao.getInstance().selectValidTimeSlot(day, restaurantId, timezone);

        List<TimeSlot> timeSlots = timeSlotRepo.findByRestaurantIdAndDate(Long.parseLong(restaurantId),day);
        Date today = getCurrentDate(timezone);
        //filter time
        timeSlots.stream().forEach(x->{
            int hour = Integer.valueOf(x.getTime().split(":")[0]);
            if(x.getDate().equalsIgnoreCase(DateExtraction.formatDateOnly(today)) & today.getHours()<hour){
                String time_slot = new StringBuilder("Date: ").append(x.getDate())
                        .append(", Time: ").append(x.getTime()).toString();
                if (!slot.contains(time_slot)) slot.add(time_slot);
            }
        });

        if(slot.isEmpty()){
            return "";
        }
        return String.join("\n",slot);
    }

    public void createValidTimeSlot(List<String> restaurantIds){
        //TODO: check with 3rd party API
        List<TimeSlot> timeSlots = new ArrayList<>();
        Random random = new Random();
        List<String> times = new ArrayList<>();
        for(int i=11; i< 24; i++){
            times.add(i +":00");
        }
        try {
            for(int i=0; i< 14; i++){
                for(int j =0 ; j< 3; j++){
                    for(String restaurantId: restaurantIds){
                        Date today = new Date();
                        today.setDate(today.getDate()+i);

                        TimeSlot timeSlot = new TimeSlot();
                        timeSlot.setRestaurantId(Integer.parseInt(restaurantId));
                        timeSlot.setDate(DateExtraction.formatDateOnly(today));
                        timeSlot.setTime(times.get(random.nextInt(times.size())));
                        timeSlots.add(timeSlot);

                        if(timeSlots.size()%1000==0){
                            timeSlotRepo.save(timeSlots);
                            timeSlots.clear();
                        }
                    }
                }
            }
            timeSlotRepo.save(timeSlots);
        } catch (Exception e) {
            System.out.println("Error inserting time slot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String mergeDate(Date newDate, String sOldDate){
        Date oldDate = DateExtraction.getDate(sOldDate);
        oldDate.setHours(newDate.getHours());
        oldDate.setMinutes(newDate.getMinutes());
        return DateExtraction.formatDate(oldDate);

    }

    static SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    //Local time zone
    static SimpleDateFormat dateFormatLocal = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");

    public static Date getCurrentDate(String timezone){
        try{
            //local time
            dateFormatGmt.setTimeZone(TimeZone.getTimeZone(timezone));
            return dateFormatLocal.parse( dateFormatGmt.format(new Date()));
        }catch (Exception e){
            return new Date();
        }
    }

    public boolean isChangingDate(Conversation conversation){
        return (conversation.getLatestQA().getMatchedPattern().getQuestionType().contains("Booking.ChangeBooking.SinglePax")
                || conversation.getLatestQA().getMatchedPattern().getQuestionType().contains("Booking.ChangeBooking.Double"));
    }

    public static String getTimezone(Conversation conversation){
        //if city available, from restaurant or from QA, just return city
        String timezone = "";
        try{
            if(conversation.getLatestQA().getGeo() != null && conversation.getLatestQA().getGeo().split(",").length == 2){
                timezone = GeoCalculator.getCityFromLatLongOpenMap(conversation.getLatestQA().getGeo().split(",")[0], conversation.getLatestQA().getGeo().split(",")[1]);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        if (timezone.isEmpty()){
            timezone = "UTC";
        }
        return timezone;
    }

    public static String getCityByRestaurantId(RecommenderResultsRestaurant restaurant, Conversation conversation){
        //if city available, from restaurant or from QA, just return city
        String city = "";
        try {
            if(restaurant!=null) city = restaurant.getCity();
            if(city.isEmpty()) city = getTimezone(conversation);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return city;
    }
}
