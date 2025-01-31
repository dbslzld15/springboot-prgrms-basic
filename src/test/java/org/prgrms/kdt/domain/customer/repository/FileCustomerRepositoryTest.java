package org.prgrms.kdt.domain.customer.repository;

import org.junit.jupiter.api.*;
import org.prgrms.kdt.domain.customer.model.Customer;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileCustomerRepositoryTest {

    String csvPath = "src\\test\\resources";
    String fileName = "customer_blacklist_sample.csv";

    /**
     * 각 테스트 실행 후 csv에 있는 내용 전부를 지운다.
     */
    @AfterEach
    void deleteCsvContents() throws IOException {
        new FileOutputStream(csvPath+"\\"+fileName).close();
    }

    @Test
    @DisplayName("csv로 저장된 고객 목록을 불러올 수 있다.")
    void findCustomers(){
        //given
        CustomerRepository customerRepository = new FileCustomerRepository(csvPath, fileName);
        LocalDateTime now = LocalDateTime.now();
        Customer customerPark = new Customer(UUID.randomUUID(),"park" , "d@naver.com", now, now);
        Customer customerKim = new Customer(UUID.randomUUID(),"kim", "a@gmail.com", now, now);
        customerRepository.save(customerPark);
        customerRepository.save(customerKim);
        //when
        List<Customer> customers = customerRepository.findAll();
        //then
        assertThat(customers.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("고객 저장 시 파일을 읽지 못할 경우 예외가 발생한다.")
    void saveCustomer_exception(){
        //given
        CustomerRepository customerRepository = new FileCustomerRepository("", "");
        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer(UUID.randomUUID(),"park" , "a@naver.com", now, now);
        ReflectionTestUtils.setField(customerRepository, "csvPath", "");
        //when
        //then
        assertThatThrownBy(() -> customerRepository.save(customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일의 경로 혹은 이름을 확인해주세요.");
    }

    @Test
    @DisplayName("고객 전체 조회시 파일을 읽지 못할 경우 예외가 발생한다.")
    void findAllCustomer_exception(){
        //given
        CustomerRepository customerRepository = new FileCustomerRepository("", "");
        //when
        //then
        assertThatThrownBy(() -> customerRepository.findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일의 경로 혹은 이름을 확인해주세요.");
    }
}