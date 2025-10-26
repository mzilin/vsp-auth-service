package com.mariuszilinskas.streamix.auth.identity.client;

import com.mariuszilinskas.streamix.auth.identity.dto.AuthDetails;
import com.mariuszilinskas.streamix.auth.identity.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient("users")
public interface UserFeignClient {

    @GetMapping(value = "/user/auth-details/by-email", consumes = "application/json")
    AuthDetails getUserAuthDetailsByEmail(@RequestParam String email);

    @GetMapping(value = "/user/auth-details/by-userid", consumes = "application/json")
    AuthDetails getUserAuthDetailsByUserId(@RequestParam UUID userId);

    @GetMapping(value = "/user/{userId}", consumes = "application/json")
    UserResponse getUser(@PathVariable("userId") UUID userId);

}
