# Design Notes

## Why ArrayList instead of an array?

LearnTrack does not know how many students, courses or enrollments there will be. They are added while the program runs.

- An **array** has a fixed size, chosen when it is created (`new Student[100]`). If you guess too small, the 101st student has no room. If you guess too big, you waste memory. To grow it you have to create a bigger array and copy everything over by hand. To remove an item you have to shift the others down, and you have to keep your own counter of how many slots are really used.
- An **ArrayList** grows and shrinks by itself. `add`, `remove`, `size`, `isEmpty` and `contains` are ready to use, and the list is still backed by an array inside, so reading by position stays fast.

So `StudentService`, `CourseService` and `EnrollmentService` each keep their data in a private `ArrayList` (`students`, `courses`, `enrollments`), and the code never needs to know how many items there are in advance.

The `list...()` methods return `new ArrayList<>(students)`, a **copy**. This stops other classes from adding or removing items in the service's list behind its back (encapsulation).

Arrays are still the better choice when the size is known and fixed, for example the arguments array `String[] args` in `main`.

## Where static members are used, and why

A `static` member belongs to the class itself, not to one object. There is exactly one copy, shared by everyone.

| Where | What | Why static |
|---|---|---|
| `IdGenerator` | `studentIdCounter`, `courseIdCounter`, `enrollmentIdCounter` and `getNextStudentId()` etc. | IDs must be unique across the whole program. One shared counter per kind of entity guarantees that, and callers do not need an `IdGenerator` object. |
| `InputValidator` | `requireNonBlank`, `parseInt`, `cleanOptionalEmail`, ... | These helpers keep no data, so they do not need an object. You call `InputValidator.parseInt(...)`. |
| `Main.main` | the program entry point | The JVM has to start the program before any object exists, so `main` must be static. |
| `Main.DIVIDER`, `StudentService.NO_ID` | `static final` constants | A constant has one value for the whole class, so there is no point storing a copy per object. |

Both utility classes are `final` and have a `private` constructor, so nobody can create or extend them by mistake.

Trade-off: static state is global. The ID counters never reset while the program runs, which is what we want here, but it would make automated tests depend on each other. That is a good reason to look at other designs later.

## Where inheritance is used, and what it gives us

```text
Person            (id, firstName, lastName, email, getFullName(), getDisplayName())
 |-- Student      (+ batch, active; overrides getDisplayName())
 `-- Trainer      (+ expertise;     overrides getDisplayName())
```

What we gained:

- **No repeated code.** The four shared fields, their getters and setters, and `getFullName()` are written once in `Person`. `Student` and `Trainer` only add what is different.
- **Constructor chaining with `super`.** `Student(...)` calls `super(id, firstName, lastName, email)` to let `Person` fill in its part. `Student` also uses `this(...)` so the constructor without an email reuses the one with an email.
- **Method overriding.** `Student.getDisplayName()` returns `Ada Lovelace (Batch B1)`; `Trainer.getDisplayName()` returns `Grace Hopper (Trainer: Java)`. Each calls `super.getDisplayName()` and adds its own detail.
- **Polymorphism.** `Person.toString()` calls `getDisplayName()`. When the object is really a `Student`, Java runs the `Student` version at run time, so `System.out.println(student)` shows the batch without `Person` knowing anything about students. A `Person` variable can hold a `Student` or a `Trainer`, and the right behaviour is chosen automatically.

`Trainer` is not used in the menus yet. It is there to show that the hierarchy can grow: a new kind of person only needs its extra fields and the methods that differ.

## Other design choices

- **Entity / service / UI separation.** Entities only hold data. Services hold the rules (validation, "inactive students cannot enroll", "no duplicate active enrollment"). `Main` only prints menus, reads input and calls services. That way the same services could be reused with a different UI.
- **Checked custom exceptions.** `EntityNotFoundException` and `InvalidInputException` extend `Exception`, so the compiler forces every caller to handle them or declare `throws`. The menu loops catch them and print a short message instead of crashing.
- **Validate first, then change state.** `addStudent` checks all input before asking `IdGenerator` for an ID, and `updateStudent` runs its only failing check before changing anything. A rejected request leaves no gaps in the IDs and no half-updated objects.
- **Deactivate instead of delete.** Students are deactivated (`active = false`), so enrollments never point at a student that no longer exists. `StudentService.removeStudent` exists but the menu does not call it.
- **Enum for the enrollment status.** `EnrollmentStatus` allows only `ACTIVE`, `COMPLETED` and `CANCELLED`. With plain Strings, a typo would only show up at run time.
- **Access levels.** `public` for the API of each class, `private` for fields and helper methods, and default (package-private) for `ConsoleInput`, which only the `ui` package needs.
- **Entities refer to each other by ID.** `Enrollment` stores `studentId` and `courseId`, not `Student` and `Course` objects. This keeps the entities simple; `EnrollmentService` looks things up through the other services.
