As tested on my device (AMD 9950X):

1. `1kBatchUpdate` and `_1kVertxSqlClientBatchUpdate` produce on-par results, which are only 1% of the TFB results.
1. Refactoring to a `Verticle`-based way (multi-threads, queries invoked via the event-bus) with Copilot yields 3 times
   the performance.