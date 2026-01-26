package com.maximum0.fastpickbe.coupon.support;

import com.maximum0.fastpickbe.coupon.application.admin.KeywordExtractor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("local")
@Disabled("운영/개발 환경에 대량 데이터를 직접 주입할 때만 수동으로 실행합니다.")
@DisplayName("Coupon Data Seeding 도구")
class CouponDataSeeder {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private KeywordExtractor keywordExtractor;

    private static final int TOTAL_COUNT = 1_000_000;
    private static final int BATCH_SIZE = 1000;

    @Nested
    @DisplayName("Coupon Provisioning Test")
    class CouponProvisioningTest {

        @Test
        @DisplayName("쿠폰과 쿠폰 키워드를 DB에 저장합니다.")
        void couponData_bulkInsert_success() {
            // given
            String[] brands = {"HOKA", "NIKE", "A.P.C.", "KITSUNE", "SOTO", "NEW BALANCE", "PATAGONIA"};
            String[] modifiers = {"[단독]", "[OFF]", "[NEW]", "[LAST]", "[29CM]"};
            String[] items = {"스니커즈", "티셔츠", "백팩", "재킷", "팬츠"};
            Random random = new Random();
            LocalDateTime now = LocalDateTime.now();

            // when
            for (int i = 0; i < TOTAL_COUNT / BATCH_SIZE; i++) {
                final int batchStep = i;
                transactionTemplate.execute(status -> {
                    for (int j = 0; j < BATCH_SIZE; j++) {
                        int offset = (batchStep * BATCH_SIZE) + j;
                        LocalDateTime createdAt = now.minusSeconds(offset);

                        String brand = brands[random.nextInt(brands.length)];
                        String title = String.format("%s %s %s", modifiers[random.nextInt(modifiers.length)], brand, items[random.nextInt(items.length)]);

                        // 쿠폰 추가
                        Long couponId = jdbcTemplate.queryForObject(
                                "INSERT INTO tb_coupon (brand, title, summary, description, total_quantity, issued_quantity, limit_per_user, is_sold_out, start_at, end_at, use_status, created_at, updated_at) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                                Long.class,
                                brand, title, "29CM 혜택", "상세 설명", 1000, 0, 1, false,
                                createdAt.minusDays(1), createdAt.plusDays(30), "AVAILABLE", createdAt, createdAt
                        );

                        // 쿠폰 키워드 추출
                        List<String> keywords = keywordExtractor.extract(brand, title);
                        List<Object[]> keywordArgs = keywords.stream()
                                .map(kw -> new Object[]{
                                        couponId,
                                        kw,
                                        false,
                                        createdAt.minusDays(1),
                                        createdAt.plusDays(30),
                                        "AVAILABLE",
                                        createdAt
                                })
                                .collect(Collectors.toList());

                        // 4. 키워드 벌크 삽입
                        jdbcTemplate.batchUpdate(
                                "INSERT INTO tb_coupon_keyword (coupon_id, keyword, is_sold_out, start_at, end_at, use_status, created_at) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                                keywordArgs
                        );
                    }
                    return null;
                });
                System.out.printf("[Provisioning] %d / %d 건 (%.1f%%) 완료... 🥊%n",
                        (batchStep + 1) * BATCH_SIZE, TOTAL_COUNT, ((double)(batchStep + 1) * BATCH_SIZE / TOTAL_COUNT) * 100);
            }
        }
    }
}
