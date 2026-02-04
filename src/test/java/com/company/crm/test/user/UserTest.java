package com.company.crm.test.user;

import com.company.crm.AbstractTest;
import com.company.crm.model.user.User;
import io.jmix.core.security.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample integration test for the User entity.
 */
public class UserTest extends AbstractTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void test_saveAndLoad() {
        // Create and save a new User
        var user = entities.user();

        // Check the new user can be loaded
        User loadedUser = dataManager.load(User.class).id(user.getId()).one();
        assertThat(loadedUser).isEqualTo(user);

        // Check the new user is available through UserRepository
        UserDetails userDetails = userRepository.loadUserByUsername(user.getUsername());
        assertThat(userDetails).isEqualTo(user);
    }
}
