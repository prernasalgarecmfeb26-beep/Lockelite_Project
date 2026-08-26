package com.lockelite.cron;

import com.lockelite.audit.AuditLogService;
import com.lockelite.model.Allocation;
import com.lockelite.repository.AllocationRepository;
import com.lockelite.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class RentManagementCron {

    private static final Logger log = LoggerFactory.getLogger(RentManagementCron.class);
    private static final BigDecimal PENALTY_PER_DAY = new BigDecimal("50.00");
    private static final int RECLAIM_DAYS = 30;

    @Autowired private AllocationRepository allocationRepository;
    @Autowired private EmailService emailService;
    @Autowired private AuditLogService auditLogService;

    /**
     * Runs every day at midnight — checks for expiring/overdue allocations
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processRentManagement() {
        log.info("=== RentManagementCron running at {} ===", LocalDateTime.now());

        List<Allocation> approvedAllocations = allocationRepository.findByStatus(Allocation.AllocationStatus.APPROVED);

        for (Allocation allocation : approvedAllocations) {
            LocalDate approvedDate = allocation.getApprovedAt() != null ? allocation.getApprovedAt().toLocalDate() : LocalDate.now();
            LocalDate expiryDate = approvedDate.plusMonths(allocation.getTenureMonths());
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
            long daysOverdue = ChronoUnit.DAYS.between(expiryDate, LocalDate.now());

            // 7 days reminder
            if (daysUntilExpiry == 7 || daysUntilExpiry == 3 || daysUntilExpiry == 1) {
                log.info("Sending rent reminder to customer {} for locker {}", allocation.getCustomer().getId(), allocation.getLocker().getLockerNumber());
                emailService.sendRentReminderEmail(
                    allocation.getCustomer().getEmail(),
                    allocation.getCustomer().getFullName(),
                    allocation.getLocker().getLockerNumber(),
                    expiryDate.toString(),
                    allocation.getRentAmount().toString()
                );
            }

            // Overdue processing
            if (daysOverdue > 0) {
                BigDecimal penalty = PENALTY_PER_DAY.multiply(BigDecimal.valueOf(daysOverdue));
                log.warn("Locker {} is {} days overdue. Penalty: ₹{}", allocation.getLocker().getLockerNumber(), daysOverdue, penalty);

                auditLogService.log(null, "RENT_OVERDUE",
                    "Allocation", allocation.getId(),
                    null, "OVERDUE_" + daysOverdue + "_DAYS_PENALTY_" + penalty, null
                );

                // Flag for reclaim after 30 days
                if (daysOverdue >= RECLAIM_DAYS) {
                    log.error("RECLAIM REQUIRED: Locker {} is {} days overdue!", allocation.getLocker().getLockerNumber(), daysOverdue);
                    auditLogService.log(null, "LOCKER_FLAGGED_FOR_RECLAIM",
                        "Allocation", allocation.getId(), null, "RECLAIM_FLAG", null
                    );
                }
            }
        }

        log.info("=== RentManagementCron completed. Processed {} allocations ===", approvedAllocations.size());
    }

    /**
     * Runs every Monday at 9 AM — weekly summary
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void weeklyRentSummary() {
        log.info("Generating weekly rent summary...");
        // Weekly summary logic here
    }
}
