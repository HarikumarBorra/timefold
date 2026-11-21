package com.timefold.user.repository;

import com.timefold.user.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {

	Optional<Role> findByName(String name);

}