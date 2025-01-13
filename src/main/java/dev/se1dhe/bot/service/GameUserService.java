package dev.se1dhe.bot.service;

import dev.se1dhe.bot.exception.DuplicateCharIdException;
import dev.se1dhe.bot.exception.UserAlreadyBoundException;
import dev.se1dhe.bot.exception.UserNotFoundException;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.GameUser;
import dev.se1dhe.bot.repository.DbUserRepository;
import dev.se1dhe.bot.repository.GameUserRepository;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameUserService {

    private final GameUserRepository userRepository;
    private final DbUserRepository dbUserRepository;
    @Setter
    private int codeValidityMinutes = 5;

    @Autowired
    public GameUserService(GameUserRepository userRepository, DbUserRepository dbUserRepository) {
        this.userRepository = userRepository;
        this.dbUserRepository = dbUserRepository;
    }

    @Transactional
    public GameUser createGameUser(Long dbUserId, Long charId, String serverName) {
        String code = UUID.randomUUID().toString().substring(0, 8);

        Optional<DbUser> dbUserOptional = dbUserRepository.findById(dbUserId);
        if (dbUserOptional.isEmpty()) {
            throw new UserNotFoundException("Пользователь с ID " + dbUserId + " не найден.");
        }

        DbUser dbUser = dbUserOptional.get();

        // Проверяем, существует ли уже привязка для этого пользователя и сервера
        Optional<GameUser> existingGameUser = userRepository.findByDbUserAndServerName(dbUser, serverName);
        if (existingGameUser.isPresent()) {
            throw new UserAlreadyBoundException("У этого пользователя уже есть привязанный игровой аккаунт на сервере " + serverName);
        }

        GameUser user = new GameUser(dbUser, charId, code, serverName);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getMostSpecificCause().getMessage().contains("charId")) {
                throw new DuplicateCharIdException("Пользователь с таким charId уже существует.", e);
            } else {
                throw new RuntimeException("Ошибка при создании пользователя.", e);
            }
        }
    }

    public boolean isCodeValid(String code) {
        Optional<GameUser> user = userRepository.findByCode(code);
        return user.isPresent() && ChronoUnit.MINUTES.between(user.get().getCodeCreatedAt(), LocalDateTime.now()) <= codeValidityMinutes;
    }

    @Transactional
    public boolean activateUser(String code, Long charId) {
        Optional<GameUser> userOptional = userRepository.findByCode(code);
        if (userOptional.isEmpty() || !userOptional.get().getCharId().equals(charId) || !isCodeValid(code)) {
            return false;
        }
        GameUser user = userOptional.get();
        user.setActive(true);
        userRepository.save(user);
        userRepository.deleteByCode(code);
        return true;
    }

    @Transactional
    public void markAsNotified(Long dbUserId) {
        Optional<DbUser> dbUserOptional = dbUserRepository.findById(dbUserId);
        if (dbUserOptional.isPresent()) {
            DbUser dbUser = dbUserOptional.get();
            Optional<GameUser> userOptional = userRepository.findByDbUser(dbUser);
            if (userOptional.isPresent()) {
                GameUser user = userOptional.get();
                user.setNotified(true);
                userRepository.save(user);
            }
        }
    }

    public Optional<GameUser> findByDbUserId(Long dbUserId) {
        Optional<DbUser> dbUserOptional = dbUserRepository.findById(dbUserId);
        return dbUserOptional.flatMap(userRepository::findByDbUser);
    }

    public Optional<GameUser> findByCharId(Long charId) {
        return userRepository.findByCharId(charId);
    }

    public Optional<GameUser> findByDbUserAndServerName(DbUser dbUser, String serverName) {
        return userRepository.findByDbUserAndServerName(dbUser, serverName);
    }

    @Transactional
    public void deleteAllByDbUser(DbUser dbUser) {
        userRepository.deleteAllByDbUser(dbUser);
    }
}