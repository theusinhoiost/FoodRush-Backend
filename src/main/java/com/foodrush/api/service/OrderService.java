package com.foodrush.api.service;

import com.foodrush.api.dto.request.OrderCreateRequestDTO;
import com.foodrush.api.dto.request.OrderItemComplementRequestDTO;
import com.foodrush.api.dto.request.OrderItemRequestDTO;
import com.foodrush.api.dto.response.OrderItemComplementResponseDTO;
import com.foodrush.api.dto.response.OrderItemResponseDTO;
import com.foodrush.api.dto.response.OrderResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.event.OrderStatusChangedEvent;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.*;
import com.foodrush.api.producer.OrderEventProducer;
import com.foodrush.api.repository.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;
  private final MenuItemSizeRepository menuItemSizeRepository;
  private final ComplementItemRepository complementItemRepository;
  private final OrderEventProducer orderEventProducer;
  private final DeliveryCalculationService deliveryCalculationService;

  @Transactional
  public OrderResponseDTO create(OrderCreateRequestDTO dto) {
    // 1. Idempotency Check
    if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
      var existingOrder = orderRepository.findByIdempotencyKey(dto.idempotencyKey());
      if (existingOrder.isPresent()) {
        log.info("Idempotent order request detected for key: {}", dto.idempotencyKey());
        return toDTO(existingOrder.get());
      }
    }

    // 2. Validate Customer
    User customer = userRepository.findById(dto.customerId())
        .orElseThrow(() -> new BusinessException("Customer not found with ID: " + dto.customerId()));

    if (!Boolean.TRUE.equals(customer.getActive())) {
      throw new BusinessException("Customer account is inactive and cannot place orders");
    }

    // 3. Validate Restaurant & Operational Status
    Restaurant restaurant = restaurantRepository.findById(dto.restaurantId())
        .orElseThrow(() -> new BusinessException("Restaurant not found with ID: " + dto.restaurantId()));

    if (!Boolean.TRUE.equals(restaurant.getIsOpen())) {
      throw new BusinessException("Cannot place order: Restaurant is currently closed");
    }

    // 4. Resolve Delivery Address & Calculate Delivery Fee
    String destinationAddress = dto.deliveryAddress();
    BigDecimal calculatedDeliveryFee = restaurant.getBaseDeliveryFee() != null
        ? restaurant.getBaseDeliveryFee()
        : BigDecimal.ZERO;

    if (destinationAddress == null || destinationAddress.isBlank()) {
      if (customer.getAddress() != null) {
        Address addr = customer.getAddress();
        destinationAddress = String.format("%s, %s%s, %s, %s - %s (CEP: %s)",
            addr.getStreet(),
            addr.getNumber(),
            addr.getComplement() != null && !addr.getComplement().isBlank() ? " - " + addr.getComplement() : "",
            addr.getNeighborhood(),
            addr.getCity(),
            addr.getState(),
            addr.getCep());

        // Dynamic delivery fee calculation if coordinates are present
        if (addr.getLatitude() != null && addr.getLongitude() != null
            && restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
          var estimate = deliveryCalculationService.calculateEstimate(
              addr.getLatitude(),
              addr.getLongitude(),
              restaurant
          );
          calculatedDeliveryFee = estimate.totalDeliveryFee();
        }
      } else {
        throw new BusinessException("Delivery address is mandatory since customer has no saved default address");
      }
    }

    // 5. Build and validate Order Items
    Order order = Order.builder()
        .customer(customer)
        .restaurant(restaurant)
        .status(OrderStatus.CREATED)
        .deliveryFee(calculatedDeliveryFee)
        .deliveryAddress(destinationAddress)
        .idempotencyKey(dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank() ? dto.idempotencyKey() : null)
        .items(new ArrayList<>())
        .build();

    BigDecimal calculatedSubtotal = BigDecimal.ZERO;

    for (OrderItemRequestDTO itemDTO : dto.items()) {
      if (itemDTO.quantity() == null || itemDTO.quantity() <= 0) {
        throw new BusinessException("Item quantity must be greater than zero");
      }

      MenuItem menuItem = menuItemRepository.findById(itemDTO.menuItemId())
          .orElseThrow(() -> new BusinessException("Menu item not found with ID: " + itemDTO.menuItemId()));

      if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
        throw new BusinessException(String.format("Menu item '%s' does not belong to restaurant '%s'",
            menuItem.getName(), restaurant.getTradeName()));
      }

      if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
        throw new BusinessException("Menu item '" + menuItem.getName() + "' is currently unavailable");
      }

      BigDecimal unitPrice;
      MenuItemSize selectedSize = null;

      if (itemDTO.sizeId() != null) {
        selectedSize = menuItemSizeRepository.findByIdWithPessimisticLock(itemDTO.sizeId())
            .orElseThrow(() -> new BusinessException("Menu item size not found with ID: " + itemDTO.sizeId()));

        if (!selectedSize.getMenuItem().getId().equals(menuItem.getId())) {
          throw new BusinessException("Size does not belong to the specified menu item");
        }

        if (!Boolean.TRUE.equals(selectedSize.getAvailable())) {
          throw new BusinessException("Selected size is currently unavailable");
        }

        if (selectedSize.getStockQuantity() != null && selectedSize.getStockQuantity() < itemDTO.quantity()) {
          throw new BusinessException(String.format("Insufficient stock for size '%s'. Available: %d, Requested: %d",
              selectedSize.getName(), selectedSize.getStockQuantity(), itemDTO.quantity()));
        }

        // Deduct stock safely
        if (selectedSize.getStockQuantity() != null) {
          selectedSize.setStockQuantity(selectedSize.getStockQuantity() - itemDTO.quantity());
          menuItemSizeRepository.save(selectedSize);
        }

        unitPrice = selectedSize.getPrice();
      } else {
        unitPrice = menuItem.getBasePrice();
      }

      // Calculate complements
      BigDecimal complementsSubtotal = BigDecimal.ZERO;
      List<OrderItemComplement> complementEntities = new ArrayList<>();

      if (itemDTO.complements() != null) {
        for (OrderItemComplementRequestDTO compDTO : itemDTO.complements()) {
          ComplementItem complementItem = complementItemRepository.findById(compDTO.complementItemId())
              .orElseThrow(() -> new BusinessException("Complement item not found with ID: " + compDTO.complementItemId()));

          if (!Boolean.TRUE.equals(complementItem.getAvailable())) {
            throw new BusinessException("Complement '" + complementItem.getName() + "' is currently unavailable");
          }

          int compQty = compDTO.quantity() != null && compDTO.quantity() > 0 ? compDTO.quantity() : 1;
          BigDecimal compPrice = complementItem.getPrice() != null ? complementItem.getPrice() : BigDecimal.ZERO;

          complementsSubtotal = complementsSubtotal.add(compPrice.multiply(BigDecimal.valueOf(compQty)));

          OrderItemComplement complement = OrderItemComplement.builder()
              .complementItem(complementItem)
              .name(complementItem.getName())
              .price(compPrice)
              .quantity(compQty)
              .build();

          complementEntities.add(complement);
        }
      }

      BigDecimal singleItemTotal = unitPrice.add(complementsSubtotal);
      BigDecimal lineTotal = singleItemTotal.multiply(BigDecimal.valueOf(itemDTO.quantity()));
      calculatedSubtotal = calculatedSubtotal.add(lineTotal);

      OrderItem orderItem = OrderItem.builder()
          .order(order)
          .menuItem(menuItem)
          .size(selectedSize)
          .quantity(itemDTO.quantity())
          .unitPrice(unitPrice)
          .totalPrice(lineTotal)
          .complements(new ArrayList<>())
          .build();

      for (OrderItemComplement complement : complementEntities) {
        complement.setOrderItem(orderItem);
        orderItem.getComplements().add(complement);
      }

      order.getItems().add(orderItem);
    }

    order.setSubtotal(calculatedSubtotal);
    order.setTotal(calculatedSubtotal.add(order.getDeliveryFee()));

    Order savedOrder = orderRepository.save(order);
    log.info("Order successfully created with ID: {} for Customer: {}", savedOrder.getId(), customer.getId());

    return toDTO(savedOrder);
  }

  @Transactional(readOnly = true)
  public OrderResponseDTO findById(UUID id) {
    Order order = orderRepository.findByIdWithDetails(id)
        .or(() -> orderRepository.findById(id))
        .orElseThrow(() -> new BusinessException("Order not found with ID: " + id));
    return toDTO(order);
  }

  @Transactional(readOnly = true)
  public List<OrderResponseDTO> findAllByCustomerId(UUID customerId) {
    return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponseDTO> findAllByRestaurantIdAndStatus(UUID restaurantId, OrderStatus status) {
    return orderRepository.findAllByRestaurantIdAndStatus(restaurantId, status)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  @Transactional
  public OrderResponseDTO updateStatus(UUID id, OrderStatus newStatus) {
    Order order = orderRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Order not found with ID: " + id));

    OrderStatus currentStatus = order.getStatus();

    if (currentStatus == newStatus) {
      return toDTO(order);
    }

    validateStatusTransition(currentStatus, newStatus);

    order.setStatus(newStatus);
    Order updatedOrder = orderRepository.save(order);

    log.info("Order {} transitioned from {} to {}", id, currentStatus, newStatus);

    // Publish event to RabbitMQ
    OrderStatusChangedEvent event = new OrderStatusChangedEvent(
        order.getId(),
        order.getCustomer().getId(),
        order.getRestaurant().getId(),
        currentStatus,
        newStatus,
        OffsetDateTime.now()
    );
    orderEventProducer.publishStatusChanged(event);

    return toDTO(updatedOrder);
  }

  private void validateStatusTransition(OrderStatus from, OrderStatus to) {
    if (from == OrderStatus.DELIVERED || from == OrderStatus.CANCELED) {
      throw new BusinessException(String.format("Cannot transition order from finalized state '%s'", from));
    }

    boolean isValid = switch (from) {
      case CREATED -> to == OrderStatus.PAID || to == OrderStatus.CANCELED;
      case PAID -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELED;
      case PREPARING -> to == OrderStatus.READY || to == OrderStatus.CANCELED;
      case READY -> to == OrderStatus.IN_ROUTE || to == OrderStatus.CANCELED;
      case IN_ROUTE -> to == OrderStatus.DELIVERED || to == OrderStatus.CANCELED;
      case DELIVERED, CANCELED -> false;
    };

    if (!isValid) {
      throw new BusinessException(String.format("Invalid status transition from '%s' to '%s'", from, to));
    }
  }

  private OrderResponseDTO toDTO(Order order) {
    List<OrderItemResponseDTO> itemDTOs = order.getItems() != null
        ? order.getItems().stream().map(this::toItemDTO).toList()
        : List.of();

    return new OrderResponseDTO(
        order.getId(),
        order.getCustomer().getId(),
        order.getCustomer().getName(),
        order.getRestaurant().getId(),
        order.getRestaurant().getTradeName(),
        order.getStatus(),
        order.getSubtotal(),
        order.getDeliveryFee(),
        order.getTotal(),
        order.getDeliveryAddress(),
        order.getIdempotencyKey(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        itemDTOs
    );
  }

  private OrderItemResponseDTO toItemDTO(OrderItem item) {
    List<OrderItemComplementResponseDTO> compDTOs = item.getComplements() != null
        ? item.getComplements().stream().map(this::toComplementDTO).toList()
        : List.of();

    return new OrderItemResponseDTO(
        item.getId(),
        item.getMenuItem().getId(),
        item.getMenuItem().getName(),
        item.getSize() != null ? item.getSize().getId() : null,
        item.getSize() != null ? item.getSize().getName() : null,
        item.getQuantity(),
        item.getUnitPrice(),
        item.getTotalPrice(),
        compDTOs
    );
  }

  private OrderItemComplementResponseDTO toComplementDTO(OrderItemComplement comp) {
    return new OrderItemComplementResponseDTO(
        comp.getId(),
        comp.getComplementItem().getId(),
        comp.getName(),
        comp.getPrice(),
        comp.getQuantity()
    );
  }
}
