package org.example.passpoint.domain.question.document;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.question.entity.Question;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

/**
 * 질문 검색용 ElasticSearch 문서
 * - PostgreSQL의 Question을 검색하기 좋은 형태로 색인 (검색 전용 사본)
 * - title/content는 Nori(korean) 분석기로 한글 형태소 검색
 * - 카테고리/난이도/태그는 keyword로 정확 매칭 필터
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "questions")
@Setting(settingPath = "elasticsearch/question-settings.json")
public class QuestionDocument {

    @Id
    private Long id;

    // 한글 검색 대상 - Nori 분석기 적용
    @Field(type = FieldType.Text, analyzer = "korean")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean")
    private String content;

    @Field(type = FieldType.Keyword)
    private String mainCategory;

    @Field(type = FieldType.Keyword)
    private String subCategory;

    @Field(type = FieldType.Keyword)
    private String difficulty;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Builder
    private QuestionDocument(Long id, String title, String content,
                             String mainCategory, String subCategory,
                             String difficulty, List<String> tags) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.difficulty = difficulty;
        this.tags = tags;
    }

    /** PostgreSQL의 Question 엔티티 → ES 색인용 Document 변환 */
    public static QuestionDocument from(Question question) {
        return QuestionDocument.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .mainCategory(question.getMainCategory().name())
                .subCategory(question.getSubCategory().name())
                .difficulty(question.getDifficulty().name())
                .tags(List.of())        // 태그 연동은 나중에 (지금은 빈 리스트)
                .build();
    }
}
