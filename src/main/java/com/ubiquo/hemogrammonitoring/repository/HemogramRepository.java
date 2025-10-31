package com.ubiquo.hemogrammonitoring.repository;

import com.ubiquo.hemogrammonitoring.entity.HemogramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HemogramRepository extends JpaRepository<HemogramEntity, Long> {
}
