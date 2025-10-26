package com.mariuszilinskas.streamix.auth.identity.dto;

import com.mariuszilinskas.streamix.auth.identity.enums.UserAuthority;
import com.mariuszilinskas.streamix.auth.identity.enums.UserRole;
import com.mariuszilinskas.streamix.auth.identity.enums.UserStatus;

import java.util.List;
import java.util.UUID;

public record AuthDetails(
        UUID userId,
        List<UserRole> roles,
        List<UserAuthority> authorities,
        UserStatus status
) {}
