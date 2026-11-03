package com.foodrush.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.foodrush.api.dto.request.OrderCreateRequestDTO;
import com.foodrush.api.dto.request.OrderItemComplementRequestDTO;
import com.foodrush.api.dto.request.OrderItemRequestDTO;
import com.foodrush.api.dto.response.OrderResponseDTO;
import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.event.OrderStatusChangedEvent;
import com.foodrush.api.exception.BusinessException;
import com.foodrush.api.model.*;
import com.foodrush.api.producer.OrderEventProducer;
import com.foodrush.api.repository.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RestaurantRepository restaurantRepository;

  @Mock
  private MenuItemRepository menuItemRepository;

  @Mock
  private MenuItemSizeRepository menuItemSizeRepository;

  @Mock
  private ComplementItemRepository complementItemRepository;

  @Mock
  private OrderEventProducer orderEventProducer;

  @InjectMocks
  private OrderService orderService;

  private User customer;
  private Restaurant restaurant;
  private MenuItem menuItem;
  private MenuItemSize menuItemSize;
  private ComplementItem complementItem;

  @BeforeEach
  void setUp() {
    Address customerAddress = new Address();
    customerAddress.setId(UUID.randomUUID());
    customerAddress.setStreet("Av Paulista");
    customerAddress.setNumber("1000");
    customerAddress.setNeighborhood("Bela Vista");
    customerAddress.setCity("São Paulo");
    customerAddress.setState("SP");
    customerAddress.setCep("01310100");

    customer = new User();
    customer.setId(UUID.randomUUID());
    customer.setName("Cliente Teste");
    customer.setEmail("cliente@foodrush.com");
    customer.setActive(true);
    customer.setRole(RoleType.CLIENT);
    customer.setAddress(customerAddress);

    restaurant = new Restaurant();
    restaurant.setId(UUID.randomUUID());
    restaurant.setTradeName("Pizzaria Rush");
    restaurant.setIsOpen(true);
    restaurant.setBaseDeliveryFee(new BigDecimal("8.00"));

    menuItem = MenuItem.builder()
        .id(UUID.randomUUID())
        .restaurant(restaurant)
        .name("Pizza Calabresa")
        .basePrice(new BigDecimal("45.00"))
        .available(true)
        .build();

    menuItemSize = MenuItemSize.builder()
        .id(UUID.randomUUID())
        .menuItem(menuItem)
        .name("Grande")
        .price(new BigDecimal("55.00"))
        .stockQuantity(10)
        .available(true)
        .build();

    complementItem = ComplementItem.builder()
        .id(UUID.randomUUID())
        .name("Borda Recheada de Catupiry")
        .price(new BigDecimal("10.00"))
        .available(true)
        .build();
  }

  @Test
  @DisplayName("Deve criar pedido com sucesso calculando subtotal, complementos, estoque e frete")
  void shouldCreateOrderSuccessfully() {
    OrderItemComplementRequestDTO compDTO = new OrderItemComplementRequestDTO(complementItem.getId(), 1);
    OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(menuItem.getId(), menuItemSize.getId(), 2, List.of(compDTO));
    OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
        customer.getId(),
        restaurant.getId(),
        null,
        "idem-key-123",
        List.of(itemDTO)
    );

    when(orderRepository.findByIdempotencyKey("idem-key-123")).thenReturn(Optional.empty());
    when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
    when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
    when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
    when(menuItemSizeRepository.findByIdWithPessimisticLock(menuItemSize.getId())).thenReturn(Optional.of(menuItemSize));
    when(complementItemRepository.findById(complementItem.getId())).thenReturn(Optional.of(complementItem));

    when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
      Order saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID());
      saved.setCreatedAt(OffsetDateTime.now());
      saved.setUpdatedAt(OffsetDateTime.now());
      return saved;
    });

    OrderResponseDTO response = orderService.create(dto);

    assertThat(response).isNotNull();
    assertThat(response.id()).isNotNull();
    assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    assertThat(response.deliveryFee()).isEqualByComparingTo("8.00");
    // (55.00 [size] + 10.00 [complement]) * 2 = 130.00 subtotal
    assertThat(response.subtotal()).isEqualByComparingTo("130.00");
    // 130.00 + 8.00 = 138.00 total
    assertThat(response.total()).isEqualByComparingTo("138.00");
    assertThat(response.deliveryAddress()).contains("Av Paulista, 1000");

    // Valida que o estoque foi decrementado de 10 para 8
    assertThat(menuItemSize.getStockQuantity()).isEqualTo(8);
    verify(menuItemSizeRepository).save(menuItemSize);
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  @DisplayName("Deve retornar pedido existente quando chave de idempotência já existir")
  void shouldReturnExistingOrderWhenIdempotencyKeyMatches() {
    Order existingOrder = Order.builder()
        .id(UUID.randomUUID())
        .customer(customer)
        .restaurant(restaurant)
        .status(OrderStatus.CREATED)
        .subtotal(new BigDecimal("50.00"))
        .deliveryFee(new BigDecimal("8.00"))
        .total(new BigDecimal("58.00"))
        .deliveryAddress("Rua Teste, 1")
        .idempotencyKey("idem-existing")
        .items(new ArrayList<>())
        .build();

    when(orderRepository.findByIdempotencyKey("idem-existing")).thenReturn(Optional.of(existingOrder));

    OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
        customer.getId(),
        restaurant.getId(),
        null,
        "idem-existing",
        List.of()
    );

    OrderResponseDTO result = orderService.create(dto);

    assertThat(result.id()).isEqualTo(existingOrder.getId());
    verify(userRepository, never()).findById(any());
    verify(orderRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar exceção quando restaurante estiver fechado")
  void shouldThrowWhenRestaurantIsClosed() {
    restaurant.setIsOpen(false);

    OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
        customer.getId(),
        restaurant.getId(),
        "Rua Teste, 1",
        null,
        List.of(new OrderItemRequestDTO(menuItem.getId(), null, 1, null))
    );

    when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
    when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));

    assertThatThrownBy(() -> orderService.create(dto))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Restaurant is currently closed");
  }

  @Test
  @DisplayName("Deve lançar exceção quando estoque do tamanho selecionado for insuficiente")
  void shouldThrowWhenStockIsInsufficient() {
    menuItemSize.setStockQuantity(1);

    OrderItemRequestDTO itemDTO = new OrderItemRequestDTO(menuItem.getId(), menuItemSize.getId(), 3, null);
    OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
        customer.getId(),
        restaurant.getId(),
        "Rua Teste, 1",
        null,
        List.of(itemDTO)
    );

    when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
    when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
    when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
    when(menuItemSizeRepository.findByIdWithPessimisticLock(menuItemSize.getId())).thenReturn(Optional.of(menuItemSize));

    assertThatThrownBy(() -> orderService.create(dto))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Insufficient stock");
  }

  @Test
  @DisplayName("Deve atualizar status e publicar evento no RabbitMQ em transição válida")
  void shouldUpdateStatusAndPublishEvent() {
    UUID orderId = UUID.randomUUID();
    Order order = Order.builder()
        .id(orderId)
        .customer(customer)
        .restaurant(restaurant)
        .status(OrderStatus.PAID)
        .subtotal(new BigDecimal("40.00"))
        .deliveryFee(BigDecimal.ZERO)
        .total(new BigDecimal("40.00"))
        .deliveryAddress("Rua A, 10")
        .items(new ArrayList<>())
        .build();

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenReturn(order);

    OrderResponseDTO response = orderService.updateStatus(orderId, OrderStatus.PREPARING);

    assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
    verify(orderEventProducer).publishStatusChanged(any(OrderStatusChangedEvent.class));
    verify(orderRepository).save(order);
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar transição de status inválida")
  void shouldThrowOnInvalidStatusTransition() {
    UUID orderId = UUID.randomUUID();
    Order order = Order.builder()
        .id(orderId)
        .customer(customer)
        .restaurant(restaurant)
        .status(OrderStatus.CREATED)
        .build();

    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    // CREATED não pode ir direto para DELIVERED
    assertThatThrownBy(() -> orderService.updateStatus(orderId, OrderStatus.DELIVERED))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Invalid status transition");

    verify(orderEventProducer, never()).publishStatusChanged(any());
  }
}
