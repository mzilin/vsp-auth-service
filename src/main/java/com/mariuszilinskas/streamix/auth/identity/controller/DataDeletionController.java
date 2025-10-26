package com.mariuszilinskas.streamix.auth.identity.controller;

import com.mariuszilinskas.streamix.auth.identity.service.DataDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * This class provides REST APIs for deleting user data in auth service.
 *
 * @author Marius Zilinskas
 */
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataDeletionController {

    private final DataDeletionService dataDeletionService;

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserAuthData(@PathVariable UUID userId){
        dataDeletionService.deleteUserAuthData(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
