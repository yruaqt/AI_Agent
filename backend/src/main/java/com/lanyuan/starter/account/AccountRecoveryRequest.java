package com.lanyuan.starter.account;

import com.lanyuan.starter.database.entity.BusinessEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "account_recovery_request")
public class AccountRecoveryRequest extends BusinessEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 16)
    private RecoveryRequestType requestType;

    @Column(name = "requested_username", length = 64)
    private String requestedUsername;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(nullable = false, length = 128)
    private String contact;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RecoveryRequestStatus status = RecoveryRequestStatus.PENDING;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    public RecoveryRequestType getRequestType() { return requestType; }
    public void setRequestType(RecoveryRequestType requestType) { this.requestType = requestType; }
    public String getRequestedUsername() { return requestedUsername; }
    public void setRequestedUsername(String requestedUsername) { this.requestedUsername = requestedUsername; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public RecoveryRequestStatus getStatus() { return status; }
    public void setStatus(RecoveryRequestStatus status) { this.status = status; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public OffsetDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(OffsetDateTime handledAt) { this.handledAt = handledAt; }
}
