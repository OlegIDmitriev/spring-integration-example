package com.dmitriev.i.oleg.si.example.persitence.repository;

import com.dmitriev.i.oleg.si.example.persitence.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
}
