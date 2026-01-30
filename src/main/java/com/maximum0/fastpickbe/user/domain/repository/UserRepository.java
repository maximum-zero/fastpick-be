package com.maximum0.fastpickbe.user.domain.repository;

import com.maximum0.fastpickbe.user.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    List<User> saveAll(List<User> users);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    User saveAndFlush(User user);
    void deleteAllInBatch();
}