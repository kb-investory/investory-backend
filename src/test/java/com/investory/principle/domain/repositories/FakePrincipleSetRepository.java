package com.investory.principle.domain.repositories;

import com.investory.principle.domain.constant.PrincipleSetStatus;
import com.investory.principle.domain.model.PrincipleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FakePrincipleSetRepository implements PrincipleSetRepository {

    private final List<PrincipleSet> sets = new ArrayList<>();
    private long nextId = 1L;

    public void add(PrincipleSet... sets) {
        this.sets.addAll(List.of(sets));
    }

    @Override
    public Optional<PrincipleSet> findActiveByUserId(Long userId) {
        return sets.stream()
                .filter(s -> s.getUserId().equals(userId) && s.getStatus() == PrincipleSetStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public int findMaxVersionNo(Long userId) {
        return sets.stream()
                .filter(s -> s.getUserId().equals(userId))
                .mapToInt(PrincipleSet::getVersionNo)
                .max()
                .orElse(0);
    }

    @Override
    public void archiveActive(Long userId) {
        for (int i = 0; i < sets.size(); i++) {
            PrincipleSet s = sets.get(i);
            if (s.getUserId().equals(userId) && s.getStatus() == PrincipleSetStatus.ACTIVE) {
                sets.set(i, PrincipleSet.of(s.getPrincipleSetId(), s.getUserId(), s.getAnalysisRunId(), s.getVersionNo(),
                        PrincipleSetStatus.ARCHIVED, s.getItems(), s.getCreatedAt(), s.getUpdatedAt()));
            }
        }
    }

    @Override
    public Long save(PrincipleSet principleSet) {
        Long id = nextId++;
        sets.add(PrincipleSet.of(id, principleSet.getUserId(), principleSet.getAnalysisRunId(), principleSet.getVersionNo(),
                principleSet.getStatus(), principleSet.getItems(), principleSet.getCreatedAt(), principleSet.getUpdatedAt()));
        return id;
    }
}
