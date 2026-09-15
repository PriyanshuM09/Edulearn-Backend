package com.edulearn.auth.service;

import com.edulearn.auth.dto.LoginResponse;
import com.edulearn.auth.dto.RegisterRequest;
import com.edulearn.auth.dto.UpdateProfileRequest;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_SUSPENDED = "SUSPENDED";

    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String RESET_TOKEN_PREFIX = "reset_token:";
    private static final String RESET_EMAIL_PREFIX = "reset_email:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.reset.token.expiry.minutes}")
    private int resetTokenExpiryMinutes;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${google.client.id}")
    private String googleClientId;

    // ── JWT Helpers ───────────────────────────────────────────────

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Auth Methods ──────────────────────────────────────────────

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email already registered: " + request.getEmail());
        }

        if (ROLE_ADMIN.equalsIgnoreCase(request.getRole())) {
            throw new RuntimeException("Admin registration is not allowed.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().toUpperCase());
        user.setProvider(PROVIDER_LOCAL);
        
        // New users need email verification
        user.setEmailVerified(false);
        user.setVerificationCode(generateOtp());

        // Instructors require admin approval
        user.setApproved(!ROLE_INSTRUCTOR.equalsIgnoreCase(user.getRole()));

        User saved = userRepository.save(user);

        // Send verification email
        sendVerificationEmail(saved);

        return saved;
    }

    private String generateOtp() {
        return String.valueOf(100000 + new java.util.Random().nextInt(900000));
    }

    @Override
    public LoginResponse login(String email, String password) {
        log.info("Login attempt: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + email));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getRole().equals(ROLE_SUSPENDED)) {
            throw new RuntimeException(
                    "Account suspended. Please contact admin.");
        }

        if (ROLE_INSTRUCTOR.equals(user.getRole()) && !user.isApproved()) {
            throw new RuntimeException(
                    "Instructor account pending admin approval. Please wait for verification.");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email via OTP.");
        }

        String token = generateToken(user);

        String refreshToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + jwtExpiration * 7))
                .signWith(getSigningKey())
                .compact();

        // Send login alert email after successful login
        sendLoginAlertEmail(user);

        return new LoginResponse(token, refreshToken, user.getRole(),
                user.getUserId(), user.getFullName(), user.getEmail(),
                user.getProfilePicUrl(), user.getBio(), user.getMobile());
    }

    protected GoogleIdTokenVerifier getGoogleVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId.trim()))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse googleLogin(String idTokenString) {
        log.info("Google login attempt with ID: [{}]", googleClientId.trim());
        log.info("Token received (length: {})", idTokenString != null ? idTokenString.length() : 0);

        GoogleIdTokenVerifier verifier = getGoogleVerifier();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.error("Google token verification returned null. Check if the Client ID matches exactly.");
                throw new RuntimeException("Invalid Google ID token");
            }

            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                log.info("Creating new user for Google login: {}", email);
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name != null ? name : email.split("@")[0]);
                newUser.setProfilePicUrl(pictureUrl);
                newUser.setProvider(PROVIDER_GOOGLE);
                newUser.setRole(ROLE_STUDENT);
                newUser.setApproved(true);
                newUser.setPasswordHash(""); // No password for Google users
                return userRepository.save(newUser);
            });

            if (ROLE_SUSPENDED.equals(user.getRole())) {
                throw new RuntimeException("Account suspended. Please contact admin.");
            }

            String token = generateToken(user);

            String refreshToken = Jwts.builder()
                    .subject(user.getEmail())
                    .claim("type", "refresh")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration * 7))
                    .signWith(getSigningKey())
                    .compact();

            return new LoginResponse(token, refreshToken, user.getRole(),
                    user.getUserId(), user.getFullName(), user.getEmail(),
                    user.getProfilePicUrl(), user.getBio(), user.getMobile());

        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage());
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        log.info("Logout called");
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();
            
            if (diff > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "logged_out", diff, TimeUnit.MILLISECONDS);
                log.info("Token blacklisted in Redis");
            }
        } catch (Exception e) {
            log.error("Redis logout failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // First check Redis blacklist
            Boolean isBlacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("Attempt to use blacklisted token");
                return false;
            }

            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String refreshToken(String token) {
        Claims claims = extractClaims(token);
        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return generateToken(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + email));
    }

    @Override
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + userId));
    }

    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        log.info("Verifying email: {} with OTP", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(otp)) {
            throw new RuntimeException("Invalid verification code");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);

        // After successful verification, send the actual welcome email
        sendWelcomeEmail(user);
        log.info("Email verified successfully for: {}", email);
    }

    @Override
    @Transactional
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(int userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getBio() != null)
            user.setBio(request.getBio());
        if (request.getProfilePicUrl() != null)
            user.setProfilePicUrl(request.getProfilePicUrl());
        if (request.getMobile() != null)
            user.setMobile(request.getMobile());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(int userId) {
        userRepository.deleteById(userId);
    }

    // ── Forgot / Reset Password ───────────────────────────────────

    @Override
    @Transactional
    public void forgotPassword(String email) {
        log.info("Forgot password request for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "No account found with email: " + email));

        try {
            // Delete any old reset token for this user
            String oldToken = redisTemplate.opsForValue().get(RESET_EMAIL_PREFIX + email);
            if (oldToken != null) {
                redisTemplate.delete(RESET_TOKEN_PREFIX + oldToken);
                redisTemplate.delete(RESET_EMAIL_PREFIX + email);
            }

            String token = UUID.randomUUID().toString();

            // Store in Redis
            redisTemplate.opsForValue().set(RESET_TOKEN_PREFIX + token, String.valueOf(user.getUserId()), 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(RESET_EMAIL_PREFIX + email, token, 30, TimeUnit.MINUTES);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            sendResetEmail(user.getEmail(), user.getFullName(), resetLink);

            log.info("Reset token stored in Redis and email sent to: {}", email);
        } catch (Exception e) {
            log.error("Redis failed in forgotPassword: {}", e.getMessage());
            throw new RuntimeException("Failed to process forgot password. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Reset password attempt with token");

        try {
            String userIdStr = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + token);
            if (userIdStr == null) {
                throw new RuntimeException("Invalid or expired reset token");
            }

            int userId = Integer.parseInt(userIdStr);
            User user = getUserById(userId);
            
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Cleanup Redis
            redisTemplate.delete(RESET_TOKEN_PREFIX + token);
            redisTemplate.delete(RESET_EMAIL_PREFIX + user.getEmail());

            log.info("Password reset successfully for userId: {}", userId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis failed in resetPassword: {}", e.getMessage());
            throw new RuntimeException("Failed to reset password. Please try again later.");
        }
    }

    // ── Admin Methods ─────────────────────────────────────────────

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByRole(String role) {
        return userRepository.findAllByRole(role.toUpperCase());
    }

    @Override
    @Transactional
    public User suspendUser(int userId) {
        User user = getUserById(userId);
        user.setRole(ROLE_SUSPENDED);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User approveInstructor(int userId) {
        log.info("Approving instructor with ID: {}", userId);
        User user = getUserById(userId);
        if (!ROLE_INSTRUCTOR.equals(user.getRole())) {
            throw new RuntimeException("User is not an instructor");
        }
        user.setApproved(true);
        return userRepository.save(user);
    }

    @Override
    public List<User> getPendingInstructors() {
        return userRepository.findByRoleAndIsApproved(ROLE_INSTRUCTOR, false);
    }

    @Override
    public void testEmail(String email) {
        log.info("Sending test email to: {}", email);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("EduLearn — SMTP Test Connection");
            message.setText("This is a test email from EduLearn to verify SMTP configuration.\n" +
                    "If you received this, your email service is working correctly!");
            mailSender.send(message);
            log.info("Test email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("SMTP Test Failed for {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("SMTP Test Failed: " + e.getMessage());
        }
    }

    @Override
    public List<User> getUsersByIds(List<Integer> userIds) {
        log.info("Batch fetching user profiles for {} IDs", userIds.size());
        return userRepository.findAllById(userIds);
    }

    // ── Email Methods ─────────────────────────────────────────────

    private void sendWelcomeEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Welcome to EduLearn! 🎓");
            message.setText(
                    "Hi " + user.getFullName() + ",\n\n" +
                            "Welcome to EduLearn — Learn Anytime. Grow Everywhere.\n\n" +
                            "Your account has been successfully created.\n\n" +
                            "Account Details:\n" +
                            "─────────────────────────────\n" +
                            "Name  : " + user.getFullName() + "\n" +
                            "Email : " + user.getEmail() + "\n" +
                            "Role  : " + user.getRole() + "\n" +
                            "─────────────────────────────\n\n" +
                            getRoleMessage(user.getRole()) + "\n\n" +
                            "Get started here: " + frontendUrl + "\n\n" +
                            "If you did not create this account,\n" +
                            "please contact us immediately.\n\n" +
                            "Happy Learning!\n" +
                            "Team EduLearn");
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    private void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("EduLearn — Verify Your Email");
            message.setText(
                    "Hi " + user.getFullName() + ",\n\n" +
                            "Thank you for joining EduLearn!\n\n" +
                            "Please use the following 6-digit OTP to verify your email address:\n\n" +
                            "Verification Code: " + user.getVerificationCode() + "\n\n" +
                            "This code is required to activate your account.\n" +
                            "Do not share this OTP with anyone.\n\n" +
                            "Happy Learning!\n" +
                            "Team EduLearn");
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    private void sendLoginAlertEmail(User user) {
        try {
            String loginTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a"));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("New Login to Your EduLearn Account");
            message.setText(
                    "Hi " + user.getFullName() + ",\n\n" +
                            "We noticed a new login to your EduLearn account.\n\n" +
                            "Login Details:\n" +
                            "─────────────────────────────\n" +
                            "Email : " + user.getEmail() + "\n" +
                            "Role  : " + user.getRole() + "\n" +
                            "Time  : " + loginTime + "\n" +
                            "─────────────────────────────\n\n" +
                            "If this was you, no action is needed.\n\n" +
                            "If you did NOT log in, please reset your\n" +
                            "password immediately here:\n" +
                            frontendUrl + "/forgot-password\n\n" +
                            "Stay safe!\n" +
                            "Team EduLearn");
            mailSender.send(message);
            log.info("Login alert email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send login alert email to {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    private void sendResetEmail(String to, String name,
            String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject("EduLearn — Reset Your Password");
            message.setText(
                    "Hi " + name + ",\n\n" +
                            "We received a request to reset your EduLearn password.\n\n" +
                            "Click the link below to reset your password:\n" +
                            resetLink + "\n\n" +
                            "─────────────────────────────\n" +
                            "This link expires in " + resetTokenExpiryMinutes
                            + " minutes.\n" +
                            "─────────────────────────────\n\n" +
                            "If you did not request this,\n" +
                            "please ignore this email.\n" +
                            "Your password will not change.\n\n" +
                            "Team EduLearn");
            mailSender.send(message);
            log.info("Reset email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}. Error: {}",
                    to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email. Please check configuration.");
        }
    }

    private String getRoleMessage(String role) {
        return switch (role) {
            case ROLE_STUDENT ->
                "As a Student you can:\n" +
                        "→ Browse and enroll in courses\n" +
                        "→ Watch video lessons\n" +
                        "→ Take quizzes\n" +
                        "→ Track your progress\n" +
                        "→ Earn certificates";
            case ROLE_INSTRUCTOR ->
                "As an Instructor you can:\n" +
                        "→ Create and publish courses\n" +
                        "→ Add lessons and quizzes\n" +
                        "→ Track student progress\n" +
                        "→ Moderate course forums";
            case ROLE_ADMIN ->
                "As an Admin you can:\n" +
                        "→ Manage all users\n" +
                        "→ Approve or reject courses\n" +
                        "→ View platform analytics\n" +
                        "→ Manage payments and subscriptions";
            default -> "Explore EduLearn and start learning today!";
        };
    }
}
