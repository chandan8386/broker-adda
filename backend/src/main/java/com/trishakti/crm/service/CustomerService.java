package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.Customer;
import com.trishakti.crm.dto.CustomerDtos.CustomerRequest;
import com.trishakti.crm.dto.CustomerDtos.CustomerResponse;
import com.trishakti.crm.exception.DomainExceptions.DuplicateResourceException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.CustomerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String q, Pageable pageable) {
        return PageResponse.of(customerRepository.search(q, pageable), CrmMappers::customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return CrmMappers.customer(find(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        if (customerRepository.existsByMobile(req.mobile())) {
            throw new DuplicateResourceException("A customer with mobile " + req.mobile() + " already exists");
        }
        Customer c = new Customer();
        apply(c, req);
        customerRepository.save(c);
        return CrmMappers.customer(c);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        Customer c = find(id);
        if (!c.getMobile().equals(req.mobile()) && customerRepository.existsByMobile(req.mobile())) {
            throw new DuplicateResourceException("A customer with mobile " + req.mobile() + " already exists");
        }
        apply(c, req);
        return CrmMappers.customer(c);
    }

    private void apply(Customer c, CustomerRequest req) {
        c.setFullName(req.fullName());
        c.setMobile(req.mobile());
        c.setAltMobile(req.altMobile());
        c.setEmail(req.email());
        c.setAddress(req.address());
        c.setCity(req.city());
        c.setIdProofType(req.idProofType());
        c.setIdProofNumber(req.idProofNumber());
        c.setNotes(req.notes());
    }

    private Customer find(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
