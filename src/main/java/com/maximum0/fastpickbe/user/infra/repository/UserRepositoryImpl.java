package com.maximum0.fastpickbe.user.infra.repository;

import com.maximum0.fastpickbe.user.domain.model.User;
import com.maximum0.fastpickbe.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public List<User> saveAll(List<User> users) {
        return userJpaRepository.saveAll(users);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User saveAndFlush(User user) {
        return userJpaRepository.saveAndFlush(user);
    }

    @Override
    public void deleteAllInBatch() {
        userJpaRepository.deleteAllInBatch();
    }

}
