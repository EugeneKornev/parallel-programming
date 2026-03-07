# Часть А

[Lock.lock()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/locks/Lock.html#lock())
> A Lock implementation may be able to detect erroneous use of the lock, such as an invocation that would cause deadlock, and may throw an (unchecked) exception in such circumstances. The circumstances and the exception type must be documented by that Lock implementation.

[Lock.unlock()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/locks/Lock.html#unlock())
> A Lock implementation will usually impose restrictions on which thread can release a lock (typically only the holder of the lock can release it) and may throw an (unchecked) exception if the restriction is violated. Any restrictions and the exception type must be documented by that Lock implementation.

И в качестве примеров можно привести реализации `ReentrantLock` и `ReentrantReadWriteLock.WriteLock`, которые могут выбросить `IllegalMonitorStateException`, если поток,
пытающийся освободить Lock не является его владельцем.

[ReentrantLock.unlock()](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html#unlock--)

[ReentrantReadWriteLock.WriteLock()](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantReadWriteLock.WriteLock.html#unlock--)
> Throws: IllegalMonitorStateException - if the current thread does not hold this lock.

Ответ: да, эти методы могут выбрасывать исключения

# Часть В

В 270 [JEP](https://openjdk.org/jeps/270)'е говорится о выделении дополнительной секции на стеке, при попадании в которую мы можем в зависимости от аннотации либо продолжить исполнение критической секции и затем бросить StackOverflowError, либо сразу его выбросить. Также там написано, что были сделаны тесты, чтобы воспроизвести проблему с ReentrantLock, в ходе которых этой доп. секции хватило. И так как эта аннотация относится только к привилегированному коду, и для него места хватило, то можно сделать предположение, что для примитивов места хватит.

Ответ: с помощью выделения дполонительной секции на стеке, можно сделать примитивы синхронизации, защищенные от исключений.
