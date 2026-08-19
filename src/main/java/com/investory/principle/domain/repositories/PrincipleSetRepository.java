package com.investory.principle.domain.repositories;

import com.investory.principle.domain.model.PrincipleSet;

import java.util.Optional;

public interface PrincipleSetRepository {
    Optional<PrincipleSet> findActiveByUserId(Long userId);
    int findMaxVersionNo(Long userId);
    void archiveActive(Long userId);

    // 세트+아이템을 함께 insert하고, 새로 생성된 principleSetId를 반환한다.
    Long save(PrincipleSet principleSet);

    // 계정 탈퇴 시 — 버전 구분 없이 이 사용자의 모든 세트(+아이템)를 지운다.
    void deleteByUserId(Long userId);
}
