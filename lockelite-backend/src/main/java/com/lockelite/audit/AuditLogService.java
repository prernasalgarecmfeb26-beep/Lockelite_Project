package com.lockelite.audit;

import com.lockelite.model.AuditLog;
import com.lockelite.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired private AuditLogRepository auditLogRepository;

    @Async
    public void log(Long userId, String action, String entityType, Long entityId,
                    String previousState, String newState, String ipAddress) {
        try {
            String previousHash = auditLogRepository.findTopByOrderByTimestampDesc()
                    .map(AuditLog::getCurrentHash)
                    .orElse("GENESIS_BLOCK_LOCKELITE_2026");

            String dataToHash = action + userId + entityType + entityId + LocalDateTime.now() + previousHash;
            String currentHash = sha256(dataToHash);

            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .previousState(previousState)
                    .newState(newState)
                    .ipAddress(ipAddress)
                    .previousHash(previousHash)
                    .currentHash(currentHash)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Audit log failed for action {}: {}", action, e.getMessage());
        }
    }

    // Constant previousHash value the very first audit entry is created with
    // (see log() below). Used to detect tampering of the genesis entry
    // itself, which the pairwise loop alone can't catch since it only
    // compares each entry against its immediate predecessor starting at i=1.
    private static final String GENESIS_HASH = "GENESIS_BLOCK_LOCKELITE_2026";

    public List<AuditLog> getAllLogs() {
        List<AuditLog> logs = auditLogRepository.findAllOrderByTimestampDesc();
        annotateChainValidity(logs);
        return logs;
    }

    public boolean verifyChainIntegrity() {
        List<AuditLog> logs = auditLogRepository.findAll();
        if (logs.isEmpty()) return true;
        logs.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        // Genesis entry must chain back to the known constant — without this
        // check, tampering with the very first record was never detected.
        if (!GENESIS_HASH.equals(logs.get(0).getPreviousHash())) return false;
        for (int i = 1; i < logs.size(); i++) {
            if (!logs.get(i).getPreviousHash().equals(logs.get(i - 1).getCurrentHash())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets chainValid on each entry by walking the chain chronologically.
     * The repository query returns newest-first for display, which is the
     * opposite order the hash chain was built in — sort a copy before
     * comparing so each entry is checked against its true predecessor.
     * Since the copy holds the same object references, mutating it also
     * mutates the entries in the caller's (differently-ordered) list.
     */
    private void annotateChainValidity(List<AuditLog> logs) {
        List<AuditLog> chronological = new java.util.ArrayList<>(logs);
        chronological.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        for (int i = 0; i < chronological.size(); i++) {
            AuditLog entry = chronological.get(i);
            String expectedPreviousHash = (i == 0) ? GENESIS_HASH : chronological.get(i - 1).getCurrentHash();
            entry.setChainValid(expectedPreviousHash.equals(entry.getPreviousHash()));
        }
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
