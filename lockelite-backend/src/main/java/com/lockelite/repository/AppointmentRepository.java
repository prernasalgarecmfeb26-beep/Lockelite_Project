package com.lockelite.repository;

import com.lockelite.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.customer.id = :customerId ORDER BY a.visitDate DESC")
    List<Appointment> findByCustomerId(@Param("customerId") Long customerId);

    List<Appointment> findByStatus(Appointment.AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.branch.id = :branchId " +
           "AND a.status IN ('UPCOMING','CONFIRMED') " +
           "ORDER BY a.visitDate ASC, a.visitTime ASC")
    List<Appointment> findUpcomingByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.visitDate = :date")
    long countByBranchIdAndVisitDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    List<Appointment> findByBranchId(Long branchId);

    /**
     * Used by DigitalKeyCron — finds CONFIRMED appointments for today
     * where the digital key email has NOT been sent yet.
     */
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.customer " +
           "LEFT JOIN FETCH a.locker " +
           "LEFT JOIN FETCH a.branch " +
           "WHERE a.visitDate = :today " +
           "AND a.status = 'CONFIRMED' " +
           "AND (a.digitalKeySent = false OR a.digitalKeySent IS NULL)")
    List<Appointment> findConfirmedTodayKeyNotSent(@Param("today") LocalDate today);

    /**
     * For employee to verify a digital key at the branch.
     * Deliberately does NOT filter on visitDate — that's already governed by
     * digitalKeyExpiresAt (checked in EmployeeServiceImpl.verifyDigitalKey),
     * which correctly allows the key up to 2 hours past the scheduled visit
     * time even if the calendar date has rolled over since. Requiring an
     * exact visitDate == today match here caused a perfectly valid,
     * unexpired key to be rejected as "Invalid or expired" the moment
     * midnight passed, regardless of whether the key was still in its
     * legitimate window.
     */
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.digitalKey = :key " +
           "AND a.status = 'CONFIRMED'")
    java.util.Optional<Appointment> findByDigitalKey(@Param("key") String key);
}