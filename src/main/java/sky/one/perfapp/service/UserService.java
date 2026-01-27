package sky.one.perfapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sky.one.perfapp.cache.UsersCache;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.mapper.UserMapper;
import sky.one.perfapp.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UsersCache cache;

    public List<UserDto> getUsers() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

    public List<UserDto> getCachedUsers() {
        return cache.get().orElseGet(() -> {
            List<UserDto> users = userRepository.findAll()
                    .stream()
                    .map(UserMapper::toDto)
                    .toList();

            cache.put(users);
            return users;
        });
    }

}
