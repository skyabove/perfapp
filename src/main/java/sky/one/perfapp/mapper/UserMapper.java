package sky.one.perfapp.mapper;

import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.model.UserEntity;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(UserEntity e) {
        return new UserDto(
                e.getName(),
                e.getAge()
        );
    }
}