package dev.se1dhe.bot.model;


import jakarta.persistence.*;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Winner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participant_id", nullable = false)
    private DbUser participant;

    @ManyToOne
    @JoinColumn(name = "raffle_id", nullable = false)
    private Raffle raffle;
    @ManyToOne
    @JoinColumn(name = "prize_id", nullable = false)
    private Prize prize;

    private boolean getPrize = false;

    public Winner(DbUser participant, Raffle raffle,Prize prize) {
        this.participant = participant;
        this.raffle = raffle;
        this.prize = prize;
    }


}