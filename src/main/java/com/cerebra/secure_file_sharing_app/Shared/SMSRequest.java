package com.cerebra.secure_file_sharing_app.Shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSRequest {
    private String phoneNumber;
    private String message;
}