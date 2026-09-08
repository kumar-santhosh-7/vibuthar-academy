package com.academy.project.repository.user;

import com.academy.project.entity.user.User;
import com.academy.project.entity.user.UserRole;
import com.academy.project.util.PhoneUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByUserId(String userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    default Optional<User> findActiveByPhone(String phone) {
        String normalized = PhoneUtils.normalize(phone);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        Optional<User> exact = findByPhone(phone.trim()).filter(u -> u.getDeletedAt() == null);
        if (exact.isPresent()) {
            return exact;
        }

        Optional<User> normalizedMatch = findByPhone(normalized).filter(u -> u.getDeletedAt() == null);
        if (normalizedMatch.isPresent()) {
            return normalizedMatch;
        }

        String withCountryCode = PhoneUtils.toSmsNumber(phone, "91");
        return findByPhone(withCountryCode).filter(u -> u.getDeletedAt() == null);
    }

    default boolean existsByPhoneNormalized(String phone) {
        String normalized = PhoneUtils.normalize(phone);
        if (normalized.isBlank()) {
            return false;
        }
        return existsByPhone(phone.trim()) || existsByPhone(normalized)
                || existsByPhone(PhoneUtils.toSmsNumber(phone, "91"));
    }

    /**
     * Login can happen via email or phone in the same field ("identifier").
     * Excludes soft-deleted accounts.
     */
    default Optional<User> findActiveByIdentifier(String identifier) {
        Optional<User> byEmail = findByEmailIgnoreCase(identifier);
        if (byEmail.isPresent()) {
            return byEmail.filter(u -> u.getDeletedAt() == null);
        }
        return findActiveByPhone(identifier);
    }

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND u.deletedAt IS NULL
              AND u.userId IN :userIds
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR u.phone LIKE CONCAT('%', :search, '%'))
            """)
    Page<User> findMembersByIds(
            @Param("role") UserRole role,
            @Param("userIds") List<String> userIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND u.deletedAt IS NULL
              AND u.userId NOT IN :excludedUserIds
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR u.phone LIKE CONCAT('%', :search, '%'))
            """)
    Page<User> findMembersExcludingIds(
            @Param("role") UserRole role,
            @Param("excludedUserIds") List<String> excludedUserIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND u.deletedAt IS NULL
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR u.phone LIKE CONCAT('%', :search, '%'))
            """)
    Page<User> findMembers(
            @Param("role") UserRole role,
            @Param("search") String search,
            Pageable pageable
    );

    long countByRoleAndDeletedAtIsNull(UserRole role);
}