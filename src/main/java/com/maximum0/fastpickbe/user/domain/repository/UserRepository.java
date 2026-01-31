package com.maximum0.fastpickbe.user.domain.repository;

import com.maximum0.fastpickbe.user.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * 사용자 정보를 저장한다
     *
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티
     */
    User save(User user);

    /**
     * 사용자 목록을 일괄 저장한다
     *
     * @param users 저장할 사용자 엔티티 목록
     * @return 저장된 사용자 엔티티 목록
     */
    List<User> saveAll(List<User> users);

    /**
     * 식별자를 통해 사용자 정보를 조회한다
     *
     * @param id 사용자 식별자
     * @return 사용자 엔티티의 Optional 객체
     */
    Optional<User> findById(Long id);

    /**
     * 이메일을 통해 사용자 정보를 조회한다
     *
     * @param email 사용자 이메일
     * @return 사용자 엔티티의 Optional 객체
     */
    Optional<User> findByEmail(String email);

    /**
     * 해당 이메일을 사용하는 사용자가 존재하는지 확인한다
     *
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 정보를 저장하고 즉시 DB에 반영(Flush)한다
     *
     * @param user 저장 및 반영할 사용자 엔티티
     * @return 저장된 사용자 엔티티
     */
    User saveAndFlush(User user);

    /**
     * 모든 사용자 데이터를 일괄 삭제한다 (테스트 환경 전용)
     */
    void deleteAllInBatch();

}
