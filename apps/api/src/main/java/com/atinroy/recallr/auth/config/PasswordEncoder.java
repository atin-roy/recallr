package com.atinroy.recallr.auth.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {
    public String encode(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
