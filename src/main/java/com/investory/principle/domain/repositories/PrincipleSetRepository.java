package com.investory.principle.domain.repositories;

import com.investory.principle.domain.model.PrincipleSet;

import java.util.Optional;

public interface PrincipleSetRepository {
    Optional<PrincipleSet> findActiveByUserId(Long userId);
    int findMaxVersionNo(Long userId);
    void archiveActive(Long userId);

    // 세트+아이템을 함께 insert하고, 새로 생성된 principleSetId를 반환한다.
    Long save(PrincipleSet principleSet);
}
