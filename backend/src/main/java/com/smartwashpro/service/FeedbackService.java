package com.smartwashpro.service;

import com.smartwashpro.dto.request.FeedbackRequest;
import com.smartwashpro.dto.response.FeedbackResponse;
import com.smartwashpro.exception.ForbiddenException;
import com.smartwashpro.exception.ResourceNotFoundException;
import com.smartwashpro.model.Customer;
import com.smartwashpro.model.Feedback;
import com.smartwashpro.model.Order;
import com.smartwashpro.model.User;
import com.smartwashpro.model.enums.FeedbackStatus;
import com.smartwashpro.model.enums.Role;
import com.smartwashpro.repository.CustomerRepository;
import com.smartwashpro.repository.FeedbackRepository;
import com.smartwashpro.repository.OrderRepository;
import com.smartwashpro.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, CustomerRepository customerRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.feedbackRepository = feedbackRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getId()));

        Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElse(null);
        }

        Feedback feedback = Feedback.builder()
                .customer(customer)
                .order(order)
                .rating(request.getRating())
                .feedbackText(request.getFeedbackText())
                .status(FeedbackStatus.PUBLISHED)
                .build();

        feedback = feedbackRepository.save(feedback);
        return mapToResponse(feedback);
    }

    public Page<FeedbackResponse> getAllFeedback(Pageable pageable) {
        return feedbackRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<FeedbackResponse> getMyFeedback(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getId()));
        return feedbackRepository.findByCustomerId(customer.getId(), pageable).map(this::mapToResponse);
    }

    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", id));
        return mapToResponse(feedback);
    }

    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request, String userEmail) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
            if (feedback.getCustomer() == null || !feedback.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("You can only modify your own feedback");
            }
        }

        if (request.getRating() >= 1 && request.getRating() <= 5) {
            feedback.setRating(request.getRating());
        }
        if (request.getFeedbackText() != null) {
            feedback.setFeedbackText(request.getFeedbackText());
        }
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId()).orElse(null);
            feedback.setOrder(order);
        }

        feedback = feedbackRepository.save(feedback);
        return mapToResponse(feedback);
    }

    @Transactional
    public void deleteFeedback(Long id, String userEmail) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
            if (feedback.getCustomer() == null || !feedback.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("You can only delete your own feedback");
            }
        }

        feedbackRepository.delete(feedback);
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .customerId(feedback.getCustomer() != null ? feedback.getCustomer().getId() : null)
                .customerName(feedback.getCustomer() != null && feedback.getCustomer().getUser() != null ? feedback.getCustomer().getUser().getFullName() : "Customer")
                .orderId(feedback.getOrder() != null ? feedback.getOrder().getId() : null)
                .rating(feedback.getRating())
                .feedbackText(feedback.getFeedbackText())
                .status(feedback.getStatus() != null ? feedback.getStatus().name() : "PUBLISHED")
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}

