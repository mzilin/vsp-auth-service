package com.mariuszilinskas.streamix.auth.identity.service;

import com.mariuszilinskas.streamix.auth.identity.dto.VerifyPasscodeRequest;

import java.util.UUID;

public interface PasscodeService {

    void verifyPasscode(UUID userId, VerifyPasscodeRequest request);

    void createPasscode(UUID userId, String firstName, String email);

    void resetPasscode(UUID userId);

    void deleteUserPasscodes(UUID userId);

    void deleteExpiredPasscodes();

}
