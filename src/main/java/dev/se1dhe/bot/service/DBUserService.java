package dev.se1dhe.bot.service;


import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.repository.DbUserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Log4j2
public class DBUserService {

    private final DbUserRepository dbUserRepository;

    public DBUserService(DbUserRepository dbUserRepository) {
        this.dbUserRepository = dbUserRepository;
    }

    public DbUser registerUser(User user) {
        String userName = Objects.requireNonNullElse(user.getUserName(), user.getFirstName());
        Long userId = user.getId();
        Optional<DbUser> existingUserOptional = dbUserRepository.findById(userId);

        if (existingUserOptional.isPresent()) {
            if(existingUserOptional.get().getLang()==null) {
                existingUserOptional.get().setLang("ru");
            }
            return existingUserOptional.get();
        } else {
            DbUser newUser = new DbUser(userId, userName, 0, LocalDateTime.now(), "ru");
            dbUserRepository.save(newUser);
            log.info("Пользователь успешно зарегистрирован: {}", newUser.getUserName());
            return newUser;
        }
    }

    public DbUser findUserById(Long id) {
        DbUser user = dbUserRepository.findById(id).orElse(null);

        if (user != null) {
            log.info("Пользователь найден: {}", user);
        } else {
            log.info("Пользователь с ID {} не найден", id);
        }

        return user;
    }

    public void updateUser(DbUser user) {
        log.info("Изменение информации пользователя: {}", user);

        // Проверка существования пользователя с таким же ID
        // ...

        // Обновление информации пользователя в базе данных
        dbUserRepository.save(user);

        log.info("Информация пользователя успешно обновлена");
    }
    public void deleteUser(Long id) {
        log.info("Удаление пользователя с ID: {}", id);
        dbUserRepository.deleteById(id);
        log.info("Пользователь с ID {} успешно удален", id);
    }

    public boolean validate(Long id, int level) {
        if (level == 0) {
            return true;
        }

        final DbUser user = findUserById(id);
        return (user != null) && (user.getAccessLevel() >= level);
    }

    public List<Raffle> getRafflesByUserId(Long userId) {
        DbUser user = dbUserRepository.findById(userId).orElse(null);
        if (user != null) {
            return user.getRaffles();
        }
        return null;
    }

}


