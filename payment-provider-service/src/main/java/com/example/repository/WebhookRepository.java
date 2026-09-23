package com.example.repository;

import com.example.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {
}
