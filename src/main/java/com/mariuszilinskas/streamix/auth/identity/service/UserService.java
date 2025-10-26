package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.AuthDetails;

import java.util.UUID;

public interface UserService {

    AuthDetails getUserAuthDetailsWithEmail(String email);

    AuthDetails getUserAuthDetailsWithId(UUID userId);

}
