package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "webhooks")
public class Webhook {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhookIdGenerator")
    @SequenceGenerator(name = "webhookIdGenerator", sequenceName = "webhook_id_seq")
    private Long id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "payload")
    private String payload;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "notification_url")
    private String notificationUrl;
}
