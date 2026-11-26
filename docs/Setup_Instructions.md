# Setup Instructions

## JDK version used

| Item | Value |
|---|---|
| Distribution | OpenJDK (Microsoft Build of OpenJDK) |
| Version | **17.0.9 LTS** (build `17.0.9+8-LTS`, released 2023-10-17) |
| VM | OpenJDK 64-Bit Server VM |
| Operating system | Windows 11 Pro |
| Install folder | `C:\Program Files\Java\OpenJDK 17.0.9+8 x64` |

Output of the two version commands on this machine:

```text
> java -version
openjdk version "17.0.9" 2023-10-17 LTS
OpenJDK Runtime Environment Microsoft-8552009 (build 17.0.9+8-LTS)
OpenJDK 64-Bit Server VM Microsoft-8552009 (build 17.0.9+8-LTS, mixed mode, sharing)

> javac -version
javac 17.0.9
```

`javac` is the compiler. If `javac -version` works, you have a **JDK** (not just a JRE).

## Installing and configuring a JDK

1. Download a JDK 17 (or newer) from a trusted vendor, for example the Microsoft Build of OpenJDK or Eclipse Temurin, and run the installer.
2. Make sure the JDK's `bin` folder is on your `PATH` (most installers can do this for you), so `java` and `javac` work from any terminal.
3. Optional but recommended: set `JAVA_HOME` to the JDK folder. It is not needed for the commands in this project, but Maven, Gradle and some IDEs look for it.
4. Open a **new** terminal and check with `java -version` and `javac -version`.

LearnTrack only uses Java 8 language features (it compiles with `javac --release 8`), but it has only been run on JDK 17, so JDK 17 is the safe choice.

## "Hello World" run

Create a file named `HelloWorld.java`. The file name must match the public class name:

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

Compile it, then run it:

```text
> javac HelloWorld.java
> java HelloWorld
Hello, World!
```

What happened:

1. `javac HelloWorld.java` compiled the source into **bytecode** and wrote `HelloWorld.class` (427 bytes) next to it. Nothing is printed when compilation succeeds.
2. `java HelloWorld` started the **JVM**, which loaded `HelloWorld.class`, found the `main` method and ran it. The `println` call printed `Hello, World!`.

Note that you run `java HelloWorld` (the class name), not `java HelloWorld.class`.
See [JVM_Basics.md](JVM_Basics.md) for what the JDK, JRE, JVM and bytecode are.

## Running LearnTrack

Build and run steps for the project itself are in the [README](../README.md#how-to-compile-and-run).
In an IDE (IntelliJ IDEA, Eclipse, VS Code): open the `D:\Java_Crashcourse` folder, mark `src` as the sources root, and run `com.airtribe.learntrack.ui.Main`.
