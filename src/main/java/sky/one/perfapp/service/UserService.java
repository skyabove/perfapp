package sky.one.perfapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sky.one.perfapp.cache.UsersCache;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.mapper.UserMapper;
import sky.one.perfapp.repository.UserRepository;

import sky.one.perfapp.model.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UsersCache cache;

    public List<UserDto> getUsers() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

    public List<UserDto> getCachedUsers() {
        return cache.getUsers().orElseGet(() -> {
            List<UserDto> users = userRepository.findAll()
                    .stream()
                    .map(UserMapper::toDto)
                    .toList();

            cache.put(users);
            return users;
        });
    }

    public UserDto getRandomUserCached() {
        Long id = getRandomId();
        return cache.getUserById(id).orElseGet(() -> {
            UserDto userDto = userRepository.findById(id).map(UserMapper::toDto).orElseGet(UserDto::new);
            cache.putUserById(id, userDto);
            return userDto;
        });
    }

    public List<UserDto> getFiveRandomUsersCached() {
        List<UserDto> result = new ArrayList<>();
        IntStream.range(0,5).forEach(i -> result.add(this.getRandomUserCached()));
        return result;
    }

    public List<UserDto> getFiveRandomUsersCached(boolean singleSelect) {
        if (!singleSelect) {
            return getFiveRandomUsersCached();
        }
        return getFiveRandomUsersCachedSingleSelect();
    }

    public UserDto getRandomUser() {
        return userRepository.findById(getRandomId()).map(UserMapper::toDto).orElseGet(UserDto::new);
    }

    public List<UserDto> getFiveRandomUsers() {
        List<UserDto> result = new ArrayList<>();
        IntStream.range(0,5).forEach(i -> result.add(this.getRandomUser()));
        return result;
    }

    public List<UserDto> getFiveRandomUsers(boolean singleSelect) {
        if (!singleSelect) {
            return getFiveRandomUsers();
        }
        return getFiveRandomUsersSingleSelect();
    }

    private List<UserDto> getFiveRandomUsersSingleSelect() {
        List<Long> ids = getDistinctRandomIds(5);
        Map<Long, UserDto> byId = userRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, UserMapper::toDto));

        return ids.stream()
                .map(id -> byId.getOrDefault(id, new UserDto()))
                .toList();
    }

    private List<UserDto> getFiveRandomUsersCachedSingleSelect() {
        List<Long> ids = getDistinctRandomIds(5);
        Map<Long, UserDto> resultById = new HashMap<>();
        List<Long> missing = new ArrayList<>();

        for (Long id : ids) {
            cache.getUserById(id).ifPresentOrElse(
                    userDto -> resultById.put(id, userDto),
                    () -> missing.add(id)
            );
        }

        if (!missing.isEmpty()) {
            Map<Long, UserDto> fetchedById = userRepository.findAllById(missing)
                    .stream()
                    .collect(Collectors.toMap(UserEntity::getId, UserMapper::toDto));

            for (Long id : missing) {
                UserDto userDto = fetchedById.getOrDefault(id, new UserDto());
                resultById.put(id, userDto);
                cache.putUserById(id, userDto);
            }
        }

        return ids.stream()
                .map(id -> resultById.getOrDefault(id, new UserDto()))
                .toList();
    }

    private long getRandomId() {
        return ThreadLocalRandom.current().nextLong(1, 20);
    }

    private List<Long> getDistinctRandomIds(int count) {
        Set<Long> ids = new LinkedHashSet<>(count);
        while (ids.size() < count) {
            ids.add(getRandomId());
        }
        return new ArrayList<>(ids);
    }

}
