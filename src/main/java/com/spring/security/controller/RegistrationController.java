package com.spring.security.controller;

import com.spring.security.entity.User;
import com.spring.security.entity.VerificationToken;
import com.spring.security.event.RegistrationCompleteEvent;
import com.spring.security.model.PasswordModel;
import com.spring.security.model.UserModel;
import com.spring.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

@RestController
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    private final UserService userService;
    private final ApplicationEventPublisher publisher;

    public RegistrationController(UserService userService, ApplicationEventPublisher publisher) {
        this.userService = userService;
        this.publisher = publisher;
    }

    @PostMapping("/register")
    public String registerUser(@RequestBody UserModel userModel, final HttpServletRequest request)
    {
        User user = userService.registerUser(userModel);
        publisher.publishEvent(new RegistrationCompleteEvent(
                user, applicationUrl(request)
        ));
        return "Success";

    }

    @GetMapping("/verifyRegistration")
    public String verifyRegistration(@RequestParam("token") String token)
    {
        String result = userService.validateVerificationToken(token);

        if (result.equalsIgnoreCase("valid"))
        {
            return "User enabled successfully";
        }
        else
            return "Bad token";
    }

    @GetMapping("/resendVerificationToken")
    public String resendVerificationToken(@RequestParam("token") String oldToken, final HttpServletRequest request)
    {
        VerificationToken verificationToken = userService.generateNewVerificationToken(oldToken);
        User user = verificationToken.getUser();
        resendVerificationTokenMail(user,applicationUrl(request), verificationToken);
        return "Verification email sent";

    }

    @PostMapping("resetPassword")
    public String resetPassword(@RequestBody PasswordModel passwordModel, HttpServletRequest request)
    {
        User user = userService.findUserByEmail(passwordModel.getEmail());
        String url = "";
        if (user != null)
        {
            String token = UUID.randomUUID().toString();
            userService.createPasswordResetToken(user, token);
            url = passwordResetTokenMail(user,applicationUrl(request),token);
        }
        else{
            return "User not found";
        }
        return url;

    }

    @PostMapping("/savePassword")
    public String savePassword(@RequestParam("token") String token, @RequestBody PasswordModel passwordModel)
    {
        String result = userService.validatePasswordResetToken(token);

        if( !(result.equalsIgnoreCase("valid")))
        {
            return "Invalid Token";
        }
        Optional<User> user = userService.getUserByPasswordResetToken(token);
        if (user.isPresent())
        {
            userService.changePassword(user.get(), passwordModel.getNewPassword());
            return "Password reset successfully";
        }
        else
            return "Invalid token";

    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestBody PasswordModel passwordModel)
    {
        User user = userService.findUserByEmail(passwordModel.getEmail());
        if(!(userService.checkOldPassword(user, passwordModel.getOldPassword())))
        {
            return "invalid old password";
        }
        userService.changePassword(user, passwordModel.getNewPassword());
        return "password changed successfully";
    }
    {

    }

    private String passwordResetTokenMail(User user, String applicationUrl, String token) {
        String url = applicationUrl + "/savePassword?token=" + token ;
        logger.info("Click below url to reset your password {}",url);
        return url;
    }

    private void resendVerificationTokenMail(User user, String applicationUrl, VerificationToken verificationToken) {
        String url = applicationUrl + "/verifyRegistration?token=" + verificationToken.getToken() ;
        logger.info("Click below url to activate your account {}",url);
    }

    private String applicationUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath();
    }
}
