package com.maximum0.fastpickbe.base;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maximum0.fastpickbe.common.exception.GlobalExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * API 문서 자동화를 위한 공통 지원 클래스
 * 모든 Controller 단위 테스트는 이 클래스를 상속받아 중복 설정을 제거한다.
 */
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
public abstract class BaseRestDocsTest {
    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp(RestDocumentationContextProvider provider) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(initController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true))
                .apply(documentationConfiguration(provider)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    /**
     * 테스트할 컨트롤러를 구현체에서 반환
     */
    protected abstract Object initController();

    /**
     * REST Docs 핸들러 생성
     */
    protected RestDocumentationResultHandler restDocument(String identifier, Snippet... snippets) {
        return MockMvcRestDocumentation.document(identifier, snippets);
    }

    /**
     * JSON POST 요청 생성
     */
    protected MockHttpServletRequestBuilder postRequest(String url, Object dto) throws Exception {
        return post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto));
    }

    /**
     * 성공 응답 필드 결합
     */
    protected List<FieldDescriptor> successFields(FieldDescriptor... descriptors) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("code").description("응답 코드"));
        fields.add(fieldWithPath("message").description("응답 메시지"));
        if (descriptors != null) fields.addAll(Arrays.asList(descriptors));
        return fields;
    }

    /**
     * 에러 응답 필드 정의
     */
    protected List<FieldDescriptor> errorFields() {
        return List.of(
                fieldWithPath("code").description("에러 코드"),
                fieldWithPath("message").description("에러 메시지"),
                fieldWithPath("status").description("HTTP 상태 코드"),

                fieldWithPath("errors").type(JsonFieldType.ARRAY).description("상세 에러 목록 (필드 검증 실패 시)").optional(),
                fieldWithPath("errors[].field").type(JsonFieldType.STRING).description("에러 필드").optional(),
                fieldWithPath("errors[].value").type(JsonFieldType.STRING).description("에러 값").optional(),
                fieldWithPath("errors[].reason").type(JsonFieldType.STRING).description("에러 사유").optional()
        );
    }
}