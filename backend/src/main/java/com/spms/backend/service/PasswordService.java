package com.spms.backend.service;

import com.spms.backend.dto.request.PasswordChangeRequest;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.model.User;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.spms.backend.exception.UnauthorizedException;

import java.util.Optional;

@Service
public class PasswordService {

    private final UserRepository userRepository;

    public PasswordService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void changePassword(PasswordChangeRequest request) {

        String email = requireText(request.email(), "Email is required.");
        String tempPassword = requireText(request.tempPassword(), "Temporary password is required.");
        String newPassword = requireText(request.newPassword(), "New password is required.");

        
        Optional<User> optionalUser = userRepository.findByStudentId(email);

        if (optionalUser.isEmpty()) {
            throw new BadRequestException("User not found.");
        }

        User user = optionalUser.get();

        
        if (!tempPassword.equals(user.getPasswordHash())) {
            throw new UnauthorizedException("Temporary password is incorrect.");
        }

       
        user.setPasswordHash(newPassword);

     
        user.setRequiresPasswordChange(false);

        userRepository.save(user);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }
}