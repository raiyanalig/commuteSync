package com.commutesync.route.dto;

import com.commutesync.route.domain.Stop;

public record StopResponse(
        Long id,
        String name,
        String address,
        int stopOrder
) {

    public static StopResponse from(Stop stop) {
        return new StopResponse(
                stop.getId(),
                stop.getName(),
                stop.getAddress(),
                stop.getStopOrder()
        );
    }
}
