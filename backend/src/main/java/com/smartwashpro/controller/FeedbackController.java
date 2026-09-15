package com.smartwashpro.controller;

import com.smartwashpro.dto.request.FeedbackRequest;
import com.smartwashpro.dto.response.FeedbackResponse;
import com.smartwashpro.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.createFeedback(request, userDetails.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER_ADMIN', 'CUSTOMER')")
    public ResponseEntity<Page<FeedbackResponse>> getAllFeedback(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            return ResponseEntity.ok(feedbackService.getMyFeedback(userDetails.getUsername(), pageable));
        }
        return ResponseEntity.ok(feedbackService.getAllFeedback(pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<FeedbackResponse>> getMyFeedback(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getMyFeedback(userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BRANCH_MANAGER_ADMIN')")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(feedbackService.updateFeedback(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'BRANCH_MANAGER_ADMIN')")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        feedbackService.deleteFeedback(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
