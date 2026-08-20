package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.repository.TeacherRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

  @Mock private TeacherRepository teacherRepository;

  private TeacherService service;

  @BeforeEach
  void setUp() {
    service = new TeacherService(teacherRepository);
  }

  @Test
  void lists_teachers_from_paginated_repository() {
    var userAccount = new UserAccount();
    userAccount.setId(UUID.randomUUID());

    var teacher = new Teacher();
    teacher.setId(UUID.randomUUID());
    teacher.setUserAccount(userAccount);
    teacher.setEmployeeNumber("TEC001");
    teacher.setFirstName("Jean");
    teacher.setLastName("Rakoto");

    var page = new PageImpl<>(List.of(teacher));
    when(teacherRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

    var result = service.list(0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmployeeNumber()).isEqualTo("TEC001");
  }

  @Test
  void list_returns_empty_when_no_teachers() {
    when(teacherRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of()));

    assertThat(service.list(0, 10)).isEmpty();
  }
}
