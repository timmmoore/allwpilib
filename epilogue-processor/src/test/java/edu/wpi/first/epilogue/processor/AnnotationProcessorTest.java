// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.epilogue.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:LineLength") // Source code templates exceed the line length limit
class AnnotationProcessorTest {
  @Test
  void simple() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        double x;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void multiple() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        double x;
        double y;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("y", object.y);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateFields() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        private double x;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", (double) $x.get(object));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateWithGenerics() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        private edu.wpi.first.wpilibj.smartdashboard.SendableChooser<String> chooser;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $chooser;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $chooser = lookup.findVarHandle(Example.class, "chooser", edu.wpi.first.wpilibj.smartdashboard.SendableChooser.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            logSendable(dataLogger.getSubLogger("chooser"), (edu.wpi.first.wpilibj.smartdashboard.SendableChooser<java.lang.String>) $chooser.get(object));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void importanceLevels() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged(importance = Logged.Importance.INFO)
      class Example {
        @Logged(importance = Logged.Importance.DEBUG)    double low;
        @Logged(importance = Logged.Importance.INFO)     int    medium;
        @Logged(importance = Logged.Importance.CRITICAL) long   high;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("low", object.low);
          }
          if (Epilogue.shouldLog(Logged.Importance.INFO)) {
            dataLogger.log("medium", object.medium);
          }
          if (Epilogue.shouldLog(Logged.Importance.CRITICAL)) {
            dataLogger.log("high", object.high);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void logEnum() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        enum E {
          a, b, c;
        }
        E enumValue;   // Should be logged
        E[] enumArray; // Should not be logged
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("enumValue", object.enumValue);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void bytes() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        byte x;        // Should be logged
        byte[] arr1;   // Should be logged
        byte[][] arr2; // Should not be logged

        public byte getX() { return x; }
        public byte[] getArr1() { return arr1; }
        public byte[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void chars() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        char x;        // Should be logged
        char[] arr1;   // Should not be logged
        char[][] arr2; // Should not be logged

        public char getX() { return x; }
        public char[] getArr1() { return arr1; }
        public char[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("getX", object.getX());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void shorts() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        short x;        // Should be logged
        short[] arr1;   // Should not be logged
        short[][] arr2; // Should not be logged

        public short getX() { return x; }
        public short[] getArr1() { return arr1; }
        public short[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("getX", object.getX());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void ints() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        int x;           // Should be logged
        int[] arr1;   // Should be logged
        int[][] arr2; // Should not be logged

        public int getX() { return x; }
        public int[] getArr1() { return arr1; }
        public int[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void longs() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        long x;        // Should be logged
        long[] arr1;   // Should be logged
        long[][] arr2; // Should not be logged

        public long getX() { return x; }
        public long[] getArr1() { return arr1; }
        public long[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void floats() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        float x;        // Should be logged
        float[] arr1;   // Should be logged
        float[][] arr2; // Should not be logged

        public float getX() { return x; }
        public float[] getArr1() { return arr1; }
        public float[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void doubles() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import java.util.List;

      @Logged
      class Example {
        double x;        // Should be logged
        double[] arr1;   // Should be logged
        double[][] arr2; // Should not be logged
        List<Double> list; // Should not be logged

        public double getX() { return x; }
        public double[] getArr1() { return arr1; }
        public double[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void booleans() {
    String source =
        """
      package edu.wpi.first.epilogue;
      import java.util.List;

      @Logged
      class Example {
        boolean x;        // Should be logged
        boolean[] arr1;   // Should be logged
        boolean[][] arr2; // Should not be logged
        List<Boolean> list; // Should not be logged

        public boolean getX() { return x; }
        public boolean[] getArr1() { return arr1; }
        public boolean[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void strings() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import java.util.List;

      @Logged
      class Example {
        String x;         // Should be logged
        String[] arr1;   // Should be logged
        String[][] arr2; // Should not be logged
        List<String> list;  // Should be logged

        public String getX() { return x; }
        public String[] getArr1() { return arr1; }
        public String[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x);
            dataLogger.log("arr1", object.arr1);
            dataLogger.log("list", object.list);
            dataLogger.log("getX", object.getX());
            dataLogger.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void structs() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.List;

      @Logged
      class Example {
        static class Structable implements StructSerializable {
          int x, y;

          public static final Struct<Structable> struct = null; // value doesn't matter
        }

        Structable x;        // Should be logged
        Structable[] arr1;   // Should be logged
        Structable[][] arr2; // Should not be logged
        List<Structable> list; // Should be logged

        public Structable getX() { return x; }
        public Structable[] getArr1() { return arr1; }
        public Structable[][] getArr2() { return arr2; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("x", object.x, edu.wpi.first.epilogue.Example.Structable.struct);
            dataLogger.log("arr1", object.arr1, edu.wpi.first.epilogue.Example.Structable.struct);
            dataLogger.log("list", object.list, edu.wpi.first.epilogue.Example.Structable.struct);
            dataLogger.log("getX", object.getX(), edu.wpi.first.epilogue.Example.Structable.struct);
            dataLogger.log("getArr1", object.getArr1(), edu.wpi.first.epilogue.Example.Structable.struct);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void lists() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Logged
      class Example {
        /* Logged */     List<String> list;
        /* Not Logged */ List<List<String>> nestedList;
        /* Not logged */ List rawList;
        /* Logged */     Set<String> set;
        /* Logged */     Queue<String> queue;
        /* Logged */     Stack<String> stack;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("list", object.list);
            dataLogger.log("set", object.set);
            dataLogger.log("queue", object.queue);
            dataLogger.log("stack", object.stack);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void boxedPrimitiveLists() {
    // Boxed primitives are not directly supported, nor are arrays of boxed primitives
    // int[] is fine, but Integer[] is not

    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.List;

      @Logged
      class Example {
        /* Not logged */ List<Integer> ints;
        /* Not logged */ List<Double> doubles;
        /* Not logged */ List<Long> longs;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void badLogSetup() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Logged
      class Example {
        @Logged Map<String, String> notLoggableType;
        @Logged List rawType;
        @NotLogged List skippedUnloggable;

        @Logged
        private String privateMethod() { return ""; }

        @Logged
        String packagePrivateMethod() { return ""; }

        @Logged
        protected String protectedMethod() { return ""; }

        @Logged
        public static String publicStaticMethod() { return ""; }

        @Logged
        private static String privateStaticMethod() { return ""; }

        @Logged
        public void publicVoidMethod() {}

        @Logged
        public Map<String, String> publicNonLoggableMethod() { return notLoggableType; }
      }
      """;

    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).failed();
    assertThat(compilation).hadErrorCount(10);

    List<Diagnostic<? extends JavaFileObject>> errors = compilation.errors();
    assertAll(
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this field, but it is not a loggable data type!",
                9,
                31,
                errors.get(0)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this field, but it is not a loggable data type!",
                10,
                16,
                errors.get(1)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 14, 18, errors.get(2)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 17, 10, errors.get(3)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 20, 20, errors.get(4)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods cannot be static", 23, 24, errors.get(5)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 26, 25, errors.get(6)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods cannot be static", 26, 25, errors.get(7)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this method, but it does not return a loggable data type!",
                29,
                15,
                errors.get(8)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this method, but it does not return a loggable data type!",
                32,
                30,
                errors.get(9)));
  }

  @Test
  void onGenericType() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example<T extends String> {
        T value;

        public <S extends T> S upcast() { return (S) value; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            dataLogger.log("value", object.value);
            dataLogger.log("upcast", object.upcast());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void annotationInheritance() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Child {}

      class GoldenChild extends Child {} // inherits the @Logged annotation from the parent

      @Logged
      interface IO {}

      class IOImpl implements IO {}

      @Logged
      public class Example {
        /* Logged */     Child child;
        /* Not Logged */ GoldenChild goldenChild;
        /* Logged */     IO io;
        /* Not logged */ IOImpl ioImpl;
      }
      """;

    String expectedRootLogger =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            Epilogue.childLogger.tryUpdate(dataLogger.getSubLogger("child"), object.child, Epilogue.getConfig().errorHandler);
            Epilogue.ioLogger.tryUpdate(dataLogger.getSubLogger("io"), object.io, Epilogue.getConfig().errorHandler);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void customLogger() {
    String source =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.logging.*;

        record Point(int x, int y) {}

        @CustomLoggerFor(Point.class)
        class CustomPointLogger extends ClassSpecificLogger<Point> {
          public CustomPointLogger() {
            super(Point.class);
          }

          @Override
          public void update(DataLogger dataLogger, Point point) {
            // Implementation is irrelevant
          }
        }

        @Logged
        class Example {
          Point point;
        }
        """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.DataLogger;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(DataLogger dataLogger, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            Epilogue.customPointLogger.tryUpdate(dataLogger.getSubLogger("point"), object.point, Epilogue.getConfig().errorHandler);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void warnsAboutNonLoggableFields() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        class Example {
          Throwable t;
        }
        """;

    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).succeeded();
    assertEquals(1, compilation.notes().size());
    var warning = compilation.notes().get(0);
    var message = warning.getMessage(Locale.getDefault());
    assertEquals(
        "[EPILOGUE] Excluded from logs because java.lang.Throwable is not a loggable data type",
        message);
  }

  private void assertCompilationError(
      String message, long lineNumber, long col, Diagnostic<? extends JavaFileObject> diagnostic) {
    assertAll(
        () -> assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind(), "not an error"),
        () ->
            assertEquals(
                message, diagnostic.getMessage(Locale.getDefault()), "error message mismatch"),
        () -> assertEquals(lineNumber, diagnostic.getLineNumber(), "line number mismatch"),
        () -> assertEquals(col, diagnostic.getColumnNumber(), "column number mismatch"));
  }

  private void assertLoggerGenerates(String loggedClassContent, String loggerClassContent) {
    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "edu.wpi.first.epilogue.Example", loggedClassContent));

    assertThat(compilation).succeeded();
    var generatedFiles = compilation.generatedSourceFiles();
    var generatedFile =
        generatedFiles.stream()
            .filter(jfo -> jfo.getName().contains("Example"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Logger file was not generated!"));
    try {
      var content = generatedFile.getCharContent(false);
      assertEquals(
          loggerClassContent.replace("\r\n", "\n"), content.toString().replace("\r\n", "\n"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
