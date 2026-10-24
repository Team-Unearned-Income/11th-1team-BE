package org.example.knockin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.example.knockin.board.controller.RoomMateController;
import org.example.knockin.board.dto.BoardPreSignedDto;
import org.example.knockin.board.dto.BoardPreSignedModifyDto;
import org.example.knockin.board.service.RoommateBoardService;
import org.example.knockin.global.auth.dto.PrincipalDetails;
import org.example.knockin.mate.service.RoommateMatchingService;
import org.example.knockin.member.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class BoardPresignedControllerTest {
    private static final String BOARD = """
            {"title":"제목","contents":"내용","deposit":1000,"mountlyRent":50,"managementCost":5,"roomTypeId":1,"regionId":2}
            """;
    private final RoommateBoardService service = mock(RoommateBoardService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var details = mock(PrincipalDetails.class);
        when(details.getMember()).thenReturn(Member.builder().id(42L).build());
        mvc = MockMvcBuilders.standaloneSetup(new RoomMateController(service, mock(RoommateMatchingService.class)))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter p) { return p.getParameterType() == PrincipalDetails.class; }
                    public Object resolveArgument(MethodParameter p, ModelAndViewContainer c, NativeWebRequest r, WebDataBinderFactory f) {
                        return details;
                    }
                }).build();
    }

    @Test
    void existingMultipartRouteStillInvokesOriginalSave() throws Exception {
        mvc.perform(multipart("/roommate/boards").file(new MockMultipartFile("request", "", "application/json",
                BOARD.getBytes(StandardCharsets.UTF_8)))).andExpect(status().isOk());
        verify(service).save(any(), eq(42L), isNull());
        verify(service, never()).saveBoardPreSigned(any(), any());
    }

    @Test
    void newJsonRoutePassesAuthenticatedMemberIdAndImageKeys() throws Exception {
        String body = BOARD.strip().replace("}", ",\"images\":[{\"fileKey\":\"uploads/presigned/42/key.jpg\",\"originalFilename\":\"room.jpg\",\"thumbnail\":true}]}");
        mvc.perform(post("/roommate/boards/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        var request = ArgumentCaptor.forClass(BoardPreSignedDto.Request.class);
        verify(service).saveBoardPreSigned(request.capture(), eq(42L));
        assertThat(request.getValue().getImages().getFirst().getFileKey()).isEqualTo("uploads/presigned/42/key.jpg");
        verify(service, never()).save(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[{\"originalFilename\":\"room.jpg\",\"thumbnail\":true}]"})
    void rejectsIncompleteImage(String images) throws Exception {
        String body = BOARD.strip().replace("}", ",\"images\":" + images + "}");
        mvc.perform(post("/roommate/boards/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsRequestWithoutImages() throws Exception {
        mvc.perform(post("/roommate/boards/presigned").contentType(MediaType.APPLICATION_JSON).content(BOARD))
                .andExpect(status().isOk());
        verify(service).saveBoardPreSigned(any(), eq(42L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]"})
    void acceptsExplicitNullOrEmptyImages(String images) throws Exception {
        String body = BOARD.strip().replace("}", ",\"images\":" + images + "}");
        mvc.perform(post("/roommate/boards/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(service).saveBoardPreSigned(any(), eq(42L));
    }
    @Test
    void presignedModifyRoutesExistingIdsAndNewKeysWithAuthenticatedMember() throws Exception {
        String body = BOARD.replace("mountlyRent", "monthlyRent").strip().replace("}",
                ",\"existingImages\":[{\"boardFileId\":7,\"thumbnail\":false}],\"newImages\":[{\"fileKey\":\"uploads/42/new.jpg\",\"originalFilename\":\"new.jpg\",\"thumbnail\":true}]}");
        mvc.perform(put("/roommate/boards/3/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        var request = ArgumentCaptor.forClass(BoardPreSignedModifyDto.Request.class);
        verify(service).modifyBoardPreSigned(eq(42L), eq(3L), request.capture());
        assertThat(request.getValue().getMonthlyRent()).isEqualTo(50);
        assertThat(request.getValue().getExistingImages().getFirst().getBoardFileId()).isEqualTo(7L);
        assertThat(request.getValue().getNewImages().getFirst().getFileKey()).isEqualTo("uploads/42/new.jpg");
        verify(service, never()).modify(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"existingImages\":[{\"thumbnail\":true}]",
            "\"newImages\":[{\"fileKey\":\"\",\"originalFilename\":\"new.jpg\"}]",
            "\"newImages\":[{\"fileKey\":\"uploads/42/new.jpg\"}]"})
    void presignedModifyRejectsInvalidImageMetadata(String field) throws Exception {
        String body = BOARD.replace("mountlyRent", "monthlyRent").strip().replace("}", "," + field + "}");
        mvc.perform(put("/roommate/boards/3/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"existingImages\":null,\"newImages\":null", ",\"existingImages\":[],\"newImages\":[]"})
    void presignedModifyAllowsNoImages(String images) throws Exception {
        String body = BOARD.replace("mountlyRent", "monthlyRent").strip().replace("}", images + "}");
        mvc.perform(put("/roommate/boards/3/presigned").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(service).modifyBoardPreSigned(eq(42L), eq(3L), any());
    }

    @Test
    void originalMultipartModifyStillUsesOriginalService() throws Exception {
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/roommate/boards/3")
                        .file(new MockMultipartFile("request", "", "application/json",
                                BOARD.replace("mountlyRent", "monthlyRent").getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk());
        verify(service).modify(eq(42L), eq(3L), any(), isNull());
        verify(service, never()).modifyBoardPreSigned(any(), any(), any());
    }

}
