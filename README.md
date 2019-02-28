# KRY code assignment

One of our developers built a simple service poller.
The service consists of a backend service that keeps a list of services (defined by a URL), and periodically does a HTTP GET to each and saves the response ("OK" or "FAIL").

Unfortunately, the original developer din't finish the job, and it's now up to you to complete the thing.
In the backlog are the following issues:

- Whenever the server is restarted, any added services disappear
- Service URL's are not validated in any way ("sdgf" is _not_ up)
- There's no way to delete individual services
- The poller doesn't actually work
- The frontend needs some work
- Simultaneous writes sometimes causes strange behavior
- There's only one test and it only tests the GET endpoint

Spend maximum two hours working on these issues - if you can solve all of them, great!
If you cannot finish all of them, try to finish the ones you start.

Good luck!

# Building
We recommend using IntelliJ as it's what we use day to day at the KRY office.
In intelliJ, choose
```
New -> New from existing sources -> Import project from external model -> Gradle
```

You can also run gradle directly from the command line:
```
./gradlew clean run
```
