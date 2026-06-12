package com.harun.auth_service.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class HashPassword {
    public String generate(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean check(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
