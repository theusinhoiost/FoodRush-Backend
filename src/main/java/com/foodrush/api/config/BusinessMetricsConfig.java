package com.foodrush.api.config;

import com.foodrush.api.enums.OrderStatus;
import com.foodrush.api.enums.RoleType;
import com.foodrush.api.repository.OrderRepository;
import com.foodrush.api.repository.RestaurantRepository;
import com.foodrush.api.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessMetricsConfig {

    @Bean
    public MeterBinder foodRushBusinessMetrics(
            UserRepository userRepository,
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository
    ) {
        return registry -> {
            // 1. Users grouped by Role (CLIENT, RESTAURANT, COURIER, ADMIN)
            for (RoleType role : RoleType.values()) {
                Gauge.builder("foodrush_users_by_role", userRepository, repo -> repo.countByRole(role))
                        .tag("role", role.name())
                        .description("Total number of registered users by role")
                        .register(registry);
            }

            // 2. Users grouped by Active status (ACTIVE vs INACTIVE)
            Gauge.builder("foodrush_users_by_status", userRepository, repo -> repo.countByActive(true))
                    .tag("status", "ACTIVE")
                    .description("Total number of active/inactive users")
                    .register(registry);

            Gauge.builder("foodrush_users_by_status", userRepository, repo -> repo.countByActive(false))
                    .tag("status", "INACTIVE")
                    .description("Total number of active/inactive users")
                    .register(registry);

            // 3. Orders grouped by OrderStatus (CREATED, PAID, PREPARING, READY, IN_ROUTE, DELIVERED, CANCELED)
            for (OrderStatus status : OrderStatus.values()) {
                Gauge.builder("foodrush_orders_by_status", orderRepository, repo -> repo.countByStatus(status))
                        .tag("status", status.name())
                        .description("Total number of orders by lifecycle status")
                        .register(registry);
            }

            // 4. Restaurants grouped by Operational Status (OPEN vs CLOSED)
            Gauge.builder("foodrush_restaurants_by_status", restaurantRepository, repo -> repo.countByIsOpen(true))
                    .tag("status", "OPEN")
                    .description("Total number of restaurants open or closed")
                    .register(registry);

            Gauge.builder("foodrush_restaurants_by_status", restaurantRepository, repo -> repo.countByIsOpen(false))
                    .tag("status", "CLOSED")
                    .description("Total number of restaurants open or closed")
                    .register(registry);
        };
    }
}

