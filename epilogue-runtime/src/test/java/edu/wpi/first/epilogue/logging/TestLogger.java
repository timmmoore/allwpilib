// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.epilogue.logging;

import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.TestClassWithoutTestCases") // This is not a test class!
public class TestLogger implements DataLogger {
  public record LogEntry<T>(String identifier, T value) {}

  private final Map<String, SubLogger> m_subLoggers = new HashMap<>();

  private final List<LogEntry<?>> m_entries = new ArrayList<>();

  public List<LogEntry<?>> getEntries() {
    return m_entries;
  }

  @Override
  public DataLogger getSubLogger(String path) {
    return m_subLoggers.computeIfAbsent(path, k -> new SubLogger(k, this));
  }

  @Override
  public void log(String identifier, int value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, long value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, float value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, double value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, boolean value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, byte[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, int[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, long[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, float[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, double[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, boolean[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, String value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public void log(String identifier, String[] value) {
    m_entries.add(new LogEntry<>(identifier, value));
  }

  @Override
  public <S> void log(String identifier, S value, Struct<S> struct) {
    var serialized = StructBuffer.create(struct).write(value).array();

    m_entries.add(new LogEntry<>(identifier, serialized));
  }

  @Override
  public <S> void log(String identifier, S[] value, Struct<S> struct) {
    var serialized = StructBuffer.create(struct).writeArray(value).array();

    m_entries.add(new LogEntry<>(identifier, serialized));
  }
}
