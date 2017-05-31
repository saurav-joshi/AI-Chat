package com.aibot.data.repo;

import com.aibot.data.model.Booking;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BookingRepo extends PagingAndSortingRepository<Booking, Long> {
    public Booking findByQaIdAndUserId(long qaId, String userId);
    public Booking findFirstByUserId(String userId);
}
