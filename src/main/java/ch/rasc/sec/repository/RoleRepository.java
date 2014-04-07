package ch.rasc.sec.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import ch.rasc.sec.entity.Role;

public interface RoleRepository extends PagingAndSortingRepository<Role, Long> {

	Role findByName(String roleName);

}