package com.hogwarts.library.repository;

import com.hogwarts.library.domain.LibraryScope;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LibraryScopeRepository extends Repository<LibraryScope, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LibraryScope s where s.id = :id")
    Optional<LibraryScope> findByIdForUpdate(@Param("id") long id);
}
