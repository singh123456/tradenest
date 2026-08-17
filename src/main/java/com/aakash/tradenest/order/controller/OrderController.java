package com.aakash.tradenest.order.controller;


import com.aakash.tradenest.order.dto.OrderResponse;
import com.aakash.tradenest.order.dto.PlaceOrderRequest;
import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.mapper.OrderMapper;
import com.aakash.tradenest.order.service.OrderService;
import com.aakash.tradenest.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody PlaceOrderRequest request
            ){
        Long userId = userService.getCurrentUserId(principal.getUsername());

       Order order = orderService.placeOrder(userId,
                request.symbol(),
                request.side(),
                request.orderType(),
                request.quantity(),
                request.price()
        );
       OrderResponse response = orderMapper.toResponse(order);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetails principal
    ){
        Long userId = userService.getCurrentUserId(principal.getUsername());
        List<Order> orders = orderService.getOrdersByUser(userId);

        List<OrderResponse> response = orders.stream()
                .map(orderMapper::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ){
        Long userId = userService.getCurrentUserId(principal.getUsername());
        Order order = orderService.getOrderById(userId,id);

        OrderResponse response = orderMapper.toResponse(order);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ){
        Long userId = userService.getCurrentUserId(principal.getUsername());
        Order order = orderService.cancelOrder(userId,id);

        OrderResponse response = orderMapper.toResponse(order);
        return ResponseEntity.ok(response);
    }
}
