package io.github.metdaisy.amaazon.user.presentation.dto.response;

import java.time.Instant;

public record UserResponse(String name,
                           String phoneNumber,
                           int pointBalance,
                           String address,
                           Instant createdAt) {

}
