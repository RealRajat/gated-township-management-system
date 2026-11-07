package com.township.dto;

import com.township.entity.Role;
import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
    Long id,
    String fullName,
    String email,
    String phone,
    Role role,
    boolean active,
    LocalDateTime createdAt,
    List<FlatSummaryDto> flats
) {}
