package dev.se1dhe.bot.model;

import dev.se1dhe.bot.model.DbUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"db_user_id", "serverName"}) // Уникальная комбинация dbUser и serverName
})
public class GameUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Связь много к одному
    @JoinColumn(name = "tg_user_id", nullable = false)
    private DbUser dbUser;

    @Column(nullable = false, unique = true)
    private Long charId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private LocalDateTime codeCreatedAt;

    private boolean active;

    private boolean notified;

    @Column(nullable = false)
    private String serverName; // Имя сервера

    // Конструктор для создания объекта
    public GameUser(DbUser dbUser, Long charId, String code, String serverName) {
        this.dbUser = dbUser;
        this.charId = charId;
        this.code = code;
        this.codeCreatedAt = LocalDateTime.now();
        this.active = false;
        this.notified = false;
        this.serverName = serverName;
    }


}