package com.shop.service;

import com.shop.dao.AppUserDao;
import com.shop.model.AppUser;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

public class AuthService {
    private final AppUserDao appUserDao;

    public AuthService() {
        this.appUserDao = new AppUserDao();
    }

    public AuthService(AppUserDao appUserDao) {
        this.appUserDao = appUserDao;
    }

    public void setup(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        AppUser user = new AppUser();
        user.setUsername("owner");
        user.setPasswordHash(hash);
        user.setCreatedAt(Instant.now().toString());
        appUserDao.insert(user);
    }

    public boolean login(String password) {
        Optional<AppUser> userOpt = appUserDao.findFirst();
        if (userOpt.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(password, userOpt.get().getPasswordHash());
    }
}
