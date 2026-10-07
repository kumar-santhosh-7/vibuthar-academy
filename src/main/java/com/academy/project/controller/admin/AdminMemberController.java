package com.academy.project.controller.admin;

import com.academy.project.dto.intrest.InterestResponse;
import com.academy.project.dto.member.MemberResponse;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.enums.EmailStatus;
import com.academy.project.service.intrest.InterestService;
import com.academy.project.service.member.MemberService;
import com.academy.project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
public class AdminMemberController {

    private final InterestService interestService;
    private final MemberService memberService;
    private final UserService userService;

    @GetMapping("/interests")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<PagedResponse<InterestResponse>>> listInterestedCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String courseOfInterest,
            @RequestParam(required = false) EmailStatus emailStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<InterestResponse> response = interestService.listInterests(
                search, courseOfInterest, emailStatus, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok("Interested candidates fetched successfully", response));
    }

    @GetMapping("/members/subscribed")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<PagedResponse<MemberResponse>>> listSubscribedMembers(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<MemberResponse> response = memberService.listSubscribedMembers(
                courseId, search, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok("Subscribed members fetched successfully", response));
    }

    @GetMapping("/members/non-subscribed")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<PagedResponse<MemberResponse>>> listNonSubscribedMembers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<MemberResponse> response = memberService.listNonSubscribedMembers(
                search, page, size
        );
        return ResponseEntity.ok(ApiResponse.ok("Non-subscribed members fetched successfully", response));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }
}
