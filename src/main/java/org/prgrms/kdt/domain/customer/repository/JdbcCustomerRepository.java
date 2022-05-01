package org.prgrms.kdt.domain.customer.repository;

import org.prgrms.kdt.domain.customer.exception.CustomerDataException;
import org.prgrms.kdt.domain.customer.model.Customer;
import org.prgrms.kdt.domain.customer.model.CustomerType;
import org.prgrms.kdt.util.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.prgrms.kdt.domain.common.exception.ExceptionType.*;

@Repository
public class JdbcCustomerRepository implements CustomerRepository{
    private static final Logger logger = LoggerFactory.getLogger(JdbcCustomerRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCustomerRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<Customer> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM customer",
                Collections.emptyMap(),
                customerRowMapper());
    }

    @Override
    public UUID save(Customer customer) {
        int savedRows = jdbcTemplate.update(
                "INSERT INTO customer(customer_id, customer_type, name, email, created_date, modified_date) " +
                        "VALUES (UNHEX(REPLACE(:customerId, '-', '')), :customerType, :name, :email, :createdDate, :modifiedDate)",
                toParamMap(customer));
        if(savedRows != 1) {
            throw new CustomerDataException(NOT_SAVED);
        }
        return customer.getCustomerId();
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        try {
            Customer customer = jdbcTemplate.queryForObject("SELECT * FROM customer " +
                            "WHERE customer_id = UNHEX(REPLACE(:customerId, '-', ''))",
                    Collections.singletonMap("customerId", UuidUtils.UuidToByte(customerId)),
                    customerRowMapper());
            return Optional.of(customer);
        } catch (EmptyResultDataAccessException e) {
            logger.error("고객 ID에 해당하는 데이터가 존재하지 않습니다.", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Customer> findByVoucherId(UUID voucherId) {
        try{
            Customer customer = jdbcTemplate.queryForObject("SELECT * FROM customer c JOIN voucher v " +
                            "on c.customer_id = v.customer_id WHERE v.voucher_id = UNHEX(REPLACE(:voucherId, '-', ''))",
                    Collections.singletonMap("voucherId", UuidUtils.UuidToByte(voucherId)),
                    customerRowMapper());
            return Optional.of(customer);
        } catch (EmptyResultDataAccessException e) {
            logger.error("해당 바우처 ID에 대한 데이터가 존재하지 않습니다.", e);
            return Optional.empty();
        }
    }

    @Override
    public List<Customer> findAllByType(CustomerType customerType) {
        return jdbcTemplate.query("SELECT * FROM customer WHERE customer_type = :customerType",
                Collections.singletonMap("customerType", customerType.getType()),
                customerRowMapper());
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        try{
            Customer customer = jdbcTemplate.queryForObject("SELECT * FROM customer WHERE email = :email",
                    Collections.singletonMap("email", email),
                    customerRowMapper());
            return Optional.of(customer);
        } catch (EmptyResultDataAccessException e) {
            logger.error("해당 이메일에 대한 데이터가 존재하지 않습니다.", e);
            return Optional.empty();
        }
    }

    @Override
    public int update(Customer customer) {
        return jdbcTemplate.update("UPDATE customer " +
                        "SET customer_type = :customerType, name = :name, email = :email, modified_date = :modifiedDate " +
                        "WHERE customer_id = UNHEX(REPLACE(:customerId, '-', ''))",
                toParamMap(customer));
    }

    @Override
    public int deleteById(UUID customerId) {
        return jdbcTemplate.update("DELETE FROM customer " +
                        "WHERE customer_id = UNHEX(REPLACE(:customerId, '-', ''))",
                Collections.singletonMap("customerId", UuidUtils.UuidToByte(customerId)));
    }

    @Override
    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM customer", Collections.emptyMap());
    }

    private Map<String, Object> toParamMap(Customer customer) {
        return new HashMap<>() {{
            put("customerId", UuidUtils.UuidToByte(customer.getCustomerId()));
            put("customerType", customer.getCustomerType().getType());
            put("name", customer.getName());
            put("email", customer.getEmail());
            put("createdDate", Timestamp.valueOf(customer.getCreatedDateTime()));
            put("modifiedDate", customer.getModifiedDateTime() != null ?
                    Timestamp.valueOf(customer.getModifiedDateTime()) : null);
        }};
    }

    private RowMapper<Customer> customerRowMapper() {
        return (rs, rowNum) -> {
            UUID customerId = UuidUtils.byteToUUID(rs.getBytes("customer_id"));
            CustomerType customerType = CustomerType.findCustomerType(rs.getString("customer_type"));
            String name = rs.getString("name");
            String email = rs.getString("email");
            LocalDateTime createdDate = rs.getTimestamp("created_date").toLocalDateTime();
            LocalDateTime modifiedDate = rs.getTimestamp("modified_date") != null ?
                    rs.getTimestamp("modified_date").toLocalDateTime() : null;

            return new Customer(customerId, name, email, customerType, createdDate, modifiedDate);
        };
    }
}
