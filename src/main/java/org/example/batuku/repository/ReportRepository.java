package org.example.batuku.repository;

import org.example.batuku.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(Report.ReportStatus status);
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, Report.TargetType targetType, Long targetId);
}
