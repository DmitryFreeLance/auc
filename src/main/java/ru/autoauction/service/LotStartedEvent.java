package ru.autoauction.service;

public record LotStartedEvent(long lotId, String title, long startingPrice) {}
