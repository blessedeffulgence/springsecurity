package com.spring.security.service.impl;

import com.spring.security.entity.PasswordResetToken;
import com.spring.security.entity.User;
import com.spring.security.entity.VerificationToken;
import com.spring.security.model.UserModel;
import com.spring.security.repository.PasswordResetRepository;
import com.spring.security.repository.RegistrationRepository;
import com.spring.security.repository.VerificationTokenRepository;
import com.spring.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final RegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetRepository passwordResetRepository;

    public UserServiceImpl(RegistrationRepository registrationRepository, PasswordEncoder passwordEncoder,
                           VerificationTokenRepository verificationTokenRepository,
                           PasswordResetRepository passwordResetRepository) {
        this.registrationRepository = registrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetRepository = passwordResetRepository;
    }

    @Override
    public User registerUser(UserModel userModel) {
        User user = new User();
        user.setFirstName(userModel.getFirstName());
        user.setLastName(userModel.getLastName());
        user.setEmail(userModel.getEmail());
        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(userModel.getPassword()));
        registrationRepository.save(user);
        return user;
    }

    @Override
    public void saveVerificationTokenForUser(User user, String token) {
        VerificationToken verificationToken = new VerificationToken(user,token);
        verificationTokenRepository.save(verificationToken);

    }

    @Override
    public String validateVerificationToken(String token) {

        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);

        if (verificationToken == null)
        {
            return "invalid";
        }

        User user = verificationToken.getUser();
        Calendar calendar = Calendar.getInstance();

        if (verificationToken.getExpirationTime().getTime() - calendar.getTime().getTime() <=0)
        {
            verificationTokenRepository.delete(verificationToken);
            return "Token expired";

        }
        user .setEnabled(true);
        registrationRepository.save(user);

        return "valid";
    }

    @Override
    public VerificationToken generateNewVerificationToken(String oldToken) {

        VerificationToken verificationToken = verificationTokenRepository.findByToken(oldToken);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationTokenRepository.save(verificationToken);
        return verificationToken;
    }

    @Override
    public User findUserByEmail(String email) {
        User user = registrationRepository.findByEmail(email);
        return user;
    }

    @Override
    public void createPasswordResetToken(User user, String token) {
        PasswordResetToken passwordResetToken = new PasswordResetToken(user,token);

        passwordResetRepository.save(passwordResetToken);
    }

    @Override
    public String validatePasswordResetToken(String token) {
        PasswordResetToken passwordResetToken = passwordResetRepository.findByToken(token);

        if (passwordResetToken == null)
        {
            return "invalid";
        }

        Calendar calendar = Calendar.getInstance();

        if (passwordResetToken.getExpirationTime().getTime() - calendar.getTime().getTime() <=0)
        {
            passwordResetRepository.delete(passwordResetToken);
            return "Token expired";

        }

        return "valid";
    }

    @Override
    public Optional<User> getUserByPasswordResetToken(String token) {
        return Optional.ofNullable(passwordResetRepository.findByToken(token).getUser());
    }

    @Override
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        registrationRepository.save(user);
    }

    @Override
    public boolean checkOldPassword(User user, String oldPassword) {
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }
}
