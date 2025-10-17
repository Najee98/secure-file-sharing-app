package com.cerebra.secure_file_sharing_app.SMS;

import com.cerebra.secure_file_sharing_app.SMS.DTO.SMSResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
@Slf4j
public class SMSServiceImpl implements SMSService {

    @Override
    public SMSResponse sendSMS(String phoneNumber, String message) {
        log.info("Mock SMS sent to {}: '{}'", phoneNumber, message);

        try {
            // Small delay for realism
            Thread.sleep(100);

            String successMsg = String.format("SMS successfully sent to %s", phoneNumber);
            return SMSResponse.success(successMsg);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SMSResponse.failure("SMS service interrupted");
        }
    }
}