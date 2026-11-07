package com.township.service;

import com.township.dto.*;
import com.township.entity.*;
import com.township.repository.*;
import com.township.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TowerRepository towerRepository;
    private final FlatRepository flatRepository;
    private final FlatMemberRepository flatMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            TowerRepository towerRepository,
            FlatRepository flatRepository,
            FlatMemberRepository flatMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.towerRepository = towerRepository;
        this.flatRepository = flatRepository;
        this.flatMemberRepository = flatMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByIdentifier(request.identifier().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email/phone or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email/phone or password");
        }

        if (!user.isActive()) {
            throw new BadCredentialsException("User account is inactive. Please contact the Help Desk.");
        }

        String token = tokenProvider.generateToken(user);
        UserDto userDto = convertToUserDto(user);

        return new AuthResponse(token, "Bearer", tokenProvider.getExpirationMs(), userDto);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        String cleanPhone = request.phone().trim();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email is already registered: " + cleanEmail);
        }

        if (userRepository.existsByPhone(cleanPhone)) {
            throw new IllegalArgumentException("Phone number is already registered: " + cleanPhone);
        }

        // SECURITY ENFORCEMENT (Phase 5): Public registration MUST create RESIDENT accounts only.
        // Privileged roles (such as HELP_DESK) cannot be provisioned through public registration.
        Role role = Role.RESIDENT;
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.fullName().trim(),
                cleanEmail,
                cleanPhone,
                hashedPassword,
                role
        );
        user = userRepository.save(user);

        // Optional flat association during registration
        if (request.towerId() != null && !request.towerId().isBlank() &&
            request.flatNumber() != null && !request.flatNumber().isBlank()) {
            
            String tId = request.towerId().trim();
            String fNum = request.flatNumber().trim();

            Tower tower = towerRepository.findById(tId).orElse(null);
            if (tower != null) {
                Flat flat = flatRepository.findByTowerIdAndFlatNumber(tId, fNum)
                        .orElseGet(() -> {
                            int floor = 1;
                            try {
                                if (fNum.length() >= 3) {
                                    floor = Integer.parseInt(fNum.substring(0, fNum.length() - 2));
                                }
                            } catch (NumberFormatException ignored) {}
                            return flatRepository.save(new Flat(tower, fNum, floor, 1150.0, "OWNER_OCCUPIED"));
                        });

                flatMemberRepository.save(new FlatMember(flat, user, "PRIMARY_OWNER", true));
            }
        }

        String token = tokenProvider.generateToken(user);
        UserDto userDto = convertToUserDto(user);

        return new AuthResponse(token, "Bearer", tokenProvider.getExpirationMs(), userDto);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return convertToUserDto(user);
    }

    private UserDto convertToUserDto(User user) {
        List<FlatMember> memberships = flatMemberRepository.findByUserId(user.getId());
        List<FlatSummaryDto> flatDtos = memberships.stream()
                .map(m -> new FlatSummaryDto(
                        m.getFlat().getId(),
                        m.getFlat().getTower().getId(),
                        m.getFlat().getTower().getName(),
                        m.getFlat().getFlatNumber(),
                        m.getFlat().getFloorNumber(),
                        m.getFlat().getAreaSqFt(),
                        m.getFlat().getOccupancyStatus(),
                        m.getMemberType()
                ))
                .toList();

        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                flatDtos
        );
    }
}
