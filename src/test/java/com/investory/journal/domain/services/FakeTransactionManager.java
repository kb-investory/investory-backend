package com.investory.journal.domain.services;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

// 테스트에는 실제 DB 트랜잭션이 없으므로, JournalService의 TransactionTemplate이 동작할 수 있도록
// 커밋/롤백을 아무 것도 하지 않는 PlatformTransactionManager를 사용한다.
public class FakeTransactionManager implements PlatformTransactionManager {
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
    }

    @Override
    public void rollback(TransactionStatus status) {
    }
}
