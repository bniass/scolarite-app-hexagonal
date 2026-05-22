package com.ecole221.common.event.publisher;


import com.ecole221.common.event.DomainEvent;

public interface DomainEventPublisher<T extends DomainEvent> {

    void publish(T domainEvent);
}
