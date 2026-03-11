package com.example.ecommerceProject.handler;

import com.example.ecommerceProject.model.User;
import com.example.ecommerceProject.service.UserService;
import com.example.ecommerceProject.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication
        .SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        // Step 1 — Get user info from Google
        OAuth2User oAuth2User =
                (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        System.out.println("=== GOOGLE OAuth ===");
        System.out.println("Email : " + email);
        System.out.println("Name  : " + name);
        System.out.println("====================");

        // Step 2 — Find or create user via service
        User user = userService.findOrCreateGoogleUser(email, name);

        // Step 3 — Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole());

        // Step 4 — Build redirect URL
        String redirectUrl = "http://localhost:5173"
                + "/auth/callback"
                + "?token=" + token
                + "&name=" + user.getName()
                + "&email=" + user.getEmail()
                + "&role=" + user.getRole()
                + "&userId=" + user.getId();

        System.out.println("Redirect → " + redirectUrl);

        // Step 5 — Redirect to React frontend
        response.sendRedirect(redirectUrl);
    }
}