package com.timefold.user.repository;

import com.timefold.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByEmailIgnoreCase(String email);

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByPublicId(String publicId);

	// JOIN FETCH prevents LazyInitializationException during Spring Security login
	// (open-in-view=false)
	@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
	Optional<User> findByEmailWithRoles(@Param("email") String email);

}