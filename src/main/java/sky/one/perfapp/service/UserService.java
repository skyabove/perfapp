package sky.one.perfapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.mapper.UserMapper;
import sky.one.perfapp.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> getUsers() {
        return userRepository.findAll().stream().map(UserMapper::toDto).toList();
    }

}
