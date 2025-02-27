package com.festago.auth.application;

import com.festago.admin.domain.Admin;
import com.festago.admin.repository.AdminRepository;
import com.festago.auth.domain.AuthPayload;
import com.festago.auth.domain.Role;
import com.festago.auth.dto.AdminLoginRequest;
import com.festago.auth.dto.AdminSignupRequest;
import com.festago.auth.dto.AdminSignupResponse;
import com.festago.common.exception.BadRequestException;
import com.festago.common.exception.ErrorCode;
import com.festago.common.exception.ForbiddenException;
import com.festago.common.exception.UnauthorizedException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminAuthService {

    private static final String ROOT_ADMIN = "admin";

    private final AuthProvider authProvider;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public String login(AdminLoginRequest request) {
        Admin admin = findAdmin(request);
        validatePassword(admin.getPassword(), request.password());
        AuthPayload authPayload = getAuthPayload(admin);
        return authProvider.provide(authPayload);
    }

    private Admin findAdmin(AdminLoginRequest request) {
        return adminRepository.findByUsername(request.username())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.INCORRECT_PASSWORD_OR_ACCOUNT));
    }

    private void validatePassword(String password, String comparePassword) {
        if (!Objects.equals(password, comparePassword)) {
            throw new UnauthorizedException(ErrorCode.INCORRECT_PASSWORD_OR_ACCOUNT);
        }
    }

    private AuthPayload getAuthPayload(Admin admin) {
        return new AuthPayload(admin.getId(), Role.ADMIN);
    }

    public void initializeRootAdmin(String password) {
        adminRepository.findByUsername(ROOT_ADMIN).ifPresentOrElse(admin -> {
            throw new BadRequestException(ErrorCode.DUPLICATE_ACCOUNT_USERNAME);
        }, () -> adminRepository.save(new Admin(ROOT_ADMIN, password)));
    }

    public AdminSignupResponse signup(Long adminId, AdminSignupRequest request) {
        validateRootAdmin(adminId);
        String username = request.username();
        String password = request.password();
        validateExistsUsername(username);
        Admin admin = adminRepository.save(new Admin(username, password));
        return new AdminSignupResponse(admin.getUsername());
    }

    private void validateExistsUsername(String username) {
        if (adminRepository.existsByUsername(username)) {
            throw new BadRequestException(ErrorCode.DUPLICATE_ACCOUNT_USERNAME);
        }
    }

    private void validateRootAdmin(Long adminId) {
        adminRepository.findById(adminId)
            .map(Admin::getUsername)
            .filter(username -> Objects.equals(username, ROOT_ADMIN))
            .ifPresentOrElse(username -> {
            }, () -> {
                throw new ForbiddenException(ErrorCode.NOT_ENOUGH_PERMISSION);
            });
    }
}
