package com.mariuszilinskas.streamix.auth.identity.service;

import java.util.UUID;

public interface DataDeletionService {

    void deleteUserAuthData(UUID userId);

}
