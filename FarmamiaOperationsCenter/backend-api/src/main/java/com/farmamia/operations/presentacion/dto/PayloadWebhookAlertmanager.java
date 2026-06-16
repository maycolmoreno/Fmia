package com.farmamia.operations.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record PayloadWebhookAlertmanager(
    String version,
    String groupKey,
    Integer truncatedAlerts,
    String status,
    String receiver,
    Map<String, String> groupLabels,
    Map<String, String> commonLabels,
    Map<String, String> commonAnnotations,
    @JsonProperty("externalURL") String externalUrl,
    List<AlertaWebhook> alerts
) {

    public record AlertaWebhook(
        String status,
        Map<String, String> labels,
        Map<String, String> annotations,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        @JsonProperty("generatorURL") String generatorUrl,
        String fingerprint
    ) {}
}
