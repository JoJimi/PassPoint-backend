package org.example.passpoint.domain.question.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.question.document.QuestionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * QuestionSearchCustom 구현
 * - bool 쿼리로 키워드(must)와 카테고리(filter)를 동적 조립
 */
@RequiredArgsConstructor
public class QuestionSearchCustomImpl implements QuestionSearchCustom{

    private final ElasticsearchOperations operations;

    @Override
    public Page<QuestionDocument> searchQuestions(String keyword, String mainCategory, Pageable pageable) {
        // ── ES bool 쿼리의 두 가지 조건 리스트 ──
        // must:   검색 조건. 매칭 정도가 점수(관련도)에 반영됨 → 정렬에 영향
        // filter: 거르기 조건. 점수 계산 없이 "조건 충족/불충족"만 판단 → 더 빠르고 캐싱됨
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // ── 키워드 조건 (있을 때만 추가) ──
        // multi_match: 여러 필드(title, content)에서 한 검색어를 동시에 찾는 쿼리
        // title/content는 Nori(korean) 분석기로 매핑돼 있어 형태소 단위로 매칭됨
        // → "가비지컬렉션"으로 검색해도 "가비지 컬렉션"을 찾을 수 있음
        if (StringUtils.hasText(keyword)) {
            mustQueries.add(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(keyword)               // 사용자가 입력한 검색어
                            .fields("title", "content")   // 이 두 필드에서 검색
                    )
            ));
        }

        // ── 카테고리 조건 (있을 때만 추가) ──
        // term: 분석하지 않고 값을 "정확히" 매칭하는 쿼리 (Keyword 필드용)
        // mainCategory는 Keyword 타입이라 "CS"면 정확히 "CS"인 문서만 걸러짐
        // filter에 넣는 이유: 카테고리는 "맞다/아니다"만 중요하고 점수는 불필요
        if (StringUtils.hasText(mainCategory)) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t
                            .field("mainCategory")   // 필터할 필드
                            .value(mainCategory)     // 이 값과 정확히 일치하는 것만
                    )
            ));
        }

        // ── bool 쿼리로 조건들을 하나로 조립 ──
        // bool: 여러 조건을 must/filter/should 등으로 묶는 복합 쿼리
        // must와 filter가 둘 다 비어있으면 → 전체 문서 매칭 (조건 없는 전체 조회)
        // 키워드만 있으면 → 키워드 검색 / 카테고리만 → 카테고리 필터 / 둘 다 → 조합
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(mustQueries)        // 키워드 조건 적용
                                .filter(filterQueries)    // 카테고리 조건 적용
                        )
                )
                .withPageable(pageable)   // 페이징 (page, size) 적용
                .build();

        // ── 쿼리 실행 ──
        // operations.search: 위에서 만든 쿼리를 ES에 보내 결과(SearchHits)를 받음
        // SearchHits = 검색 결과 묶음 (각 hit가 문서 1건 + 점수 등 메타정보)
        SearchHits<QuestionDocument> hits = operations.search(query, QuestionDocument.class);

        // ── SearchHits → Spring의 Page로 변환 ──
        // hit.getContent()로 실제 QuestionDocument만 꺼내 리스트로 모음
        List<QuestionDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        // PageImpl: 내용 + 페이징 정보 + 전체 건수(totalHits)로 Page 객체 생성
        // → 컨트롤러가 totalElements, totalPages 등을 응답에 담을 수 있음
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}
