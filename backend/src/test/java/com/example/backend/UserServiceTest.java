package com.example.backend;

import com.example.backend.model.User;
import com.example.backend.respository.UserRepository;
import com.example.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setNickname("Test User");
        testUser.setEmail("test@example.com");
        testUser.setInvitationCode("ABC123");
        testUser.setStatus(User.UserStatus.SINGLE);
        testUser.setGender(User.Gender.MALE);
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setAvatarUrl("/images/default-male.png");
    }

    @Test
    void testRegister_Success() {
        // Given
        Map<String, String> registerData = new HashMap<>();
        registerData.put("username", "newuser");
        registerData.put("password", "password123");
        registerData.put("nickname", "New User");
        registerData.put("email", "newuser@example.com");
        registerData.put("gender", "FEMALE");

        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newuser");
        newUser.setPassword("encodedPassword");
        newUser.setNickname("New User");
        newUser.setEmail("newuser@example.com");
        newUser.setInvitationCode("XYZ789");
        newUser.setStatus(User.UserStatus.SINGLE);
        newUser.setGender(User.Gender.FEMALE);
        newUser.setAvatarUrl("/images/default-female.png");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        Map<String, Object> result = userService.register(registerData);

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals("注册成功", result.get("message"));
        assertNotNull(result.get("user"));

        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals("newuser", userMap.get("username"));
        assertEquals("New User", userMap.get("nickname"));
        assertEquals("newuser@example.com", userMap.get("email"));
        assertNotNull(userMap.get("invitationCode"));

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_UsernameAlreadyExists() {
        // Given
        Map<String, String> registerData = new HashMap<>();
        registerData.put("username", "testuser");
        registerData.put("password", "password123");
        registerData.put("nickname", "Test User");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        Map<String, Object> result = userService.register(registerData);

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名已存在", result.get("message"));

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When
        Map<String, Object> result = userService.login("testuser", "password123");

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals("登录成功", result.get("message"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = userService.login("testuser", "wrongpassword");

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名或密码错误", result.get("message"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Map<String, Object> result = userService.getUserById(1L);

        // Then
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("user"));

        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals(1L, userMap.get("id"));
        assertEquals("testuser", userMap.get("username"));
        assertEquals("Test User", userMap.get("nickname"));

        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = userService.getUserById(999L);

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户不存在", result.get("message"));

        verify(userRepository).findById(999L);
    }

    @Test
    void testUpdateUserProfile_Success() {
        // Given
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("nickname", "Updated User");
        profileData.put("email", "updated@example.com");
        profileData.put("phone", "1234567890");
        profileData.put("gender", "FEMALE");
        profileData.put("birthDate", "1995-05-15");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);

        // When
        Map<String, Object> result = userService.updateUserProfile(1L, profileData);

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals("资料更新成功", result.get("message"));

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("updated@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile_EmailAlreadyExists() {
        // Given
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("email", "other@example.com");

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("other@example.com")).thenReturn(true);

        // When
        Map<String, Object> result = userService.updateUserProfile(1L, profileData);

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("邮箱已被其他用户使用", result.get("message"));

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("other@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        // When
        Map<String, Object> result = userService.changePassword(1L, "oldPassword", "newPassword");

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals("密码修改成功", result.get("message"));
        assertEquals("newEncodedPassword", testUser.getPassword());

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangePassword_InvalidOldPassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When
        Map<String, Object> result = userService.changePassword(1L, "wrongPassword", "newPassword");

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("原密码错误", result.get("message"));

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testMatchWithInvitationCode_Success() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setInvitationCode("XYZ789");
        user2.setStatus(User.Status.SINGLE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByInvitationCode("XYZ789")).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Map<String, Object> result = userService.matchWithInvitationCode(1L, "XYZ789");

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals("匹配成功", result.get("message"));

        assertEquals(User.Status.MATCHED, testUser.getStatus());
        assertEquals(User.Status.MATCHED, user2.getStatus());
        assertNotNull(testUser.getCoupleId());
        assertEquals(testUser.getCoupleId(), user2.getCoupleId());
        assertNotNull(testUser.getMatchDate());
        assertNotNull(user2.getMatchDate());

        verify(userRepository, times(2)).findById(1L);
        verify(userRepository).findByInvitationCode("XYZ789");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void testMatchWithInvitationCode_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = userService.matchWithInvitationCode(999L, "ABC123");

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户不存在", result.get("message"));

        verify(userRepository).findById(999L);
        verify(userRepository, never()).findByInvitationCode(anyString());
    }

    @Test
    void testMatchWithInvitationCode_InvalidCode() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByInvitationCode("INVALID")).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = userService.matchWithInvitationCode(1L, "INVALID");

        // Then
        assertFalse((Boolean) result.get("success"));
        assertEquals("邀请码无效", result.get("message"));

        verify(userRepository).findById(1L);
        verify(userRepository).findByInvitationCode("INVALID");
    }

    @Test
    void testSearchUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.searchUsers("test")).thenReturn(users);

        // When
        Map<String, Object> result = userService.searchUsers("test");

        // Then
        assertTrue((Boolean) result.get("success"));
        assertEquals(1, ((List<?>) result.get("users")).size());

        verify(userRepository).searchUsers("test");
    }

    @Test
    void testIsUsernameAvailable_True() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // When
        boolean result = userService.isUsernameAvailable("newuser");

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername("newuser");
    }

    @Test
    void testIsUsernameAvailable_False() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean result = userService.isUsernameAvailable("testuser");

        // Then
        assertFalse(result);
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    void testIsEmailAvailable_True() {
        // Given
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // When
        boolean result = userService.isEmailAvailable("new@example.com");

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail("new@example.com");
    }

    @Test
    void testIsEmailAvailable_False() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.isEmailAvailable("test@example.com");

        // Then
        assertFalse(result);
        verify(userRepository).existsByEmail("test@example.com");
    }
}
