package org.example.passpoint.domain.question.repository;

import org.example.passpoint.domain.question.document.QuestionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 질문 검색용 ElasticSearch 리포지토리
 * - QuestionDocument를 ES 인덱스에 색인/조회
 */
public interface QuestionSearchRepository extends ElasticsearchRepository<QuestionDocument, Long> {

    /**
     * 제목·본문에서 키워드 검색 (multi_match)
     * - title, content 두 필드를 Nori로 분석해 매칭
     */
    @Query("""
            {
              "multi_match": {
                "query": "?0",
                "fields": ["title", "content"]
              }
            }
            """)
    Page<QuestionDocument> searchByKeyword(String keyword, Pageable pageable);
}
