# java-agent-poc

Sample java agent which allows for playing with the time of the JVM.
Unlike Jodatime which only allows for playing with the time used by the jodatime classes, this intercepts all calls to System.currentTimeMillis and adjusts the time any javacode thinks it runs with.  This makes it useful for simulating time warps on the JVM (such as tests for 'what happens if I run this code at midnight?', 'on a DST change?', 'february 29th?', etc)
