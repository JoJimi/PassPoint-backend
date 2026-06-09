package org.example.passpoint.domain.question.repository.search;

import org.example.passpoint.domain.question.document.QuestionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 질문 검색용 ElasticSearch 리포지토리
 * - QuestionDocument를 ES 인덱스에 색인/조회
 */
public interface QuestionSearchRepository
        extends ElasticsearchRepository<QuestionDocument, Long>, QuestionSearchCustom {
}
