package com.subscriptiontracker.repository;

import com.subscriptiontracker.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    Page<Service> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT s FROM Service s WHERE s.user.id = :userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Service> findByUserIdAndNameContaining(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    Optional<Service> findByUserIdAndName(Long userId, String name);

    Optional<Service> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    @Query("SELECT DISTINCT s.category FROM Service s WHERE s.user.id = :userId AND s.category IS NOT NULL ORDER BY s.category")
    List<String> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(sub) FROM Subscription sub WHERE sub.service.id = :serviceId")
    long countSubscriptionsByServiceId(@Param("serviceId") Long serviceId);

    List<Service> findByUserIdOrderByNameAsc(Long userId);
}
