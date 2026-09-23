package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "webhooks")
public class Webhook {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhookIdGenerator")
    @SequenceGenerator(schema = "payment", name = "webhookIdGenerator", sequenceName = "webhooks_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "payload")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "notification_url")
    private String notificationUrl;
}
