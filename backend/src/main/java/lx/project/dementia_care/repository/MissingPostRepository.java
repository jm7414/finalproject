package lx.project.dementia_care.repository;

import lx.project.dementia_care.entity.MissingPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissingPostRepository extends JpaRepository<MissingPost, Integer> {
}