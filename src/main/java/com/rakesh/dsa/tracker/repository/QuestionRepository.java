package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionRepository extends MongoRepository<Question, String> {

//    Page<Question> findAll(Query query, Pageable pageable);

}
