package com.mrattorneys.contactapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String message
) {}