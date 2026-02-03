package sky.one.perfapp.dto;

public record UserDto( String name, Integer age) {
    public UserDto() {
        this("", 0);
    }
}
