# JVM Basics

## JDK, JRE and JVM

These three names are nested inside each other:

```text
+--------------------------------------------------+
| JDK  (Java Development Kit)                      |
|   javac, javap, jar, ... (tools to build Java)   |
|  +--------------------------------------------+  |
|  | JRE  (Java Runtime Environment)            |  |
|  |   the standard class library (java.util..) |  |
|  |  +--------------------------------------+  |  |
|  |  | JVM  (Java Virtual Machine)          |  |  |
|  |  |   runs bytecode                      |  |  |
|  |  +--------------------------------------+  |  |
|  +--------------------------------------------+  |
+--------------------------------------------------+
```

| Name | What it is | Who needs it |
|---|---|---|
| **JVM** | The program that actually runs your compiled Java code. It loads `.class` files, checks them, runs them, and manages memory (garbage collection). | Everyone who runs Java |
| **JRE** | The JVM plus the standard libraries (`ArrayList`, `Scanner`, `String`, ...). Everything needed to *run* a Java program. | People who only run Java apps |
| **JDK** | The JRE plus developer tools such as `javac` (compiler), `javap` (class inspector) and `jar`. Everything needed to *write and build* Java programs. | Developers, so you |

Today most vendors ship only the JDK. Installing a JDK gives you all three, and the `java` command inside it is the JVM launcher.

## What is bytecode?

Computers cannot run `.java` files directly, and Java does not compile to machine code for one specific CPU either. The compiler `javac` turns your source code into **bytecode**: a compact, low-level instruction set stored in `.class` files. The JVM reads that bytecode and carries it out.

You can look at it with `javap -c`. For the Hello World program in [Setup_Instructions.md](Setup_Instructions.md), `javap -c HelloWorld` shows:

```text
public static void main(java.lang.String[]);
  Code:
     0: getstatic     #7    // Field java/lang/System.out:Ljava/io/PrintStream;
     3: ldc           #13   // String Hello, World!
     5: invokevirtual #15   // Method java/io/PrintStream.println:(Ljava/lang/String;)V
     8: return
```

In plain words: get `System.out`, load the text `"Hello, World!"`, call `println`, and return.

The flow is: `HelloWorld.java` -- `javac` --> `HelloWorld.class` (bytecode) -- `java` (JVM) --> output.

## "Write once, run anywhere"

Bytecode is the same on every operating system, because it is written for the JVM and not for Windows, macOS or Linux. So you compile your program once and the same `.class` files can run on any machine that has a JVM, with no changes and no recompiling.

The JVM is what makes this work. Each operating system has its own JVM build that knows how to turn the shared bytecode into instructions for that machine. The differences between systems are hidden inside the JVM, so a LearnTrack built on Windows can run on a Mac or Linux server without changing a line of code.
