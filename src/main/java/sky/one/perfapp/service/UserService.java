package sky.one.perfapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sky.one.perfapp.cache.UsersCache;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.mapper.UserMapper;
import sky.one.perfapp.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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

    public UserDto getRandomUser() {
        return userRepository.findById(getRandomId()).map(UserMapper::toDto).orElseGet(UserDto::new);
    }

    public List<UserDto> getFiveRandomUsers() {
        List<UserDto> result = new ArrayList<>();
        IntStream.range(0,5).forEach(i -> result.add(this.getRandomUser()));
        return result;
    }

    private long getRandomId() {
        return ThreadLocalRandom.current().nextLong(1, 20);
    }

}
