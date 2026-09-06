package com.academy.project.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Email(message = "Must be a valid email")
    @Size(max = 255)
    private String email;

    @Size(max = 150, message = "UG degree must be at most 150 characters")
    private String ugDegree;

    @Size(max = 150, message = "PG degree must be at most 150 characters")
    private String pgDegree;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
}
