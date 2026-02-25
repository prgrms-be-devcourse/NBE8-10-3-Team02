package com.back.domain.game.game.repository;

import com.back.domain.game.game.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformRepository extends JpaRepository<Platform, Long> {
    List<Platform> findByIgdbIdIn(List<Long> igdbIds);
}
