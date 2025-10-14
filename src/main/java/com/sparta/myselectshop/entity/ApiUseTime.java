package com.sparta.myselectshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "api_use_time")
public class ApiUseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne //한유저가 유저타임 한 개 가짐 (단방향 - user에서는 조회 안 할 것임)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long totalTime;

    public ApiUseTime(User user, Long totalTime) {
        this.user = user;
        this.totalTime = totalTime;
    }

    public void addUseTime(long useTime) {
        this.totalTime += useTime;
    }
}