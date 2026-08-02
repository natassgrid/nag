package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ShiftSeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftSeatAllocationRepository extends JpaRepository<ShiftSeatAllocation, UUID> {

    List<ShiftSeatAllocation> findByShiftId(UUID shiftId);

    Optional<ShiftSeatAllocation> findByShiftIdAndCentreId(UUID shiftId, UUID centreId);

    List<ShiftSeatAllocation> findByCentreId(UUID centreId);
}
