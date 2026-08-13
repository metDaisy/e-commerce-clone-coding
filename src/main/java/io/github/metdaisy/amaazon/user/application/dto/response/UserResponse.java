package io.github.metdaisy.amaazon.user.application.dto.response;

import java.time.Instant;

public record UserResponse(String name, String phoneNumber, int pointBalance, String address,
                           Instant createdAt) {

}
