package com.township.repository;

import com.township.entity.AmenityBooking;
import com.township.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, Long> {

    Optional<AmenityBooking> findByBookingReference(String bookingReference);

    List<AmenityBooking> findByResidentIdOrderByBookingDateDescStartTimeDesc(Long residentId);

    List<AmenityBooking> findByFlatIdInOrderByBookingDateDescStartTimeDesc(List<Long> flatIds);

    boolean existsByAmenityIdAndFlatIdAndBookingDateAndStartTimeAndStatus(
            Long amenityId, Long flatId, LocalDate bookingDate, LocalTime startTime, BookingStatus status);

    @Query("SELECT COUNT(b) FROM AmenityBooking b WHERE b.flat.id = :flatId AND b.amenity.id = :amenityId AND b.bookingDate >= :fromDate AND b.status = :status")
    long countActiveBookingsForFlatAndAmenity(
            @Param("flatId") Long flatId,
            @Param("amenityId") Long amenityId,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.attendeeCount), 0) FROM AmenityBooking b WHERE b.amenity.id = :amenityId AND b.bookingDate = :bookingDate AND b.startTime = :startTime AND b.status = :status")
    int sumAttendeesForSlot(
            @Param("amenityId") Long amenityId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("status") BookingStatus status);

    List<AmenityBooking> findByAmenityIdAndBookingDateAndStatus(
            Long amenityId, LocalDate bookingDate, BookingStatus status);

    List<AmenityBooking> findByAmenityIdAndBookingDateAndStartTimeAndStatus(
            Long amenityId, LocalDate bookingDate, LocalTime startTime, BookingStatus status);

    @Query("SELECT b FROM AmenityBooking b WHERE " +
           "(:date IS NULL OR b.bookingDate = :date) AND " +
           "(:amenityId IS NULL OR b.amenity.id = :amenityId) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:flatId IS NULL OR b.flat.id = :flatId) " +
           "ORDER BY b.bookingDate DESC, b.startTime DESC")
    List<AmenityBooking> searchBookings(
            @Param("date") LocalDate date,
            @Param("amenityId") Long amenityId,
            @Param("status") BookingStatus status,
            @Param("flatId") Long flatId);

    long countByBookingDate(LocalDate date);

    long countByBookingDateAndStatus(LocalDate date, BookingStatus status);

    @Query("SELECT COUNT(b) FROM AmenityBooking b WHERE b.bookingDate >= :startDate AND b.bookingDate <= :endDate AND b.status = 'CONFIRMED'")
    long countUpcomingBookingsBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(b) FROM AmenityBooking b WHERE b.bookingDate >= :startDate AND b.status = 'CANCELLED'")
    long countCancelledSince(@Param("startDate") LocalDate startDate);
}

