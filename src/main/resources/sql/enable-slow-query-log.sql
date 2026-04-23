SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.2;
SET GLOBAL log_queries_not_using_indexes = 'ON';

SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'slow_query_log_file';