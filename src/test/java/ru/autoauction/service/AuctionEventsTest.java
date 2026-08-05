package ru.autoauction.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionEventsTest {
  @Test
  void countsUniqueUsersGloballyAndPerAuction() {
    AuctionEvents events = new AuctionEvents();

    events.subscribeGlobal(10L);
    events.subscribe(101L, 10L);
    events.subscribe(101L, 20L);
    events.subscribe(202L, 20L);

    assertEquals(2L, events.onlineUsers());
    assertEquals(2L, events.onlineUsers(101L));
    assertEquals(1L, events.onlineUsers(202L));
    assertEquals(2L, events.onlineByLot().get(101L));
    assertEquals(1L, events.onlineByLot().get(202L));
  }
}
