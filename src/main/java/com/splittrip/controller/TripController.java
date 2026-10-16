package com.splittrip.controller;

import com.splittrip.dto.AddParticipantRequest;
import com.splittrip.dto.CreateTripRequest;
import com.splittrip.dto.ParticipantResponse;
import com.splittrip.dto.TripResponse;
import com.splittrip.dto.TripSummaryResponse;
import com.splittrip.security.CurrentUser;
import com.splittrip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public List<TripSummaryResponse> listTrips(@AuthenticationPrincipal Jwt jwt) {
        return tripService.listTrips(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse createTrip(@Valid @RequestBody CreateTripRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return tripService.createTrip(request, CurrentUser.id(jwt));
    }

    @GetMapping("/{tripId}")
    public TripResponse getTrip(@PathVariable Long tripId, @AuthenticationPrincipal Jwt jwt) {
        return tripService.getTrip(tripId, CurrentUser.id(jwt));
    }

    @PostMapping("/{tripId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse addParticipant(@PathVariable Long tripId,
                                              @Valid @RequestBody AddParticipantRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        return tripService.addParticipant(tripId, request, CurrentUser.id(jwt));
    }
}