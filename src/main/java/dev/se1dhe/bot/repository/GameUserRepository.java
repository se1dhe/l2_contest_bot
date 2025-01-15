package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.GameUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameUserRepository extends JpaRepository<GameUser, Long> {

    Optional<GameUser> findByCode(String code);
    List<GameUser> findByDbUser(DbUser dbUser);
    Optional<GameUser> findByCharId(Long charId);
    Optional<GameUser> findByDbUserAndServerName(DbUser dbUser, String serverName);
    List<GameUser> findByDbUserAndActive(DbUser dbUser, boolean active);
    void deleteByCode(String code);
    List<GameUser> findByActiveTrueAndNotifiedFalse();
    void deleteAllByDbUser(DbUser dbUser);

    Optional<GameUser> findByDbUserAndServerNameAndActive(DbUser dbUser, String serverName, boolean active);

    List<GameUser> findByCharIdAndServerNameAndActive(Long charId, String serverName, boolean active);
}