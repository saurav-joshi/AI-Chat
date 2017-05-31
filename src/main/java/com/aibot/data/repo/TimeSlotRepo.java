package com.aibot.data.repo;

import com.aibot.data.model.TimeSlot;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TimeSlotRepo extends PagingAndSortingRepository<TimeSlot, Long> {
    public TimeSlot findByRestaurantIdAndDateAndTime(long restaurantId, String date, String time);
    public List<TimeSlot> findByRestaurantIdAndDate(long restaurantId, String date);
}
