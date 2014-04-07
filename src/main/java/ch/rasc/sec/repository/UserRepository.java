package ch.rasc.sec.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import ch.rasc.sec.entity.User;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {

	User findByUserName(String userName);

}