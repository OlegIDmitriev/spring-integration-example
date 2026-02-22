package com.dmitriev.i.oleg.si.example.config.integration.model;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public class ExternalServiceRequest {
    private UUID integrationId;
    private ZonedDateTime createdAt;
}
