package dev.se1dhe.bot.model;


import dev.se1dhe.bot.model.enums.PrizeType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class Prize {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private PrizeType type;
    private String itemName;
    private int itemId;
    private int count;

    private int place;
    @ManyToMany(mappedBy = "prizes")
    private List<Raffle> raffles = new ArrayList<>();


}
