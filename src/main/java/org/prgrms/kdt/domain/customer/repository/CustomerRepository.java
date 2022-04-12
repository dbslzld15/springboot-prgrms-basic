package org.prgrms.kdt.domain.customer.repository;

import org.prgrms.kdt.domain.customer.model.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository {

    List<Customer> findAll();

    UUID save(Customer customer);
}
