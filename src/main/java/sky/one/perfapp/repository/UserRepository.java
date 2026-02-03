package sky.one.perfapp.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sky.one.perfapp.model.UserEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByName(String name);

    List<UserEntity> findByAgeGreaterThanEqual(Integer age);

    Page<UserEntity> findByAgeBetween(Integer min, Integer max, Pageable pageable);

    List<UserEntity> findTop10ByOrderByCreatedDesc();

    List<UserEntity> findByUpdatedAfter(OffsetDateTime updatedAfter);

}
