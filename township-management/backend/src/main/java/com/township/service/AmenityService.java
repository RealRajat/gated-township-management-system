package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmenityService {

    private static final Logger log = LoggerFactory.getLogger(AmenityService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final AmenityRepository amenityRepository;
    private final AmenityBookingRepository bookingRepository;
    private final FlatRepository flatRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final UserRepository userRepository;

    public AmenityService(
            AmenityRepository amenityRepository,
            AmenityBookingRepository bookingRepository,
            FlatRepository flatRepository,
            FlatMemberRepository flatMemberRepository,
            UserRepository userRepository) {
        this.amenityRepository = amenityRepository;
        this.bookingRepository = bookingRepository;
        this.flatRepository = flatRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // RESIDENT & PUBLIC AMENITY CATALOG
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AmenityDto> getActiveAmenities() {
        return amenityRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toAmenityDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmenityDto> getAllAmenities() {
        return amenityRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAmenityDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmenityDto getAmenityById(Long id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));
        return toAmenityDto(amenity);
    }

    // =========================================================================
    // SLOT GENERATION & REAL-TIME AVAILABILITY
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SlotAvailabilityDto> getAvailableSlots(Long amenityId, LocalDate date, String username) {
        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + amenityId));

        if (date == null) {
            date = LocalDate.now();
        }

        // Identify flat IDs belonging to the logged-in resident (if any)
        Set<Long> userFlatIds = new HashSet<>();
        if (username != null && !username.isBlank()) {
            userRepository.findByEmail(username)
                    .or(() -> userRepository.findByPhone(username))
                    .ifPresent(u -> userFlatIds.addAll(flatMemberRepository.findFlatIdsByUserId(u.getId())));
        }

        // Fetch confirmed bookings for this amenity on the specified date
        List<AmenityBooking> bookings = bookingRepository.findByAmenityIdAndBookingDateAndStatus(
                amenityId, date, BookingStatus.CONFIRMED);

        // Group booked attendees by start time
        Map<LocalTime, Integer> bookedCountBySlot = new HashMap<>();
        Map<LocalTime, Boolean> bookedByMyFlatBySlot = new HashMap<>();

        for (AmenityBooking b : bookings) {
            bookedCountBySlot.merge(b.getStartTime(), b.getAttendeeCount(), Integer::sum);
            if (userFlatIds.contains(b.getFlat().getId())) {
                bookedByMyFlatBySlot.put(b.getStartTime(), true);
            }
        }

        List<SlotAvailabilityDto> slots = new ArrayList<>();
        LocalTime current = amenity.getOpenTime();
        LocalTime close = amenity.getCloseTime();
        int duration = amenity.getSlotDurationMinutes();
        int capacity = amenity.getSlotCapacity();
        boolean facilityActive = Boolean.TRUE.equals(amenity.getIsActive());

        while (current.isBefore(close)) {
            LocalTime slotEnd = current.plusMinutes(duration);
            if (slotEnd.isAfter(close)) {
                break;
            }

            LocalDateTime slotStartDateTime = LocalDateTime.of(date, current);
            boolean isPast = !slotStartDateTime.isAfter(LocalDateTime.now());
            int booked = bookedCountBySlot.getOrDefault(current, 0);
            int availableSpots = Math.max(0, capacity - booked);
            boolean bookedByMyFlat = bookedByMyFlatBySlot.getOrDefault(current, false);

            boolean isAvailable = !isPast && facilityActive && availableSpots > 0 && !bookedByMyFlat;
            String slotLabel = current.format(TIME_FMT) + " - " + slotEnd.format(TIME_FMT);

            slots.add(new SlotAvailabilityDto(
                    current,
                    slotEnd,
                    slotLabel,
                    capacity,
                    booked,
                    availableSpots,
                    isPast,
                    isAvailable,
                    bookedByMyFlat
            ));

            current = slotEnd;
        }

        return slots;
    }

    // =========================================================================
    // RESIDENT BOOKING TRANSACTION (PESSIMISTIC LOCKING & STRICT CONCURRENCY)
    // =========================================================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingConfirmationDto createBooking(CreateBookingRequest request, String username) {
        User resident = getUserByUsername(username);

        // 1. Security Check: Resident must belong to the specified flat
        if (!flatMemberRepository.existsByFlatIdAndUserId(request.flatId(), resident.getId())) {
            throw new AccessDeniedException("Forbidden: You are not authorized to book for flat ID " + request.flatId());
        }

        Flat flat = flatRepository.findById(request.flatId())
                .orElseThrow(() -> new IllegalArgumentException("Flat not found with id: " + request.flatId()));

        // 2. Pessimistic Write Lock on Amenity to prevent race conditions and overbooking
        Amenity amenity = amenityRepository.findByIdWithLock(request.amenityId())
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + request.amenityId()));

        // 3. Check if Amenity is active
        if (!Boolean.TRUE.equals(amenity.getIsActive())) {
            String reason = amenity.getMaintenanceReason() != null ? amenity.getMaintenanceReason() : "Facility temporarily closed";
            throw new IllegalStateException("Amenity '" + amenity.getName() + "' is currently unavailable: " + reason);
        }

        // 4. Validate Booking Date and Time (Must be strictly in the future)
        LocalDate bookingDate = request.bookingDate();
        LocalTime startTime = request.startTime();
        LocalDateTime slotStartDateTime = LocalDateTime.of(bookingDate, startTime);

        if (!slotStartDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Bookings cannot be made for past or immediate current time slots. Please select a future time.");
        }

        // 5. Validate Slot Alignment & Operating Hours
        LocalTime endTime = startTime.plusMinutes(amenity.getSlotDurationMinutes());
        if (startTime.isBefore(amenity.getOpenTime()) || endTime.isAfter(amenity.getCloseTime())) {
            throw new IllegalArgumentException("Requested time " + startTime + " - " + endTime +
                    " is outside operating hours (" + amenity.getOpenTime() + " - " + amenity.getCloseTime() + ")");
        }

        long minutesFromOpen = Duration.between(amenity.getOpenTime(), startTime).toMinutes();
        if (minutesFromOpen < 0 || minutesFromOpen % amenity.getSlotDurationMinutes() != 0) {
            throw new IllegalArgumentException("Start time " + startTime + " must align with slot interval of " +
                    amenity.getSlotDurationMinutes() + " minutes starting from " + amenity.getOpenTime());
        }

        // 6. Check for Overlapping / Duplicate Booking by Same Flat
        boolean duplicate = bookingRepository.existsByAmenityIdAndFlatIdAndBookingDateAndStartTimeAndStatus(
                amenity.getId(), flat.getId(), bookingDate, startTime, BookingStatus.CONFIRMED);
        if (duplicate) {
            throw new IllegalStateException("Flat " + flat.getFlatNumber() +
                    " already has a confirmed booking for this slot (" + startTime + ").");
        }

        // 7. Enforce Per-Flat Active Booking Limit
        long activeBookings = bookingRepository.countActiveBookingsForFlatAndAmenity(
                flat.getId(), amenity.getId(), LocalDate.now(), BookingStatus.CONFIRMED);
        if (activeBookings >= amenity.getMaxActiveBookingsPerFlat()) {
            throw new IllegalStateException("Flat " + flat.getFlatNumber() +
                    " has reached the maximum allowed active bookings limit of " +
                    amenity.getMaxActiveBookingsPerFlat() + " for " + amenity.getName() + ".");
        }

        // 8. Enforce Slot Capacity
        int requestedAttendees = request.attendeeCount() != null ? request.attendeeCount() : 1;
        if (requestedAttendees > amenity.getSlotCapacity()) {
            throw new IllegalArgumentException("Attendee count (" + requestedAttendees +
                    ") exceeds the maximum slot capacity (" + amenity.getSlotCapacity() + ").");
        }

        int currentAttendees = bookingRepository.sumAttendeesForSlot(
                amenity.getId(), bookingDate, startTime, BookingStatus.CONFIRMED);
        int availableCapacity = amenity.getSlotCapacity() - currentAttendees;

        if (requestedAttendees > availableCapacity) {
            throw new IllegalStateException("Slot capacity exceeded! Available spots remaining: " +
                    Math.max(0, availableCapacity) + " (requested: " + requestedAttendees + ").");
        }

        // 9. Generate Unique Booking Reference
        String bookingReference = generateUniqueBookingReference();

        // 10. Persist Booking
        AmenityBooking booking = new AmenityBooking(
                bookingReference,
                amenity,
                flat,
                resident,
                bookingDate,
                startTime,
                endTime,
                requestedAttendees,
                request.purpose()
        );

        AmenityBooking saved = bookingRepository.save(booking);
        log.info("Amenity booking created successfully: ref={}, amenity={}, flat={}, date={}, time={}",
                bookingReference, amenity.getCode(), flat.getFlatNumber(), bookingDate, startTime);

        return toConfirmationDto(saved, resident.getId(), false);
    }

    // =========================================================================
    // RESIDENT MY BOOKINGS & LOOKUP
    // =========================================================================

    @Transactional
    public List<BookingConfirmationDto> getResidentBookings(String username) {
        User resident = getUserByUsername(username);
        List<Long> flatIds = flatMemberRepository.findFlatIdsByUserId(resident.getId());

        List<AmenityBooking> bookings;
        if (flatIds.isEmpty()) {
            bookings = bookingRepository.findByResidentIdOrderByBookingDateDescStartTimeDesc(resident.getId());
        } else {
            bookings = bookingRepository.findByFlatIdInOrderByBookingDateDescStartTimeDesc(flatIds);
        }

        // Auto-complete past confirmed bookings
        autoCompletePastBookings(bookings);

        return bookings.stream()
                .map(b -> toConfirmationDto(b, resident.getId(), false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingConfirmationDto getBookingByReference(String reference, String username, boolean isHelpDesk) {
        AmenityBooking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with reference: " + reference));

        User user = getUserByUsername(username);
        if (!isHelpDesk) {
            boolean isMember = flatMemberRepository.existsByFlatIdAndUserId(booking.getFlat().getId(), user.getId());
            if (!isMember) {
                throw new AccessDeniedException("Forbidden: You cannot view bookings for other flats.");
            }
        }

        return toConfirmationDto(booking, user.getId(), isHelpDesk);
    }

    // =========================================================================
    // CANCELLATION WORKFLOW
    // =========================================================================

    @Transactional
    public BookingConfirmationDto cancelBooking(Long bookingId, CancelBookingRequest request, String username, boolean isHelpDesk) {
        AmenityBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        User user = getUserByUsername(username);

        // Security Check
        if (!isHelpDesk) {
            boolean isMember = flatMemberRepository.existsByFlatIdAndUserId(booking.getFlat().getId(), user.getId());
            if (!isMember) {
                throw new AccessDeniedException("Forbidden: You cannot cancel bookings for another flat.");
            }
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be cancelled. Current status is " + booking.getStatus());
        }

        // Resident Cancellation Deadline Enforcement
        LocalDateTime slotStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        int deadlineHours = booking.getAmenity().getCancelDeadlineHours();
        LocalDateTime deadline = slotStart.minusHours(deadlineHours);

        if (!isHelpDesk && LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException("Cancellation deadline passed. Bookings for " +
                    booking.getAmenity().getName() + " must be cancelled at least " +
                    deadlineHours + " hours before the start time.");
        }

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason()
                : (isHelpDesk ? "Cancelled by Help Desk Administration" : "Cancelled by Resident");

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(user);

        AmenityBooking updated = bookingRepository.save(booking);
        log.info("Booking cancelled: ref={}, user={}, reason={}", updated.getBookingReference(), user.getEmail(), reason);

        return toConfirmationDto(updated, user.getId(), isHelpDesk);
    }

    // =========================================================================
    // HELP DESK ADMINISTRATIVE MANAGEMENT
    // =========================================================================

    @Transactional
    public AmenityDto createAmenity(CreateAmenityRequest request) {
        if (amenityRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("An amenity with code '" + request.code() + "' already exists.");
        }
        if (amenityRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("An amenity with name '" + request.name() + "' already exists.");
        }
        if (request.closeTime().isBefore(request.openTime())) {
            throw new IllegalArgumentException("Close time must be after open time.");
        }

        Amenity amenity = new Amenity(
                request.name(),
                request.code().toUpperCase().trim(),
                request.category(),
                request.description(),
                request.location(),
                request.openTime(),
                request.closeTime(),
                request.slotDurationMinutes(),
                request.slotCapacity(),
                request.maxActiveBookingsPerFlat(),
                request.cancelDeadlineHours(),
                request.rulesAndGuidelines()
        );

        Amenity saved = amenityRepository.save(amenity);
        log.info("Help Desk created amenity: code={}, name={}", saved.getCode(), saved.getName());
        return toAmenityDto(saved);
    }

    @Transactional
    public AmenityDto updateAmenity(Long id, UpdateAmenityRequest request) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        if (request.closeTime().isBefore(request.openTime())) {
            throw new IllegalArgumentException("Close time must be after open time.");
        }

        amenity.setName(request.name());
        amenity.setCategory(request.category());
        amenity.setDescription(request.description());
        amenity.setLocation(request.location());
        amenity.setOpenTime(request.openTime());
        amenity.setCloseTime(request.closeTime());
        amenity.setSlotDurationMinutes(request.slotDurationMinutes());
        amenity.setSlotCapacity(request.slotCapacity());
        amenity.setMaxActiveBookingsPerFlat(request.maxActiveBookingsPerFlat());
        amenity.setCancelDeadlineHours(request.cancelDeadlineHours());
        if (request.isActive() != null) {
            amenity.setIsActive(request.isActive());
        }
        if (request.maintenanceReason() != null) {
            amenity.setMaintenanceReason(request.maintenanceReason());
        }
        amenity.setRulesAndGuidelines(request.rulesAndGuidelines());

        Amenity saved = amenityRepository.save(amenity);
        log.info("Help Desk updated amenity: code={}, name={}", saved.getCode(), saved.getName());
        return toAmenityDto(saved);
    }

    @Transactional
    public AmenityDto toggleAmenityStatus(Long id, ToggleAmenityStatusRequest request) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        amenity.setIsActive(request.isActive());
        if (Boolean.FALSE.equals(request.isActive())) {
            amenity.setMaintenanceReason(request.maintenanceReason() != null ? request.maintenanceReason() : "Closed for maintenance");
        } else {
            amenity.setMaintenanceReason(null);
        }

        Amenity saved = amenityRepository.save(amenity);
        log.info("Amenity status toggled: code={}, active={}", saved.getCode(), saved.getIsActive());
        return toAmenityDto(saved);
    }

    @Transactional
    public List<BookingConfirmationDto> getAllBookings(LocalDate date, Long amenityId, BookingStatus status, Long flatId, String adminUsername) {
        User admin = getUserByUsername(adminUsername);
        List<AmenityBooking> bookings = bookingRepository.searchBookings(date, amenityId, status, flatId);

        // Auto-complete past confirmed bookings
        autoCompletePastBookings(bookings);

        return bookings.stream()
                .map(b -> toConfirmationDto(b, admin.getId(), true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmenityOverviewKpiDto getOverviewKpis() {
        long totalAmenities = amenityRepository.count();
        long activeAmenities = amenityRepository.countByIsActiveTrue();
        long bookingsToday = bookingRepository.countByBookingDate(LocalDate.now());
        long upcomingBookingsWeek = bookingRepository.countUpcomingBookingsBetween(
                LocalDate.now(), LocalDate.now().plusDays(7));
        long cancelledBookingsMonth = bookingRepository.countCancelledSince(LocalDate.now().minusDays(30));

        return new AmenityOverviewKpiDto(
                totalAmenities,
                activeAmenities,
                bookingsToday,
                upcomingBookingsWeek,
                cancelledBookingsMonth
        );
    }

    // =========================================================================
    // HELPER METHODS & DTO MAPPERS
    // =========================================================================

    private void autoCompletePastBookings(List<AmenityBooking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        for (AmenityBooking b : bookings) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                LocalDateTime slotEnd = LocalDateTime.of(b.getBookingDate(), b.getEndTime());
                if (slotEnd.isBefore(now)) {
                    b.setStatus(BookingStatus.COMPLETED);
                    bookingRepository.save(b);
                }
            }
        }
    }

    private String generateUniqueBookingReference() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("BK-");
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
            }
            String ref = sb.toString();
            if (bookingRepository.findByBookingReference(ref).isEmpty()) {
                return ref;
            }
        }
        return "BK-" + System.currentTimeMillis();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private AmenityDto toAmenityDto(Amenity a) {
        return new AmenityDto(
                a.getId(),
                a.getName(),
                a.getCode(),
                a.getCategory(),
                a.getDescription(),
                a.getLocation(),
                a.getOpenTime(),
                a.getCloseTime(),
                a.getSlotDurationMinutes(),
                a.getSlotCapacity(),
                a.getMaxActiveBookingsPerFlat(),
                a.getCancelDeadlineHours(),
                a.getIsActive(),
                a.getMaintenanceReason(),
                a.getRulesAndGuidelines(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private BookingConfirmationDto toConfirmationDto(AmenityBooking b, Long currentUserId, boolean isHelpDesk) {
        Flat flat = b.getFlat();
        Tower tower = flat.getTower();
        User resident = b.getResident();
        Amenity amenity = b.getAmenity();

        String slotLabel = b.getStartTime().format(TIME_FMT) + " - " + b.getEndTime().format(TIME_FMT);

        LocalDateTime slotStart = LocalDateTime.of(b.getBookingDate(), b.getStartTime());
        LocalDateTime cancellationDeadline = slotStart.minusHours(amenity.getCancelDeadlineHours());
        boolean canCancel = b.getStatus() == BookingStatus.CONFIRMED &&
                (isHelpDesk || LocalDateTime.now().isBefore(cancellationDeadline));

        String cancelledByName = b.getCancelledBy() != null ? b.getCancelledBy().getFullName() : null;

        return new BookingConfirmationDto(
                b.getId(),
                b.getBookingReference(),
                amenity.getId(),
                amenity.getName(),
                amenity.getCode(),
                amenity.getCategory(),
                amenity.getLocation(),
                flat.getId(),
                tower != null ? tower.getId() : null,
                tower != null ? tower.getName() : null,
                tower != null ? tower.getId() + "-" + flat.getFlatNumber() : flat.getFlatNumber(),
                resident != null ? resident.getId() : null,
                resident != null ? resident.getFullName() : null,
                resident != null ? resident.getPhone() : null,
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                slotLabel,
                b.getAttendeeCount(),
                b.getPurpose(),
                b.getStatus(),
                b.getCancellationReason(),
                b.getCancelledAt(),
                cancelledByName,
                cancellationDeadline,
                canCancel,
                amenity.getRulesAndGuidelines(),
                b.getCreatedAt()
        );
    }
}
