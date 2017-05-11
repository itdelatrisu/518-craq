#!/bin/bash

# ==============================================================================
# Server setup
# ==============================================================================
# -- 3-node chains -------------------------------------------------------------
java -jar target/craq.jar server 0 0 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
java -jar target/craq.jar server 0 1 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
java -jar target/craq.jar server 0 2 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003

# ==============================================================================
# Fig. 4:
# Number of clients vs. throughput (reads/sec) for CR-3 and CRAQ-3
# (assuming 500-byte object)
# ==============================================================================
java -jar target/craq.jar client 155.98.39.4:30001 writeBytes 500
# -- CR-3 ----------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 1 1000 100
# [avg] 1996.69 ops/sec, [1st] 1989 ops/sec, [med] 1997 ops/sec, [99th] 2001 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 5 1000 100
# [avg] 9085.71 ops/sec, [1st] 8834 ops/sec, [med] 9081 ops/sec, [99th] 9268 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 10 1000 100
# [avg] 12094.84 ops/sec, [1st] 12006 ops/sec, [med] 12097 ops/sec, [99th] 12137 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 20 1000 100
# [avg] 12173.92 ops/sec, [1st] 12097 ops/sec, [med] 12178 ops/sec, [99th] 12207 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 30 1000 100
# [avg] 12228.65 ops/sec, [1st] 12157 ops/sec, [med] 12234 ops/sec, [99th] 12256 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 40 1000 100
# [avg] 12262.26 ops/sec, [1st] 12115 ops/sec, [med] 12273 ops/sec, [99th] 12284 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 50 1000 100
# [avg] 12282.55 ops/sec, [1st] 12158 ops/sec, [med] 12291 ops/sec, [99th] 12295 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 100 1000 100
# [avg] 12303.67 ops/sec, [1st] 12269 ops/sec, [med] 12306 ops/sec, [99th] 12309 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 500 1000 100
# [avg] 12680.79 ops/sec, [1st] 12613 ops/sec, [med] 12684 ops/sec, [99th] 12688 ops/sec
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 1000 1000 100
# [avg] 13069.80 ops/sec, [1st] 12663 ops/sec, [med] 13079 ops/sec, [99th] 13122 ops/sec
# -- CRAQ-3 --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 1 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 1995.36 ops/sec, [1st] 1974 ops/sec, [med] 1997 ops/sec, [99th] 2001 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 5 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 9939.37 ops/sec, [1st] 9893 ops/sec, [med] 9944 ops/sec, [99th] 9962 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 10 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 19583.61 ops/sec, [1st] 19365 ops/sec, [med] 19593 ops/sec, [99th] 19695 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 20 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 34091.31 ops/sec, [1st] 33414 ops/sec, [med] 34108 ops/sec, [99th] 34288 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 30 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 36194.08 ops/sec, [1st] 35832 ops/sec, [med] 36189 ops/sec, [99th] 36383 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 40 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 36300.34 ops/sec, [1st] 36164 ops/sec, [med] 36288 ops/sec, [99th] 36453 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 50 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 36367.75 ops/sec, [1st] 36199 ops/sec, [med] 36373 ops/sec, [99th] 36517 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 100 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 36640.64 ops/sec, [1st] 36086 ops/sec, [med] 36658 ops/sec, [99th] 36708 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 500 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 36989.60 ops/sec, [1st] 36783 ops/sec, [med] 37004 ops/sec, [99th] 37057 ops/sec
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 1000 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 37268.43 ops/sec, [1st] 37037 ops/sec, [med] 37269 ops/sec, [99th] 37486 ops/sec

# ==============================================================================
# Fig. 5:
# Operation (read/write/test-and-set) throughput for CR-3 (only reads), CRAQ-3, CRAQ-5, CRAQ-7
# (500-byte object for read/write, 4-byte object for test-and-set, 1st/median/99th percentiles)
# ==============================================================================
# READ -------------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 writeBytes 500
# -- CR-3 ----------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.50:30003 benchmarkRead 1000 1000 100
# [avg] 13069.80 ops/sec, [1st] 12663 ops/sec, [med] 13079 ops/sec, [99th] 13122 ops/sec
# -- CRAQ-3 --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkRead 1000 1000 100 155.98.39.42:30002 155.98.39.50:30003
# [avg] 37268.43 ops/sec, [1st] 37037 ops/sec, [med] 37269 ops/sec, [99th] 37486 ops/sec
# WRITE ------------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkWrite 10 500 1000 100
# [avg] 3341.48 ops/sec, [1st] 2898 ops/sec, [med] 3326 ops/sec, [99th] 3599 ops/sec
# TEST-AND-SET------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkTestAndSet 4 1000 100
# [avg] 836.54 ops/sec, [1st] 777 ops/sec, [med] 792 ops/sec, [99th] 1185 ops/sec

# ==============================================================================
# Fig. 6 & 7:
# Write throughput vs. read throughput for CR-3, CRAQ-3, CRAQ-7 (only 5000-byte)
# (500-byte and 5000-byte object)
# ==============================================================================
# 500-byte ---------------------------------------------------------------------
# -- CR-3 ----------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 0 100 10 10000 155.98.39.50:30003
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 150 250 50 10000 155.98.39.50:30003
# 12059 reads/sec [12059 clean, 0 dirty], 0 writes/sec
# 12258 reads/sec [12258 clean, 0 dirty], 10 writes/sec
# 11988 reads/sec [11988 clean, 0 dirty], 20 writes/sec
# 12212 reads/sec [12212 clean, 0 dirty], 30 writes/sec
# 12286 reads/sec [12286 clean, 0 dirty], 40 writes/sec
# 12352 reads/sec [12352 clean, 0 dirty], 50 writes/sec
# 11885 reads/sec [11885 clean, 0 dirty], 60 writes/sec
# 11894 reads/sec [11894 clean, 0 dirty], 70 writes/sec
# 12059 reads/sec [12059 clean, 0 dirty], 80 writes/sec
# 12465 reads/sec [12465 clean, 0 dirty], 90 writes/sec
# 12441 reads/sec [12441 clean, 0 dirty], 100 writes/sec
# 11928 reads/sec [11928 clean, 0 dirty], 148 writes/sec
# 11967 reads/sec [11967 clean, 0 dirty], 200 writes/sec
# 12116 reads/sec [12116 clean, 0 dirty], 250 writes/sec
# -- CRAQ-3 --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 0 100 10 10000 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 150 250 50 10000 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
# 36796 reads/sec [36796 clean, 0 dirty], 0 writes/sec
# 34956 reads/sec [34927 clean, 29 dirty], 10 writes/sec
# 34962 reads/sec [34878 clean, 84 dirty], 20 writes/sec
# 34289 reads/sec [34146 clean, 143 dirty], 30 writes/sec
# 34281 reads/sec [34087 clean, 194 dirty], 40 writes/sec
# 32886 reads/sec [32653 clean, 234 dirty], 50 writes/sec
# 33597 reads/sec [33293 clean, 304 dirty], 60 writes/sec
# 32475 reads/sec [32125 clean, 350 dirty], 70 writes/sec
# 33171 reads/sec [32776 clean, 395 dirty], 80 writes/sec
# 30975 reads/sec [30531 clean, 443 dirty], 90 writes/sec
# 31253 reads/sec [30762 clean, 491 dirty], 100 writes/sec
# 28236 reads/sec [27544 clean, 691 dirty], 148 writes/sec
# 24997 reads/sec [24006 clean, 991 dirty], 200 writes/sec
# 22480 reads/sec [21220 clean, 1260 dirty], 250 writes/sec
# 5000-byte --------------------------------------------------------------------
# -- CR-3 ----------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 5000 0 100 10 10000 155.98.39.50:30003
# 1267 reads/sec [1267 clean, 0 dirty], 0 writes/sec
# 1289 reads/sec [1289 clean, 0 dirty], 10 writes/sec
# 1286 reads/sec [1286 clean, 0 dirty], 20 writes/sec
# 1284 reads/sec [1284 clean, 0 dirty], 30 writes/sec
# 1297 reads/sec [1297 clean, 0 dirty], 40 writes/sec
# 1290 reads/sec [1290 clean, 0 dirty], 50 writes/sec
# 1282 reads/sec [1282 clean, 0 dirty], 60 writes/sec
# 1289 reads/sec [1289 clean, 0 dirty], 70 writes/sec
# 1284 reads/sec [1284 clean, 0 dirty], 80 writes/sec
# 1285 reads/sec [1285 clean, 0 dirty], 90 writes/sec
# 1283 reads/sec [1283 clean, 0 dirty], 100 writes/sec
# -- CRAQ-3 --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 50 25 5000 0 100 10 10000 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
# 3827 reads/sec [3827 clean, 0 dirty], 0 writes/sec
# 3765 reads/sec [3727 clean, 38 dirty], 10 writes/sec
# 3550 reads/sec [3471 clean, 79 dirty], 20 writes/sec
# 3424 reads/sec [3304 clean, 120 dirty], 30 writes/sec
# 3300 reads/sec [3149 clean, 152 dirty], 40 writes/sec
# 3207 reads/sec [3015 clean, 192 dirty], 50 writes/sec
# 3061 reads/sec [2838 clean, 223 dirty], 60 writes/sec
# 2881 reads/sec [2608 clean, 273 dirty], 70 writes/sec
# 2789 reads/sec [2480 clean, 309 dirty], 80 writes/sec
# 2625 reads/sec [2282 clean, 343 dirty], 90 writes/sec
# 2452 reads/sec [2055 clean, 398 dirty], 100 writes/sec

# ==============================================================================
# Fig. 8:
# Write throughput vs. read throughput for CRAQ-3, split by clean/dirty read status
# (500-byte object)
# ==============================================================================
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 0 100 10 10000 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkReadWrite 100 25 500 150 500 50 10000 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
# 36796 reads/sec [36796 clean, 0 dirty], 0 writes/sec
# 34956 reads/sec [34927 clean, 29 dirty], 10 writes/sec
# 34962 reads/sec [34878 clean, 84 dirty], 20 writes/sec
# 34289 reads/sec [34146 clean, 143 dirty], 30 writes/sec
# 34281 reads/sec [34087 clean, 194 dirty], 40 writes/sec
# 32886 reads/sec [32653 clean, 234 dirty], 50 writes/sec
# 33597 reads/sec [33293 clean, 304 dirty], 60 writes/sec
# 32475 reads/sec [32125 clean, 350 dirty], 70 writes/sec
# 33171 reads/sec [32776 clean, 395 dirty], 80 writes/sec
# 30975 reads/sec [30531 clean, 443 dirty], 90 writes/sec
# 31253 reads/sec [30762 clean, 491 dirty], 100 writes/sec
# 28236 reads/sec [27544 clean, 691 dirty], 148 writes/sec
# 24997 reads/sec [24006 clean, 991 dirty], 200 writes/sec
# 22480 reads/sec [21220 clean, 1260 dirty], 250 writes/sec

# ==============================================================================
# Fig. 9:
# Operation (read/write) latency (ms) for CRAQ-3, CRAQ-6 (only writes), split by clean/dirty read status
# (no and heavy load, 500-byte and 5000-byte object, median/95th/99th percentiles)
# ==============================================================================
# -- [NO LOAD] -----------------------------------------------------------------
# 500-byte ---------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkLatency 500 10000 0
# clean reads: [avg] 0.499ms, [med] 0.497ms, [95th] 0.518ms, [99th] 0.542ms
# dirty reads: [avg] 0.934ms, [med] 0.994ms, [95th] 1.010ms, [99th] 1.037ms
# writes:      [avg] 1.720ms, [med] 1.741ms, [95th] 2.017ms, [99th] 2.032ms
# 5000-byte --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkLatency 5000 10000 0
# clean reads: [avg] 1.266ms, [med] 1.250ms, [95th] 1.323ms, [99th] 1.522ms
# dirty reads: [avg] 1.754ms, [med] 1.746ms, [95th] 1.798ms, [99th] 1.830ms
# writes:      [avg] 4.276ms, [med] 4.245ms, [95th] 4.499ms, [99th] 5.509ms
# -- [HEAVY LOAD] --------------------------------------------------------------
# 500-byte ---------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkLatency 500 10000 50 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
# clean reads: [avg] 1.374ms, [med] 1.396ms, [95th] 1.492ms, [99th] 1.586ms
# dirty reads: [avg] 3.634ms, [med] 3.605ms, [95th] 3.995ms, [99th] 4.704ms
# writes:      [avg] 5.344ms, [med] 5.268ms, [95th] 6.465ms, [99th] 7.219ms
# 5000-byte --------------------------------------------------------------------
java -jar target/craq.jar client 155.98.39.4:30001 benchmarkLatency 5000 10000 50 155.98.39.4:30001 155.98.39.42:30002 155.98.39.50:30003
# clean reads: [avg] 12.370ms, [med] 13.861ms, [95th] 16.260ms, [99th] 16.961ms
# dirty reads: [avg] 28.550ms, [med] 29.446ms, [95th] 32.508ms, [99th] 34.656ms
# writes:      [avg] 36.280ms, [med] 36.418ms, [95th] 44.851ms, [99th] 47.397ms
