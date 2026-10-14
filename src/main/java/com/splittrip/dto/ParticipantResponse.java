package com.splittrip.dto;

import com.splittrip.model.Participant;

public record ParticipantResponse(Long id, String name) {

    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getName());
    }
}