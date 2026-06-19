package club.axinyue.easydeal.repository;

import club.axinyue.easydeal.entity.TestTable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
})
class TestTableRepositoryTest {
    @Autowired
    private TestTableRepository testTableRepository;

    @Test
    void saveAndFindById() {
        TestTable testTable = new TestTable();
        testTable.setName("测试数据");
        testTable.setStatus(1);

        TestTable saved = testTableRepository.saveAndFlush(testTable);

        Optional<TestTable> found = testTableRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("测试数据");
        assertThat(found.get().getStatus()).isEqualTo(1);
    }
}
