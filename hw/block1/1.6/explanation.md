# Single Thread


```
...
"Thread-0" #20 prio=5 os_prio=0 cpu=0.20ms elapsed=16.56s tid=0x00007fc74c626800 nid=0x8216 in Object.wait()  [0x00007fc71c2fc000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x000000071e75bd00> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x000000071e75bd00> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at SingleThread.lambda$main$0(SingleThread.java:7)
	at SingleThread$$Lambda$171/0x0000000800195840.run(Unknown Source)
	at java.lang.Thread.run(java.base@11.0.30/Thread.java:829)

   Locked ownable synchronizers:
	- None
...
```

Мы видим, что поток хочет "переахватить" монитор <0x000000071e75bd00>, чего он сделать не может.


# Double Thread


```
...
"Thread-0" #20 prio=5 os_prio=0 cpu=0.25ms elapsed=12.58s tid=0x00007fef085f6800 nid=0xa9cf in Object.wait()  [0x00007feeb5ffe000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x000000071e7fe018> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x000000071e7fe018> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at DoubleThread.lambda$main$2(DoubleThread.java:16)
	at DoubleThread$$Lambda$173/0x000000080018b040.run(Unknown Source)
	at DoubleThread.lambda$main$0(DoubleThread.java:6)
	at DoubleThread$$Lambda$171/0x000000080018b840.run(Unknown Source)
	at java.lang.Thread.run(java.base@11.0.30/Thread.java:829)

   Locked ownable synchronizers:
	- None

"Thread-1" #21 prio=5 os_prio=0 cpu=0.20ms elapsed=12.58s tid=0x00007fef085f8000 nid=0xa9d0 in Object.wait()  [0x00007feeb5bfe000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x000000071e7fc0a8> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x000000071e7fc0a8> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at DoubleThread.lambda$main$1(DoubleThread.java:9)
	at DoubleThread$$Lambda$172/0x000000080018bc40.run(Unknown Source)
	at java.lang.Thread.run(java.base@11.0.30/Thread.java:829)

   Locked ownable synchronizers:
	- None
...
```

А здесь мы видим, что оба потока ждут при методе `join()` ждут друг друга на мониторах, из-за чего происходит deadlock.
