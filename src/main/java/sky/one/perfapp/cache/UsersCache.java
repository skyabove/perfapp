package sky.one.perfapp.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sky.one.perfapp.dto.UserDto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class UsersCache {
    private static final String KEY_USERS = "users:all";
    private static final String KEY_ID = "users:id";
    private static final Duration TTL = Duration.ofSeconds(300); // под себя

    private final StringRedisTemplate redis;
    private final ObjectMapper om;


    public Optional<List<UserDto>> getUsers() {
        String json = redis.opsForValue().get(KEY_USERS);
        if (json == null) return Optional.empty();

        try {
            return Optional.of(om.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            log.warn("Unable to retrieve values from cache: {}", e.getMessage());
            redis.delete(KEY_USERS);
            return Optional.empty();
        }
    }

    public Optional<UserDto> getUserById(Integer id) {
        String json = redis.opsForValue().get(KEY_ID + id);
        if (json == null) return Optional.empty();

        try {
            return Optional.of(om.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            log.warn("Unable to retrieve value from cache: {}", e.getMessage());
            redis.delete(KEY_ID + id);
            return Optional.empty();
        }
    }

    public void put(List<UserDto> users) {
        try {
            redis.opsForValue().set(KEY_USERS, om.writeValueAsString(users), TTL);
        } catch (Exception e) {
            log.warn("Unable to put values to redis: {}", e.getMessage());
        }
    }

    public void putUserById(Integer id, UserDto user) {
        try {
            redis.opsForValue().set(KEY_ID + id, om.writeValueAsString(user), TTL);
        } catch (Exception e) {
            log.warn("Unable to put value to redis: {}", e.getMessage());
        }
    }

}
