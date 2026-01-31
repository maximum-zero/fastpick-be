package com.maximum0.fastpickbe.coupon.domain.repository;

import com.maximum0.fastpickbe.coupon.domain.model.CouponKeyword;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponListRequest;
import com.maximum0.fastpickbe.coupon.ui.dto.CouponSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CouponKeywordRepository {

    /**
     * 검색 조건에 부합하는 쿠폰 목록을 페이징 조회한다
     *
     * @param request  검색 키워드 및 필터 타입을 포함한 요청 객체
     * @param pageable 페이징 정보 (Offset, Limit)
     * @param now      현재 기준 시각
     * @return 검색 조건에 부합하는 쿠폰 요약 정보 리스트
     */
    List<CouponSummaryResponse> findAllByCondition(CouponListRequest request, Pageable pageable, LocalDateTime now);

    /**
     * 검색 조건에 부합하는 전체 쿠폰 개수를 조회한다
     *
     * @param request 검색 키워드 및 필터 타입을 포함한 요청 객체
     * @param now     현재 기준 시각
     * @return 검색 조건에 부합하는 전체 데이터 건수
     */
    Long countByCondition(CouponListRequest request, LocalDateTime now);

    /**
     * 쿠폰 키워드 목록을 일괄 저장한다.
     *
     * @param couponKeywordList 저장할 쿠폰 키워드 엔티티 목록
     */
    void saveAll(List<CouponKeyword> couponKeywordList);

}
