package dev.studyforge.insights.application;

/** Published inside the projection transaction; live views refresh after it commits. */
public record StatsChanged(String learnerId) {}
