package io.github.metdaisy.amaazon.user.presentation.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.support.RestControllerTest;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAccountControllerTest extends RestControllerTest {

  @MockitoBean
  private UserService userService;

  @Test
  void deactivate_success() throws Exception {
    mockMvc.perform(post(API_PREFIX + "/me/deactivate"))
        .andExpect(status().isNoContent());

    verify(userService).deactivate(USER_ID);
  }
}
