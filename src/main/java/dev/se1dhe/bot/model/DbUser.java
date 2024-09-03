package dev.se1dhe.bot.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "db_user")
public class DbUser {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private Long id;
    private String userName;
    private int accessLevel;
    private LocalDateTime regDate;
    private boolean getPrize;
    private String lang;

    @ManyToMany(mappedBy = "participant")
    private List<Raffle> raffles = new ArrayList<>();
    private int state;

    public DbUser(Long id, String userName, int accessLevel, LocalDateTime regDate, String lang) {
        this.id = id;
        this.userName = userName;
        this.accessLevel = accessLevel;
        this.regDate = regDate;
        this.lang = lang;
    }


}
